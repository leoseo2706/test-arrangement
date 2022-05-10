package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrConstant;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import com.fiats.arrangement.jpa.entity.SnapshotContract;
import com.fiats.arrangement.jpa.entity.SnapshotContractDetail;
import com.fiats.arrangement.jpa.repo.ArrangementOperationRepo;
import com.fiats.arrangement.jpa.repo.SnapshotContractDetailRepo;
import com.fiats.arrangement.jpa.repo.SnapshotContractRepo;
import com.fiats.arrangement.payload.TemplateFileDTO;
import com.fiats.arrangement.service.AttributeService;
import com.fiats.arrangement.service.SnapshotService;
import com.fiats.arrangement.utils.ConverterUtils;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.constant.VelocityParamKeyEnum;
import com.fiats.tmgcoreutils.jwt.JWTHelper;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgcoreutils.utils.ReactiveClientUtils;
import com.fiats.tmgjpa.entity.RecordStatus;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SnapshotServiceImpl implements SnapshotService {

    @Autowired
    ArrangementOperationRepo arrOpRepo;

    @Autowired
    SnapshotContractRepo snapshotContractRepo;

    @Autowired
    SnapshotContractDetailRepo snapshotContractDetailRepo;

    @Autowired
    @Qualifier("customWebClient")
    WebClient webClient;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    @Qualifier("customMapper")
    ObjectMapper mapper;

    @Value("${fiats.internal.api.content.url}")
    String contentUrl;

    @Value(("${fiats.internal.api.contract.url}"))
    String contractUrl;

    @Autowired
    AttributeService attributeService;

    @Override
    @Transactional
    public void snapshotContract(CustomerBrokerWrapper cusBroker, Arrangement arrangement,
                                 ArrangementOperation operation, OrderPlacementDTO dto,
                                 JWTHelper jwtHelper) {

        if (dto == null || dto.getPricing() == null) {
            throw new ValidationException("Invalid pricing payload");
        }

        log.info("Preparing for snapshot contract based on arrangement ID {}",
                dto.getArrangement().getId());

        // calling attributes to get contract code
        PropsResp groupContractAttr = attributeService.findSystemAttribute(
                ArrConstant.ATTRIBUTE.CONTRACT_VARIABLE_CODE,
                arrangement.getTradingDate(), arrangement.getProductDerivativeCode(),
                arrangement.getProductVanillaCode(),
                dto.getDerivative().getProdAgreement().getCode(),
                ArrangementTypeEnum.lookForType(arrangement.getType()),
                arrangement.getChannel(), cusBroker.getCustomer().getCustomerType());

        ContGroupDocDTO groupDoc = findLatestContent(groupContractAttr.getValue()).block();
        if (groupDoc == null || CommonUtils.isInvalidPK(groupDoc.getId())
                || CollectionUtils.isEmpty(groupDoc.getContTemplateDocVersions())) {
            throw new NeoFiatsException(ArrangementErrorCode.TEMPLATE_NOT_FOUND,
                    CommonUtils.format("All templates for {0} have been deactivated",
                            groupContractAttr.getValue()));
        }
        List<ContTemplateDocVersionDTO> activeTemplates = groupDoc.getContTemplateDocVersions();

        // now build model
        Map<String, Object> modelMap = buildSnapshotModel(cusBroker, dto);
        log.info("Done building model {}. now calling contract generator for arrangement ID {} ....",
                LoggingUtils.objToStringIgnoreEx(modelMap), dto.getArrangement().getId());

        // call contract generator
        SnapshotDTO snapshotDTO = SnapshotDTO.builder()
                .arrangementId(dto.getArrangement().getId())
                .model(modelMap).templates(activeTemplates)
                .build();
        List<ContTemplateDocVersionDTO> renderedTemplates = callContractGenerator(snapshotDTO,
                cusBroker.getCustomer().getAccount()).collectList().block();
        dto.setContracts(renderedTemplates);
        log.info("Done rendered template for {}", dto.getArrangement().getId());

        // save to snapshot tables
        saveSnapshot(arrangement, groupDoc, modelMap, renderedTemplates);

        // now mark status
        operation.setContractStatus(RecordStatus.ACTIVE.getStatus());
        operation.setContractStatusDate(DateHelper.nowInTimestamp());
        operation = arrOpRepo.save(operation);

        if (dto.getOperation() == null) {
            dto.setOperation(new ArrangementOperationDTO());
        }

        BeanUtils.copyProperties(operation, dto.getOperation());
    }

    @Override
    public Map<String, Object> buildSnapshotModel(CustomerBrokerWrapper cusBroker,
                                                  OrderPlacementDTO dto) {

        try {

            // all available parameters
            Map<String, Collection<ContParamDTO>> allParams = collectAllContractParameters()
                    .filter(param -> StringUtils.hasText(param.getParamSource()))
                    .collectMultimap(ContParamDTO::getParamSource).block();

            // modify the map structure first
            Map<String, Object> mapModel = buildOriginalMapModel(cusBroker, dto);

            // then flatten it
            Map<String, Object> flattenedMap = ConverterUtils.flattenMap(mapModel, mapper);

            // now swap keys
            Map<String, Object> modifiedMapModel = new TreeMap<>();
            flattenedMap.forEach((key, value) -> {
                Collection<ContParamDTO> params = allParams.get(key);

                // add only the mapped ones
                if (params != null) {

                    // add matching results
                    params.forEach(param -> {
                        String tmpModifiedKey = ArrConstant.DOLLAR_SIGN.concat(param.getCode());
                        Object tmpModfiedValue = value;
                        if (StringUtils.hasText(param.getType())) {
                            if (ArrConstant.PARAM_TYPE_DATE_TIME.equalsIgnoreCase(param.getType())) {
                                tmpModfiedValue = ConverterUtils.formatDateFlexibly(value);
                            } else if (ArrConstant.PARAM_TYPE_NUMBER.equalsIgnoreCase(param.getType()) && StringUtils.hasText(param.getFormat())) {
                                tmpModfiedValue = ConverterUtils.formatNumber(value, param.getFormat());
                            } else if (ArrConstant.PARAM_TYPE_STRING.equals(param.getType())) {
                                tmpModfiedValue = value instanceof String ? value
                                        : ConverterUtils.formatNumberSilently(value);
                            }
                        }
                        modifiedMapModel.put(tmpModifiedKey, tmpModfiedValue);
                    });
                }
            });

            // for table models, feel free to add more
            if (dto.getPricing() != null && !CollectionUtils.isEmpty(dto.getPricing().getCoupons())) {

                buildAndAddTableMapModel(allParams, VelocityParamKeyEnum.COUPON_TABLE_MODEL_KEY.getKey(),
                        LoggingUtils.objToStringIgnoreEx(dto.getPricing().getCoupons()),
                        modifiedMapModel, null);
            }

            return modifiedMapModel;

        } catch (Exception e) {
            log.error("Error occurred while building model for snapshotContract", e);
            throw new NeoFiatsException("Some fields while generating contracts are not valid");
        }

    }

    @Override
    public Flux<ContTemplateDocVersionDTO> callContractGenerator(SnapshotDTO dto, String userName) {

        if (dto == null || CommonUtils.isInvalidPK(dto.getArrangementId())
                || CollectionUtils.isEmpty(dto.getModel())
                || CollectionUtils.isEmpty(dto.getTemplates())
                || !StringUtils.hasText(userName)) {
            throw new NeoFiatsException("Invalid payload or user name!");
        }

        return ReactiveClientUtils.postForFluxWithModel(webClient,
                contractUrl, "/internal/snapshot/render/{userName}",
                executor, ContTemplateDocVersionDTO.class, log, dto, userName);
    }

    @Override
    public Flux<TemplateFileDTO> listAvailableFiles(Long arrangementId, String account) {

        if (CommonUtils.isInvalidPK(arrangementId) || !StringUtils.hasText(account)) {
            throw new NeoFiatsException(ArrangementErrorCode.CONTRACT_INFO_EMPTY,
                    CommonUtils.format("Empty arrangementId {0} or account {1}",
                            arrangementId, account));
        }

        return ReactiveClientUtils.getForFlux(webClient, contractUrl,
                "/internal/snapshot/list/{arrangementId}?u={account}",
                executor, TemplateFileDTO.class, log, arrangementId, account);
    }

    private void saveSnapshot(Arrangement arrangement, ContGroupDocDTO groupDoc,
                              Map<String, Object> modelMap,
                              List<ContTemplateDocVersionDTO> renderedTemplates) {

        String param = LoggingUtils.objToStringIgnoreEx(modelMap);

        Timestamp now = DateHelper.nowInTimestamp();
        SnapshotContract snapshotContract = new SnapshotContract();
        snapshotContract.setArrangement(arrangement);
        snapshotContract.setArrangementCode(arrangement.getCode());
        snapshotContract.setContGroupDocId(groupDoc.getId());
        snapshotContract.setSnapshotDate(now);
        snapshotContract.insertExtra(now);
        snapshotContract.setParameters(param);
        snapshotContract = snapshotContractRepo.save(snapshotContract);

        List<SnapshotContractDetail> snapshotContractDetails = new ArrayList<>();
        for (ContGroupDocHistoryDTO cgdh : groupDoc.getContGroupDocHistories()) {
            SnapshotContractDetail snapshotContractDetail = new SnapshotContractDetail();
            snapshotContractDetail.setSnapshotContract(snapshotContract);
            snapshotContractDetail.setGroupDocHistoryId(cgdh.getId());
            snapshotContractDetail.setParameters(param);
            snapshotContractDetail.setSnapshotDate(now);

            ContTemplateDocVersionDTO ctdv = groupDoc
                    .getContTemplateDocVersions()
                    .stream()
                    .filter(tmp -> cgdh.getTemplateId() != null
                            && cgdh.getTemplateId().equals(tmp.getTemplateId()))
                    .findAny()
                    .orElseThrow(() -> new NeoFiatsException(
                            ArrangementErrorCode.TEMPLATE_NOT_FOUND,
                            CommonUtils.format(
                                    "Template for contract group doc history {0} cannot be found",
                                    cgdh.getId()))
                    );
            snapshotContractDetail.setTemplateVersionId(ctdv.getId());
            snapshotContractDetail.setTemplateContent(ctdv.getContent());

            ContTemplateDocVersionDTO renderedCtdv = renderedTemplates.stream()
                    .filter(t -> cgdh.getTemplateId().equals(t.getTemplateId()))
                    .findAny().orElseThrow(() -> new NeoFiatsException(
                            ArrangementErrorCode.TEMPLATE_NOT_FOUND,
                            CommonUtils.format(
                                    "Template content for contract group doc history {0} cannot be found",
                                    cgdh.getId())));
            snapshotContractDetail.setContent(renderedCtdv.getContent());
            snapshotContractDetail.setTemplatePath(renderedCtdv.getTemplatePath());
            snapshotContractDetail.insertExtra(now);
            snapshotContractDetails.add(snapshotContractDetail);
        }

        snapshotContractDetails = snapshotContractDetailRepo.saveAll(snapshotContractDetails);

        log.info("Done saving snapshot records for group ID {} and arrangement ID {}",
                groupDoc.getId(), arrangement.getId());
    }


    private Mono<ContGroupDocDTO> findLatestContent(String groupDocName) {

        log.info("Finding latest template content for group doc name {}", groupDocName);

        if (!StringUtils.hasText(groupDocName)) {
            throw new NeoFiatsException(CommonUtils.format("Invalid group doc name {0}",
                    groupDocName));
        }

        return ReactiveClientUtils.getForMono(webClient, contentUrl,
                "/group-contract/{groupDocName}", executor, ContGroupDocDTO.class,
                log, groupDocName)
                .doOnSuccess(data -> log.info("Finding latest group doc id {}", data.getId()))
                .doOnError(throwable -> log.error("Failed to find group doc name {}", groupDocName))
                .onErrorResume(e -> Mono.just(new ContGroupDocDTO()));
    }

    private Flux<ContParamDTO> collectAllContractParameters() {
        return ReactiveClientUtils.getForFlux(webClient, contentUrl,
                "/contract-param/all", executor, ContParamDTO.class, log)
                .doOnComplete(() -> log.info("Done finding all contract parameters"))
                .doOnError(throwable -> log.error("Failed to find all contract parameters", throwable));
    }

    private Map<String, Object> buildOriginalMapModel(CustomerBrokerWrapper cusBroker,
                                                      OrderPlacementDTO dto) {
        Map<String, Object> mapModel = new HashMap<>();
        if (dto != null) {
            mapModel.put(VelocityParamKeyEnum.ORDER_PLACEMENT_MODEL_KEY.getKey(), dto);
        }

        if (cusBroker != null) {

            if (cusBroker.getCustomer() != null) {
                mapModel.put(VelocityParamKeyEnum.CUSTOMER_MODEL_KEY.getKey(), cusBroker.getCustomer());
            }

            if (cusBroker.getBroker() != null) {
                mapModel.put(VelocityParamKeyEnum.BROKER_MODEL_KEY.getKey(), cusBroker.getBroker());
            }

        }

        return mapModel;
    }

    private void filterAndCastObjectValue(Map<String, Collection<ContParamDTO>> tableVariables,
                                          String objKey, Object objValue, Map<String, Object> modifiedObj) {

        // filter all variables start with table keys and end with obj keys
        List<ContParamDTO> paramVariables = tableVariables.entrySet().stream()
                .filter(p -> p.getKey().toLowerCase().trim().endsWith(objKey.toLowerCase().trim()))
                .flatMap(p -> p.getValue().stream())
                .collect(Collectors.toList());

        paramVariables.forEach(param -> {
            Object modifiedValue = objValue;

            if (StringUtils.hasText(param.getType())) {
                if (ArrConstant.PARAM_TYPE_DATE_TIME.equalsIgnoreCase(param.getType())) {
                    modifiedValue = ConverterUtils.formatDateFlexibly(objValue);
                } else if (ArrConstant.PARAM_TYPE_NUMBER.equalsIgnoreCase(param.getType()) && StringUtils.hasText(param.getFormat())) {
                    modifiedValue = ConverterUtils.formatNumber(objValue, param.getFormat());
                } else if (ArrConstant.PARAM_TYPE_STRING.equals(param.getType())) {
                    modifiedValue = objValue instanceof String ? objValue
                            : ConverterUtils.formatNumberSilently(objValue);
                }
            }
            // replace them
            modifiedObj.put(param.getCode(), modifiedValue);
        });
    }

    private void buildAndAddTableMapModel(Map<String, Collection<ContParamDTO>> allParams, String tableKey,
                                          String mapModelStr, Map<String, Object> modifiedMapModel,
                                          Map<String, Object> extraAttributes) {
        try {

            if (CollectionUtils.isEmpty(allParams) || !StringUtils.hasText(tableKey)
                    || !StringUtils.hasText(mapModelStr) || modifiedMapModel == null) {
                return;
            }

            Map<String, Collection<ContParamDTO>> tableVariables = allParams.entrySet().stream()
                    .filter(p -> p.getKey().toLowerCase().trim().startsWith(tableKey.toLowerCase().trim()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            log.info("Done finding all possible variables for table key {}, {}", tableKey,
                    LoggingUtils.objToStringIgnoreEx(tableVariables));

            List<Object> result = new ArrayList<>();
            JSONArray modelArray = new JSONArray(mapModelStr);
            for (int i = 0; i < modelArray.length(); i++) {
                JSONObject modelObj = modelArray.getJSONObject(i);
                Map<String, Object> modifiedObj = new HashMap<>();

                // build attribute from the main model table - mapModelStr
                Iterator iterator = modelObj.keys();
                while (iterator.hasNext()) {
                    String objKey = iterator.next().toString();
                    Object objValue = modelObj.get(objKey);

                    // filter all variables start with table keys and end with obj keys
                    filterAndCastObjectValue(tableVariables, objKey, objValue, modifiedObj);
                }

                // add extra attributes not inside the mapModelStr variable
                if (!CollectionUtils.isEmpty(extraAttributes)) {
                    extraAttributes.forEach((extraKey, extraValue) ->
                            filterAndCastObjectValue(tableVariables, extraKey, extraValue, modifiedObj));
                }

                result.add(modifiedObj);
            }

            modifiedMapModel.put(ArrConstant.DOLLAR_SIGN.concat(tableKey), result);
            log.info("Done building table {} with result {}", tableKey, LoggingUtils.objToStringIgnoreEx(result));

        } catch (Exception e) {
            log.error("Error while building table model {}", e.getMessage(), e);

        }
    }

}