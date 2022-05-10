package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.*;
import com.fiats.arrangement.internal.event.NotificationBrokerEvent;
import com.fiats.arrangement.internal.event.NotificationWrapperEvent;
import com.fiats.arrangement.jpa.entity.*;
import com.fiats.arrangement.jpa.repo.*;
import com.fiats.arrangement.jpa.specs.ArrangementSpecs;
import com.fiats.arrangement.payload.CollateralDTO;
import com.fiats.arrangement.payload.PricingParamDTO;
import com.fiats.arrangement.payload.TemplateFileDTO;
import com.fiats.arrangement.payload.filter.ArrangementFilter;
import com.fiats.arrangement.payload.filter.ArrangementFilterStatusEnum;
import com.fiats.arrangement.service.*;
import com.fiats.arrangement.utils.ConverterUtils;
import com.fiats.arrangement.validator.ArrangementValidator;
import com.fiats.arrangement.validator.CustomerValidator;
import com.fiats.exception.ErrorCode;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.constant.MechanismType;
import com.fiats.tmgcoreutils.constant.PortfolioRetailAction;
import com.fiats.tmgcoreutils.jwt.JWTHelper;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgcoreutils.utils.ReactiveClientUtils;
import com.fiats.tmgjpa.entity.RecordStatus;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import com.neo.exception.LoggingUtils;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ArrangementServiceImpl implements ArrangementService {

    @Autowired
    CustomerValidator customerValidator;

    @Autowired
    ArrangementValidator arrValidator;

    @Autowired
    ArrangementSpecs arrSpecs;

    @Autowired
    ArrangementRepo arrRepo;

    @Autowired
    ArrangementPricingRepo arrPriRepo;

    @Autowired
    ArrangementOperationRepo arrOpRepo;

    @Autowired
    ArrangementPartyRepo arrPartyRepo;

    @Autowired
    ArrangementRelationRepo arrRelationRepo;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    @Qualifier("customMapper")
    ObjectMapper mapper;

    @Autowired
    @Qualifier("customWebClient")
    WebClient webClient;

    @Autowired
    ArrangementCodeService arrCodeService;

    @Autowired
    ProductDerivativeService pdService;

    @Autowired
    CustomerService customerService;

    @Autowired
    SnapshotService snapshotService;

    @Autowired
    PortfolioService portfolioService;

    @Autowired
    AttributeService attributeService;

    @Autowired
    ArrangementNotificationService arrNotiService;

    @Autowired
    BrokerService brokerService;

    @Autowired
    PricingService pricingService;

    @Autowired
    MatchingService matchingService;

    @Autowired
    BackSupportService backSupportService;

    @Autowired
    ContractService contractService;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    ApplicationEventPublisher publisher;

    @Value("${fiats.internal.api.info.url}")
    String infoUrl;

    @Value("${fiats.internal.api.content.url}")
    String contentUrl;

    @Value("${fiats.internal.api.matching.url}")
    String matchingUrl;

    @Value("${fiats.internal.api.portfolio.url}")
    String portfolioUrl;

    @Value("${custom.kafka.topic.prefix}")
    String KAFKA_TOPIC_PREFIX;

    @Value("${spring.application.name}")
    String applicationName;

    @Value("${retail.source.account}")
    String retailSourceAccount;

    @Autowired
    private ArrangementLogService arrangementLogService;

    @Override
    public ResponseMessage listCustomerArrangement(PagingFilterBase<ArrangementFilter> pf, JWTHelper jwt) {

        CustomerDTO customer = customerService.findByAccount(jwt.getSubFromJWT(), Constant.INACTIVE).block();
        if (customer == null || CommonUtils.isInvalidPK(customer.getId())) {
            throw new ValidationException("Invalid customer payload");
        }
        // for filtering
        pf.getFilter().setCustomerIds(new ArrayList<Long>() {{
            add(customer.getId());
        }});
        log.info("Finding arrangement records based on {}", pf);

        Specification<Arrangement> spec = arrSpecs.buildCustomerSpec(pf.getFilter());
        List<Arrangement> arrangements = findArrangementBySpecification(pf, spec);

        List<OrderPlacementDTO> records = arrangements.stream()
                .map(this::castEntityToModel)
                .collect(Collectors.toList());

        return new ResponseMessage<>(records, pf.getPaging());
    }

    @Override
    public ResponseMessage listBrokerArrangement(PagingFilterBase<ArrangementFilter> pf, JWTHelper jwt) {

        CustomerDTO broker = customerService.findByAccount(jwt.getSubFromJWT(), Constant.INACTIVE).block();
        if (broker == null || CommonUtils.isInvalidPK(broker.getId())) {
            throw new ValidationException("Invalid broker payload");
        }

        // finding all related arrangements & set filter
        List<Long> arrangementIds = arrPartyRepo.findArrangementIdsForBrokerReference(broker.getId(),
                ArrangementRoleEnum.OWNER.toString(), ArrangementRoleEnum.BROKER.toString());
        pf.getFilter().setIds(arrangementIds);
        log.info("Done collecting arrangement id list for broker reference .. {}", arrangementIds);

        Specification<Arrangement> spec = arrSpecs.buildBrokerSpec(pf.getFilter());
        List<Arrangement> arrangements = findArrangementBySpecification(pf, spec);
        log.info("Done finding arrangement based on list {} and filter {}", arrangementIds, pf.getFilter());

        if (!CollectionUtils.isEmpty(arrangements)) {
            Set<Long> customerIds = arrangements.stream().flatMap(a -> a.getParties().stream())
                    .map(ArrangementParty::getCustomerId).collect(Collectors.toSet());
            log.info("Done collecting customer id list .. {}", customerIds);

            Map<Long, CustomerDTO> customerDTOMap = customerService.retrieveCustomerInfo(customerIds)
                    .stream().collect(Collectors.toMap(CustomerDTO::getId, Function.identity()));

            Comparator<OrderPlacementDTO> sortByTradingDate = Comparator.comparing((OrderPlacementDTO p) -> p.getArrangement().getTradingDate())
                    .thenComparing(p -> p.getArrangement().getCreatedDate());

            log.info("Finding arrangement records based on {}", pf);
            List<OrderPlacementDTO> records = arrangements.stream()
                    .map(a -> {

                        OrderPlacementDTO orderInfo = castEntityToModel(a);

                        ArrangementParty ownerParty = a.getParty(ArrangementRoleEnum.OWNER);
                        if (ownerParty != null) {
                            CustomerDTO cusInfo = customerDTOMap.get(ownerParty.getCustomerId());
                            cusInfo = cusInfo == null ? new CustomerDTO() : cusInfo;
                            ArrangementPartyDTO ownParty = ArrangementPartyDTO.builder()
                                    .customerId(cusInfo.getId())
                                    .customerName(cusInfo.getName())
                                    .customerIdCard(cusInfo.getIdCard())
                                    .build();
                            orderInfo.setParty(ownParty);
                        }

                        return orderInfo;
                    }).sorted(sortByTradingDate.reversed()).collect(Collectors.toList());

            return new ResponseMessage<>(records, pf.getPaging());
        } else {
            return new ResponseMessage<>(Collections.emptyList(), pf.getPaging());
        }
    }

    @Override
    public Object signContractAndSendSignal(Long arrangementId, JWTHelper jwt) {

        log.info("Signing contract for {} {}", arrangementId, jwt.getSubFromJWT());

        CustomerDTO customer = customerService.findByAccount(jwt.getSubFromJWT(),
                Constant.ACTIVE).block();
        CustomerBrokerWrapper cusBroker = CustomerBrokerWrapper.builder()
                .customer(customer).build();
        log.info("Done finding customer info {}", customer);

        // @Transactional inside to make sure data is inserted in the database
        OrderPlacementDTO orderDTO = signContract(cusBroker, arrangementId, jwt);
        log.info("Done signing contracts for {}", customer);

        ArrangementTypeEnum type = orderDTO.getArrangement() != null
                ? ArrangementTypeEnum.lookForType(orderDTO.getArrangement().getType()) : null;
        NotificationCodeEnum template;
        if (ArrangementTypeEnum.BUY == type) {
            // Then submit data to confirm AFC
            matchingService.sendEventToAFCQueue(orderDTO);
            template = NotificationCodeEnum.BUY_CONTRACT_SIGNED;
        } else {
            template = NotificationCodeEnum.SELL_CONTRACT_SIGNED;
        }

        // run async to find paths and send noti
        buildContractPathsAndPublishNotiEvent(cusBroker, orderDTO, template,
                arrangementId, customer.getAccount());

        //Create arrangement log
        arrangementLogService.createArrangementLog(orderDTO, ArrangementAction.SIGN_CONTRACT);

        return Constant.SUCCESS;
    }

    @Override
    @Transactional
    public OrderPlacementDTO signContract(CustomerBrokerWrapper cusBrokerWrapper, Long arrangementId, JWTHelper jwt) {

        if (cusBrokerWrapper == null || cusBrokerWrapper.getCustomer() == null) {
            throw new NeoFiatsException(ArrangementErrorCode.CUSTOMER_NOT_FOUND,
                    CommonUtils.format("Customer is empty while signing contract for arrangement id {0}",
                            arrangementId));
        }

        CustomerDTO customer = cusBrokerWrapper.getCustomer();

        // validate existence
        Arrangement arr = arrValidator.validateExistence(
                arrRepo.findByIdFetchOperationPartyPricingRelations(arrangementId),
                Constant.ACTIVE);
        arrValidator.validateSigningContract(arr);
        validateJWTToken(arr, customer, jwt.getSubFromJWT());

        ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(arr.getType());
        OrderPlacementDTO orderDTO = ArrangementTypeEnum.SELL == type
                ? castEntityToModelWithCustomer(arr, customer, findSellArrangementId(arr))
                : castEntityToModelWithCustomer(arr, customer, null);
        ArrangementWrapper arrWrapper = new ArrangementWrapper();
        arrWrapper.setCusBroker(CustomerBrokerWrapper.builder()
                .customer(customer).build());

        // validate
        arrValidator.validateAndUpdateModelBeforePerformingAction(orderDTO, type, arrWrapper);

        if (ArrangementTypeEnum.SELL == type) {
            arrValidator.validateIntradayLimit(arr.getTradingDate(),
                    arr.getProductVanillaCode(),
                    orderDTO.getIssuerDTO().getCode(),
                    CommonUtils.bigDecimalToDoubleSilently(arr.getPricing().getPrincipal()));
        }

        // done validation, setting attributes
        ArrangementOperation operation = arr.getOperation();
        operation.setCustomerStatus(RecordStatus.ACTIVE.getStatus());
        operation.setCustomerStatusDate(DateHelper.nowInTimestamp());
        operation = arrOpRepo.save(operation);

        arr.setStatus(ArrangementStatusEnum.INACTIVE.getStatus());
        arr = arrRepo.save(arr);
        log.info("Done save status for arrangement {}. Returning ..", arr.getId());

        if (ArrangementTypeEnum.SELL == type) {

            // find original sell records
            Arrangement finalArr = arr;
            List<ArrangementRelation> relations = arrRelationRepo.findByArrangementIDFetchRelatedArrangement(new ArrayList<Long>() {{
                add(finalArr.getId());
            }});

            if (CollectionUtils.isEmpty(relations)) {
                throw new NeoFiatsException(ArrangementErrorCode.ARRANGEMENT_SELL_ARRANGEMENT_CODE_NOT_FOUND,
                        CommonUtils.format("Cannot find arrangement code during firing HOLD signal to portfolio for arr id {0}",
                                finalArr.getId()));
            }

            Arrangement relatedArrangement = relations.get(0).getRelatedArrangement();

            ArrangementPricing pricing = arr.getPricing();
            PortTransactionDTO model = PortTransactionDTO.builder()
                    .assetCode(relatedArrangement.getCode())
                    .customerId(customer.getId() != null ? String.valueOf(customer.getId()) : null)
                    .customerCode(customer.getAccount())
                    .accountCode(customer.getAccount())
                    .prodVanillaCode(arr.getProductVanillaCode())
                    .prodDerivativeCode(arr.getProductDerivativeCode())
                    .arrangementId(String.valueOf(arr.getId()))
                    .action(PortfolioRetailAction.HOLD.content())
                    .volume(new BigDecimal(String.valueOf(arr.getVolume())))
                    .appliedDate(DateHelper.nowInTimestamp())
                    .channel(Constant.NORMAL_CHANNEL)
                    .arrangementType(type.getTypeStr())
                    .price(pricing != null ? pricing.getPrice() : null)
                    .rate(pricing != null ? CommonUtils.doubleToBigDecimalSilently(pricing.getRate()) : null)
                    .unitPrice(pricing != null ? pricing.getUnitPrice() : null)
                    .principal(pricing != null ? pricing.getTotalMoneyRtm() : null)
                    .build();

            // don't care about status. 200 = OK, 4->5xx FAILED
            ReactiveClientUtils.postForMonoWithModel(webClient, portfolioUrl,
                    "/trans", executor, ResponseMessage.class, log, model).block();
            log.info("signContract - Done sending signal to portfolio service for arr id {} with account {}",
                    orderDTO.getArrangement().getId(), customer.getAccount());
        }

        // setting model for noti outside method
        Long brokerCustomerId = findBrokerCustomerIDIfAvailable(arr);
        cusBrokerWrapper.setBroker(BrokerDTO.builder()
                .customerId(brokerCustomerId).build());

        return orderDTO;
    }

    @Override
    public OrderPlacementDTO placeOrderAndSendNoti(OrderPlacementDTO dto, JWTHelper jwtHelper, Boolean isBrokerReferred) {

        // @Transactional inside
        ArrangementNotificationDTO notiModel = placeOrder(dto, jwtHelper, isBrokerReferred);
        OrderPlacementDTO order = notiModel.getOrderPlacementDTO();

        try {
            // after insert successfully, prepare for sending noti signal
            Integer arrStatus = order.getArrangement().getStatus();
            isBrokerReferred = arrStatus != null
                    && arrStatus.equals(ArrangementStatusEnum.REFERENCE.getStatus());
            Integer arrType = order.getArrangement().getType();
            if (isBrokerReferred) {
                log.info("Detecting broker reference. Sending notification signal ...");

                NotificationCodeEnum template =
                        ArrangementTypeEnum.BUY == ArrangementTypeEnum.lookForType(arrType)
                                ? NotificationCodeEnum.BUY_BROKER_REFERRED
                                : NotificationCodeEnum.SELL_BROKER_REFERRED;

                publisher.publishEvent(new NotificationWrapperEvent(this, notiModel, template));
                log.info("Done sending noti signal for {}", order.getArrangement().getId());
            }

        } catch (Exception e) {
            log.warn("Failed to send notification for {}", jwtHelper.getSubFromJWT());
            log.error("Noti error {}", e.getMessage(), e);
        }

        return order;
    }

    @Override
    @Transactional
    public ArrangementNotificationDTO placeOrder(OrderPlacementDTO dto, JWTHelper jwtHelper, Boolean isBrokerReferred) {

        // set purchaser account
        dto.setPurchaserAccount(jwtHelper.getSubFromJWT());

        ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(dto.getArrangement().getType());
        log.info("Placing order for dto {}", dto);

        // common validations
        arrValidator.validatePlacementAction(type);
        ArrangementWrapper arrWrapper = new ArrangementWrapper();

        if (ArrangementTypeEnum.SELL == type) {
            // create new relations between sell and original sell order
            arrValidator.validateSellTradingCode(dto.getArrangement().getSellArrangementCode(),
                    dto.getArrangement().getSellArrangementId());
            ArrangementRelationDTO relation = new ArrangementRelationDTO();
            relation.setRelatedArrangement(ArrangementDTO.builder()
                    .id(dto.getArrangement().getSellArrangementId()).build());
            relation.setType(ArrangementRelationTypeEnum.SELL_ARRANGEMENT_ID.getType());
            dto.setRelations(new ArrayList<ArrangementRelationDTO>() {{
                add(relation);
            }});
        }

        // decide role first inside ArrangementRoleDTO.role
        CustomerBrokerWrapper cusBroker = arrValidator.validateAndIdentifyBroker(
                dto.getPurchaserAccount(), // decoded from jwt
                dto.getParty().getCustomerId()).block();
        arrWrapper.setCusBroker(cusBroker);

        if (isBrokerReferred && cusBroker.getCustomer().getAccount().equals(dto.getPurchaserAccount())) {
            throw new NeoException(null, ArrangementErrorCode.SELF_REFER_VIOLATED, "Can't self-refer");
        }

        // validate
        arrValidator.validateAndUpdateModelBeforePerformingAction(dto, type, arrWrapper);
        Map<String, PropsResp> attributes = arrWrapper.getAttributes();

        isBrokerReferred = isBrokerReferred(cusBroker.getBroker());
        log.info("Purchaser {} isBrokerReferred? {}", dto.getPurchaserAccount(), isBrokerReferred);

        // now decide to add 1 (owner) or 2 (owner, broker) arrangement party rows
        decideRoleForBuyAndSell(isBrokerReferred, cusBroker, dto);
        int status;
        ArrangementAction action;
        if (isBrokerReferred) { // rm reference
            status = ArrangementStatusEnum.REFERENCE.getStatus();
            action = ArrangementAction.BROKER;
            dto.getArrangement().setAgencyId(cusBroker.getBroker().getAgency().getId());
            dto.getArrangement().setAgencyCode(cusBroker.getBroker().getAgency().getCode());
        } else { // self-serve
            status = ArrangementStatusEnum.INACTIVE.getStatus();
            action = ArrangementAction.PLACE_ORDER;
        }
        dto.getArrangement().setStatus(status);

        // call pricing
        PricingParamDTO pricingParam = buildPricingParam(dto.getArrangement(),
                cusBroker.getCustomer(),
                CollectionUtils.isEmpty(arrWrapper.getAssets()) ? null :
                        arrWrapper.getAssets().get(0));
        PricingBondResultDTO priBondResult = pricingService.calculatePricingBond(pricingParam)
                .stream().findAny().orElseThrow(() -> new NeoFiatsException(ArrangementErrorCode.PRICING_SERVICE_ERROR,
                        CommonUtils.format("Empty pricing info for {0}", dto.getDerivative().getCode())));
        ArrangementPricingDTO systemPricing = castPricingResultToArrPricingDTO(priBondResult);
        systemPricing.setPricingBondResultDTO(priBondResult);
        dto.setPricing(systemPricing);
        log.info("Done updating pricing models for {}", dto.getDerivative().getCode());

        // validate intraday limit
        if (!isBrokerReferred && ArrangementTypeEnum.SELL == type) {
            arrValidator.validateIntradayLimit(dto.getArrangement().getTradingDate(),
                    dto.getDerivative().getProdVanilla().getCode(),
                    dto.getIssuerDTO().getCode(), systemPricing.getTotalReceivedAmount());
        }

        // add one more purchaser party row
        dto.setPurchaserParty(ArrangementPartyDTO.builder()
                .customerId(isBrokerReferred
                        ? cusBroker.getBroker().getCustomerId()
                        : cusBroker.getCustomer().getId())
                .role(ArrangementRoleEnum.PURCHASER.toString())
                .build());

        // generate trading code first
        dto.getArrangement().setCode(arrCodeService.generateTradingCode(cusBroker.getBroker(),
                dto.getDerivative()));

        // now beginning save records
        Arrangement arrangement = new Arrangement();
        ArrangementPricing pricing = new ArrangementPricing();
        ArrangementParty party = new ArrangementParty();
        ArrangementParty brokerParty = isBrokerReferred ? new ArrangementParty() : null;
        ArrangementParty purchaserParty = new ArrangementParty();
        ArrangementOperation operation = new ArrangementOperation();

        ArrangementRelation relation = ArrangementTypeEnum.BUY == type
                ? null : new ArrangementRelation();
        processRecordForBuyAndSell(dto, arrangement, pricing, party,
                brokerParty, purchaserParty, operation, relation,
                cusBroker.getBroker());
        log.info("Done processing records. Snapshotting contract ...");

        // just the wrapper actually
        ArrangementNotificationDTO wrapperModel = ArrangementNotificationDTO.builder()
                .orderPlacementDTO(dto)
                .brokerDTO(cusBroker.getBroker())
                .customerDTO(cusBroker.getCustomer())
                .deadlinePayment(attributes.get(AttributeEnum.DEADLINE_PAYMENT.getVariable()))
                .build();

        if (!isBrokerReferred) {
            // snapshot only when it's not broker reference
            snapshotService.snapshotContract(cusBroker, arrangement, operation, dto, jwtHelper);
        }

        //save arrangement log
        arrangementLogService.createArrangementLog(dto, action);


        return wrapperModel;

    }

    @Override
    public OrderPlacementDTO placeOrderException(OrderPlacementDTO dto) {

        ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(dto.getArrangement().getType());
        log.info("Placing order exception for dto {}", dto);

        arrValidator.validatePlacementAction(type);

        ArrangementWrapper arrWrapper = new ArrangementWrapper();

        if (ArrangementTypeEnum.SELL == type) {
            arrValidator.validateSellTradingCode(dto.getArrangement().getSellArrangementCode(),
                    dto.getArrangement().getSellArrangementId());
            ArrangementRelationDTO relation = new ArrangementRelationDTO();
            relation.setRelatedArrangement(ArrangementDTO.builder()
                    .id(dto.getArrangement().getSellArrangementId()).build());
            relation.setType(ArrangementRelationTypeEnum.SELL_ARRANGEMENT_ID.getType());
            dto.setRelations(new ArrayList<ArrangementRelationDTO>() {{
                add(relation);
            }});
        }

        // decide role first inside ArrangementRoleDTO.role
        CustomerBrokerWrapper cusBroker = new CustomerBrokerWrapper();
        List<CustomerDTO> customer = customerService.retrieveCustomerInfo(Arrays.asList(dto.getParty().getCustomerId()));
        if (CollectionUtils.isEmpty(customer) || customer.size() > 1) {
            throw new NeoException(null, ArrangementErrorCode.CUSTOMER_NOT_UNIQUE,
                    CommonUtils.format("Customer Id {0} is not unique", dto.getParty().getCustomerId()));
        }
        cusBroker.setCustomer(customer.get(0));
        if (dto.getParty().getBroker() != null) {
            cusBroker.setBroker(dto.getParty().getBroker());
        }
        arrWrapper.setCusBroker(cusBroker);

        // validate
        arrValidator.validateAndUpdateModelBeforePerformingAction(dto, type, arrWrapper);
        if (ArrangementTypeEnum.SELL == type) {
            arrValidator.validateIntradayLimit(dto.getArrangement().getTradingDate(),
                    arrWrapper.getPiWrapper().getDerivativeDTO().getProdVanilla().getCode(),
                    dto.getIssuerDTO().getCode(), dto.getPricing().getTotalReceivedAmount());
        }
        Map<String, PropsResp> attributes = arrWrapper.getAttributes();

        boolean isBrokerReferred = isBrokerReferred(cusBroker.getBroker());
        log.info("Purchaser {} isBrokerReferred? {}", dto.getPurchaserAccount(), isBrokerReferred);

        // now decide to add 1 (owner) or 2 (owner, broker) arrangement party rows
        decideRoleForBuyAndSell(isBrokerReferred, cusBroker, dto);
//        int status;
        if (isBrokerReferred) {
            dto.getArrangement().setAgencyId(cusBroker.getBroker().getAgency().getId());
            dto.getArrangement().setAgencyCode(cusBroker.getBroker().getAgency().getCode());
        }
//        } else {
//            status = ArrangementStatusEnum.INACTIVE.getStatus();
//            // only call pricing to build model for normal cases
//        }
        dto.getArrangement().setStatus(ArrangementStatusEnum.INACTIVE.getStatus());

        // now beginning save records
        Arrangement arrangement = new Arrangement();
        ArrangementPricing pricing = new ArrangementPricing();
        ArrangementParty party = new ArrangementParty();
        ArrangementParty brokerParty = isBrokerReferred ? new ArrangementParty() : null;
        ArrangementParty purchaserParty = new ArrangementParty();
        ArrangementOperation operation = new ArrangementOperation();

        ArrangementRelation relation = ArrangementTypeEnum.BUY == type
                ? null : new ArrangementRelation();
        processRecordForBuyAndSell(dto, arrangement, pricing, party,
                brokerParty, purchaserParty, operation, relation,
                cusBroker.getBroker());
        log.info("Done processing records. Snapshotting contract ...");

//        snapshotService.snapshotContract(cusBroker, arrangement, operation, dto, null);

        if (ArrangementTypeEnum.BUY == type) {
            matchingService.sendEventToAFCQueue(dto);
        } else if (ArrangementTypeEnum.SELL == type) {
            PortTransactionDTO model = PortTransactionDTO.builder()
                    .assetCode(dto.getArrangement().getSellArrangementCode())
                    .customerId(String.valueOf(dto.getParty().getCustomerId()))
                    .customerCode(dto.getParty().getCustomer().getAccount())
                    .accountCode(dto.getParty().getCustomer().getAccount())
                    .prodVanillaCode(dto.getArrangement().getProductVanillaCode())
                    .prodDerivativeCode(dto.getArrangement().getProductDerivativeCode())
                    .arrangementId(String.valueOf(arrangement.getId()))
                    .action(PortfolioRetailAction.HOLD.content())
                    .volume(new BigDecimal(String.valueOf(dto.getArrangement().getVolume())))
                    .appliedDate(dto.getArrangement().getTradingDate())
                    .channel(Constant.NORMAL_CHANNEL)
                    .arrangementType(type.getTypeStr())
                    .price(pricing != null ? pricing.getPrice() : null)
                    .rate(pricing != null ? CommonUtils.doubleToBigDecimalSilently(pricing.getRate()) : null)
                    .unitPrice(pricing != null ? pricing.getUnitPrice() : null)
                    .principal(pricing != null ? pricing.getTotalMoneyRtm() : null)
                    .build();

            // don't care about status. 200 = OK, 4->5xx FAILED
            ReactiveClientUtils.postForMonoWithModel(webClient, portfolioUrl,
                    "/trans", executor, ResponseMessage.class, log, model).subscribe();
            log.info("Done sending signal to portfolio service for arr id {} with account {}",
                    arrangement.getId(), dto.getParty().getAccount());
        }

        return dto;

    }

    @Override
    @Transactional
    public OrderPlacementDTO placeCollateral(CollateralDTO dto) {
        ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(dto.getType());
        log.info("Placing collateral for dto {}", dto);

        DateHelper.validateHoliday(restTemplate,
                DateHelper.formatDateSilently(dto.getCollateralDate(),
                        Constant.FORMAT_SQLSERVER_SHORT), infoUrl);

        Timestamp now = DateHelper.nowInTimestamp();

        Arrangement arrangement = new Arrangement();
        arrangement.setCode(dto.getAssetCode());
        arrangement.setTradingDate(dto.getCollateralDate());
        arrangement.setType(dto.getType());
        arrangement.setVolume(dto.getMortgageVolume());
        arrangement.setProductDerivativeId(dto.getDerivativeId());
        arrangement.setProductDerivativeCode(dto.getDerivativeCode());
        arrangement.setProductVanillaId(dto.getVanillaId());
        arrangement.setProductVanillaCode(dto.getVanillaCode());
        arrangement.setStatus(ArrangementStatusEnum.ACTIVE.getStatus());
        arrangement.insertExtra(now);
        arrangement = arrRepo.save(arrangement);

        ArrangementDTO arrDTO = new ArrangementDTO();
        BeanUtils.copyProperties(arrangement, arrDTO);

        ArrangementParty party = new ArrangementParty();
        party.setCustomerId(dto.getPartyId());
        party.setRole(ArrangementRoleEnum.OWNER.toString());
        party.setArrangement(arrangement);
        party = arrPartyRepo.save(party);

        ArrangementPartyDTO partyDTO = new ArrangementPartyDTO();

        ArrangementOperation operation = new ArrangementOperation();
        operation.setCustomerStatus(1);
        operation.setCustomerStatusDate(dto.getCollateralDate());
        if (dto.getType() == ArrangementTypeEnum.COLLATERAL.getType()) {
            operation.setCollateralStatus(1);
            operation.setCollateralStatusDate(dto.getCollateralDate());
        } else if (dto.getType() == ArrangementTypeEnum.RELEASE.getType()) {
            operation.setReleaseStatus(1);
            operation.setReleaseStatusDate(dto.getCollateralDate());
        }
        operation.setArrangement(arrangement);
        operation = arrOpRepo.save(operation);

        ArrangementOperationDTO opsDTO = new ArrangementOperationDTO();
        BeanUtils.copyProperties(operation, opsDTO);

        ArrangementRelation relation = new ArrangementRelation();
        relation.insertExtra(now);
        relation.setArrangement(arrangement);
        relation.setType(ArrangementRelationTypeEnum.SELL_ARRANGEMENT_ID.getType());
        relation.setRelatedArrangement(arrRepo.findById(dto.getAssetId()).get());
        relation = arrRelationRepo.save(relation);

        ArrangementRelationDTO relationDTO = new ArrangementRelationDTO();
        BeanUtils.copyProperties(relation, relationDTO);

        ArrangementPricing pricing = new ArrangementPricing();
        pricing.setPrice(dto.getMortgagePrice());
        pricing.setArrangement(arrangement);
        pricing = arrPriRepo.save(pricing);

        ArrangementPricingDTO pricingDTO = new ArrangementPricingDTO();
        BeanUtils.copyProperties(pricing, pricingDTO);

        String arrType = null;
        String action = null;
        if (dto.getType().equals(ArrangementTypeEnum.COLLATERAL.getType())) {
            arrType = ArrangementTypeEnum.COLLATERAL.getTypeStr();
            action = "BLOCK";
        } else if (dto.getType().equals(ArrangementTypeEnum.RELEASE.getType())) {
            arrType = ArrangementTypeEnum.RELEASE.getTypeStr();
            action = "RELEASE";
        }

        PortTransactionDTO transactionDTO = PortTransactionDTO.builder()
                .customerCode(dto.getPartyAccount())
                .accountCode(dto.getPartyAccount())
                .prodVanillaCode(dto.getVanillaCode())
                .prodDerivativeCode(dto.getDerivativeCode())
                .assetCode(dto.getAssetCode())
                .arrangementId(arrangement.getId().toString())
                .arrangementType(arrType)
                .channel("BACK")
                .action(action)
                .volume(BigDecimal.valueOf(dto.getMortgageVolume()))
                .appliedDate(dto.getCollateralDate())
                .build();
        portfolioService.makePortfolioTransaction(transactionDTO);

        OrderPlacementDTO result = new OrderPlacementDTO();
        result.setArrangement(arrDTO);
        result.setParty(partyDTO);
        result.setOperation(opsDTO);
        result.setPricing(pricingDTO);

        List<ArrangementRelationDTO> relationDTOList = new ArrayList<>();
        relationDTOList.add(relationDTO);
        result.setRelations(relationDTOList);

        return result;
    }

    @Override
    @Transactional
    public OrderPlacementDTO createMatchingRecords(Arrangement arrangement) {

        log.info("Create matching records for {}", arrangement.getId());

        // now call private method
        arrangement = createMatching(arrangement);

        return ConverterUtils.castArrangementToOrder(arrangement, null);
    }

    @Override
    @Transactional
    public OrderPlacementDTO customerConfirmForReference(Long arrangementId, JWTHelper jwt) {

        CustomerDTO customer = customerService.findByAccount(jwt.getSubFromJWT(), Constant.ACTIVE).block();
        log.info("Done finding customer info {}", customer);

        Optional<Arrangement> arrOptional = arrRepo.findByIdFetchOperationPartyPricingRelations(arrangementId);

        // validation
        Arrangement arr = arrValidator.validateExistence(arrOptional, Constant.ACTIVE);
        validateJWTToken(arr, customer, jwt.getSubFromJWT());
        arrValidator.validateCustomConfirmationForReference(arr);

        // prepare models
        OrderPlacementDTO orderDTO = ArrangementTypeEnum.SELL == ArrangementTypeEnum.lookForType(arr.getType())
                ? castEntityToModelWithCustomer(arr, customer, findSellArrangementId(arr))
                : castEntityToModelWithCustomer(arr, customer, null);
        ArrangementWrapper arrWrapper = new ArrangementWrapper();
        arrWrapper.setCusBroker(CustomerBrokerWrapper.builder()
                .customer(customer).build());
        ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(arr.getType());

        // validate and update model
        arrValidator.validateAndUpdateModelBeforePerformingAction(orderDTO, type, arrWrapper);

        // building model and call pricing + update pricing result
        PricingParamDTO pricingParam = buildPricingParam(orderDTO.getArrangement(),
                customer, CollectionUtils.isEmpty(arrWrapper.getAssets()) ? null :
                        arrWrapper.getAssets().get(0));
        PricingBondResultDTO priBondResult = pricingService.calculatePricingBond(pricingParam)
                .stream().findAny().orElseThrow(() -> new NeoFiatsException(ArrangementErrorCode.PRICING_SERVICE_ERROR,
                        CommonUtils.format("Empty pricing info for {0}", orderDTO.getDerivative().getCode())));
        ArrangementPricingDTO systemPricing = castPricingResultToArrPricingDTO(priBondResult);
        systemPricing.setPricingBondResultDTO(priBondResult);
        orderDTO.setPricing(systemPricing);
        log.info("Done updating pricing models for {}", orderDTO.getDerivative().getCode());

        // validate intraday limit
        if (ArrangementTypeEnum.SELL == type) {
            arrValidator.validateIntradayLimit(arr.getTradingDate(),
                    arr.getProductVanillaCode(),
                    orderDTO.getIssuerDTO().getCode(),
                    systemPricing.getTotalReceivedAmount());
        }

        // now update pricing
        setPricingData(arr.getPricing(), orderDTO.getPricing(), arr);

        // and update arrangement
        arr.setStatus(ArrangementStatusEnum.INACTIVE.getStatus());
        arr = arrRepo.save(arr);
        log.info("Setting arrangement record status to {}", arr.getStatus());

        // set more attributes for model
        orderDTO.getArrangement().setStatus(arr.getStatus());
        snapshotService.snapshotContract(arrWrapper.getCusBroker(), arr,
                arr.getOperation(), orderDTO, jwt);

        //Create arrangement log
        arrangementLogService.createArrangementLog(orderDTO, ArrangementAction.CUSTOMER_CONFIRM);

        return orderDTO;
    }

    @Override
    public OrderPlacementDTO cancelArrangementAndSendSignal(Long arrangementId, JWTHelper jwt, boolean jobAutoExpire) {

        log.info("cancelArrangementAndSendSignal - Begin cancellation arrangement for {}", arrangementId);

        CustomerDTO customer = null;
        if (jwt != null) {
            customer = customerService.findByAccount(jwt.getSubFromJWT(),
                    Constant.INACTIVE).block();
            log.info("Done finding customer info {} by jwt", customer);
        } else {

            ArrangementParty party = arrPartyRepo.findFirstByArrangementIdAndRole(arrangementId, ArrangementRoleEnum.OWNER.toString())
                    .orElseThrow(() -> new NeoFiatsException(ArrangementErrorCode.CUSTOMER_NOT_FOUND,
                            CommonUtils.format("Cannot find customer for arrangement id {0}", arrangementId)));

            customer = customerService.retrieveCustomerInfo(party.getCustomerId());
            log.info("Done finding customer info {} by customerID {}", customer, party.getCustomerId());
        }

        // update status save inside @Transactional
        List<Arrangement> arrangements = cancelArrangement(arrangementId, customer, jwt, jobAutoExpire);

        // casting only the original order
        Arrangement originalArr = arrangements.stream()
                .filter(arr -> arr.getId().equals(arrangementId))
                .findAny().orElse(null);
        OrderPlacementDTO originalOrder = ConverterUtils.castArrangementToOrder(originalArr, customer);
        if (!jobAutoExpire) {

            //Create arrangement log
            arrangementLogService.createArrangementLog(originalOrder, ArrangementAction.CANCEL);

            // send noti
            buildModelAndSendNoti(arrangements, customer);
        }

        log.info("Done cancelled order for {}. Returning ...", arrangementId);


        return originalOrder; // return only the original order
    }

    @Override
    @Transactional
    public List<Arrangement> cancelArrangement(Long arrangementId, CustomerDTO customer, JWTHelper jwt,
                                               boolean jobAutoExpire) {

        Timestamp now = DateHelper.nowInTimestamp();

        log.info("cancelArrangement - Now find and cancel the order {} at {}",
                arrangementId, DateHelper.formatDateSilently(now));

        Arrangement originalArr = arrValidator.validateExistence(arrRepo.findByIDFetchPartyOperationPricing(arrangementId),
                Constant.ACTIVE);

        if (!jobAutoExpire) { // for auto-expire job
            validateJWTToken(originalArr, customer, jwt.getSubFromJWT());
        }
        arrValidator.validateCancellation(originalArr, jobAutoExpire);

        List<Arrangement> arrangements = new ArrayList<>();
        // set this record to CANCELLED first
        arrangements.add(originalArr);

        // check if there are other related records (matching + counter order)
        List<ArrangementRelation> relations = arrRelationRepo.findByRelatedArrangementIdAndType(originalArr.getId(),
                ArrangementRelationTypeEnum.MATCHING.getType());

        if (!CollectionUtils.isEmpty(relations)) {

            // meaning this part of matching records
            Set<Long> matchingIDs = relations.stream().map(r -> r.getArrangement().getId())
                    .collect(Collectors.toSet());
            List<ArrangementRelation> totalRelations = arrRelationRepo.findByArrangementIdInFetchAll(matchingIDs);

            // set all matching records to cancelled
            Arrangement matching = totalRelations.stream()
                    .filter(CommonUtils.distinctByKey(r -> r.getArrangement().getId()))
                    .map(relation -> {
                        Arrangement tmpArr = relation.getArrangement();
                        return tmpArr;
                    })
                    .findAny().orElse(null);

            if (matching != null) {
                arrangements.add(matching);
            }

            // now find all origin & counter orders excluding the requested one
            Arrangement counterArrangement = totalRelations.stream()
                    .filter(CommonUtils.distinctByKey(r -> r.getRelatedArrangement().getId()))
                    .filter(r -> !r.getRelatedArrangement().getId().equals(originalArr.getId())) // exclude the requested record
                    .map(relation -> {
                        Arrangement tmpArr = relation.getRelatedArrangement();
                        return tmpArr;
                    })
                    .findAny().orElse(null);

            if (counterArrangement != null) {
                arrangements.add(counterArrangement);
            }

        }

        OrderPlacementDTO model;
        if (originalArr.getStatus().equals(ArrangementStatusEnum.INACTIVE.getStatus())
                && ArrangementTypeEnum.BUY.getType().equals(originalArr.getType())
                && originalArr.getOperation().getCustomerStatus().equals(RecordStatus.ACTIVE.getStatus())) {

            // active = 0, type = buy -> to matching cancel active order on local queue
            model = ConverterUtils.castArrangementToOrder(originalArr, customer);
            ReactiveClientUtils.deleteForMono(webClient, matchingUrl, "/orders/{mechanismType}/{productCode}/{id}",
                    executor, String.class, log, MechanismType.AFC, originalArr.getProductDerivativeCode(),
                    originalArr.getId()).block();
            arrangementLogService.createArrangementLog(model, ArrangementAction.CANCEL_AFC);
            log.info("Done sending signal {} to matching to cancel order {}",
                    ArrangementAction.CANCEL_AFC, originalArr.getId());

        } else if (originalArr.getStatus().equals(ArrangementStatusEnum.ACTIVE.getStatus())
                && ArrangementTypeEnum.BUY.getType().equals(originalArr.getType())
                && originalArr.getOperation().getCustomerStatus().equals(RecordStatus.ACTIVE.getStatus())
                && originalArr.getOperation().getPaymentStatus().equals(RecordStatus.INACTIVE.getStatus())) {

            // active = 1, payment = 0, and type = buy -> to portfolio UNCONFIRM + log UNCONFIRM
            model = ConverterUtils.castArrangementToOrder(originalArr,
                    CustomerDTO.builder().account(retailSourceAccount).build());
            portfolioService.sendEventToPortfolioQueue(model, PortfolioRetailAction.CANCEL_CONFIRM);
            arrangementLogService.createArrangementLog(model, ArrangementAction.UNCONFIRM);
            log.info("Done sending signal {} to portfolio to cancel order {}",
                    ArrangementAction.UNCONFIRM, originalArr.getId());

        } else if (originalArr.getStatus().equals(ArrangementStatusEnum.ACTIVE.getStatus())
                && ArrangementTypeEnum.SELL.getType().equals(originalArr.getType())) {
            final Map<Long, Arrangement> originalSells = findOriginalSellOrders(new ArrayList<Arrangement>() {{
                add(originalArr);
            }});
            Arrangement sellArrangement = originalSells.get(originalArr.getId());
            if (sellArrangement != null) {
                log.info("SELL - status = 1 Sending signal for portfolio cancellation for originalArr id {}",
                        originalArr.getId());

                // active = 1, and type = sell -> to portfolio UNHOLD
                model = ConverterUtils.castArrangementToOrder(originalArr, customer);
                model.getArrangement().setSellArrangementCode(sellArrangement.getCode());
                portfolioService.sendEventToPortfolioQueue(model, PortfolioRetailAction.UNHOLD);
                arrangementLogService.createArrangementLog(model, ArrangementAction.UNHOLD);
                log.info("Done sending signal {} to matching to cancel order {}",
                        ArrangementAction.UNHOLD, originalArr.getId());
            }
        }

        // now save status
        arrangements = arrangements.stream().peek(a -> {
            a.setExpiredDate(now);
            a.setStatus(ArrangementStatusEnum.CANCELLED.getStatus());
        }).collect(Collectors.toList());

        arrangements = arrRepo.saveAll(arrangements);
        log.info("Setting arrangement record status of {} to CANCELLED {}",
                originalArr.getId(), ArrangementStatusEnum.CANCELLED.getStatus());

        return arrangements;
    }

    @Override
    public Map<Long, Arrangement> findOriginalSellOrders(List<Arrangement> arrangements) {

        if (CollectionUtils.isEmpty(arrangements)) {
            return Collections.emptyMap();
        }

        Set<Long> originalSellIds = arrangements.stream()
                .filter(a -> ArrangementTypeEnum.SELL.getType().equals(a.getType()))
                .map(Arrangement::getId)
                .collect(Collectors.toSet());

        List<ArrangementRelation> sellRelations = CollectionUtils.isEmpty(originalSellIds)
                ? null : arrRelationRepo.findByArrangementIDFetchRelatedArrangement(originalSellIds);

        // map of current id - sell origin arrangement
        Map<Long, Arrangement> originalSells = new HashMap<>();
        if (!CollectionUtils.isEmpty(sellRelations)) {
            originalSells = sellRelations.stream()
                    .collect(Collectors.toMap(r -> r.getArrangement().getId(),
                            ArrangementRelation::getRelatedArrangement, (o, n) -> n));
        }

        return originalSells;
    }

    @Override
    public List<ArrangementOperation> findEquivalentMatchingOperations(List<Arrangement> originalArrs) {

        if (CollectionUtils.isEmpty(originalArrs)) {
            return Collections.emptyList();
        }

        List<Long> originalIds = originalArrs.stream().map(Arrangement::getId).collect(Collectors.toList());

        List<ArrangementRelation> relations = arrRelationRepo.findByRelatedArrangementIDFetchArrangementOperation(originalIds);

        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }

        Set<Long> equivalentRelationIds = relations.stream()
                .filter(relation -> originalIds.contains(relation.getRelatedArrangement().getId()))
                .map(ArrangementRelation::getId).collect(Collectors.toSet());

        // only get the equivalent operation
        return relations.stream().flatMap(relation -> relation.getArrangement().getOperations().stream())
                .filter(operation -> equivalentRelationIds.contains(operation.getArrangementRelationId()))
                .filter(CommonUtils.distinctByKey(ArrangementOperation::getId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ArrangementOperation> findAllRelatedOperations(List<Arrangement> originalArrs) {

        if (CollectionUtils.isEmpty(originalArrs)) {
            return Collections.emptySet();
        }

        List<Long> originalIds = originalArrs.stream().map(Arrangement::getId).collect(Collectors.toList());

        List<ArrangementRelation> originalRelations = arrRelationRepo.findByRelatedArrangementIdInAndType(originalIds,
                ArrangementRelationTypeEnum.MATCHING.getType());

        if (CollectionUtils.isEmpty(originalRelations)) {
            return Collections.emptySet();
        }

        Set<Long> matchingIds = originalRelations.stream().map(r -> r.getArrangement().getId()).collect(Collectors.toSet());

        List<ArrangementRelation> allRelations = arrRelationRepo.findByArrangementIdInAndType(matchingIds,
                ArrangementRelationTypeEnum.MATCHING.getType());

        if (CollectionUtils.isEmpty(allRelations)) {
            return Collections.emptySet();
        }

        Set<Long> allIds = new HashSet<>();
        allRelations.forEach(r -> {
            allIds.add(r.getArrangement().getId());
            allIds.add(r.getRelatedArrangement().getId());
        });

        List<ArrangementOperation> allOperations = arrOpRepo.findByArrangementIdInFetchArrangements(allIds);

        log.info("For {}, Got total operation id {}", originalIds,
                allOperations.stream().map(ArrangementOperation::getId).collect(Collectors.toList()));

        return allOperations;
    }

    @Override
    public List<Arrangement> findMatchingArrangements(List<Arrangement> originalArrs) {

        if (CollectionUtils.isEmpty(originalArrs)) {
            return Collections.emptyList();
        }

        List<Long> originalIds = originalArrs.stream().map(Arrangement::getId).collect(Collectors.toList());

        List<ArrangementRelation> relations = arrRelationRepo.findByRelatedArrangementIDFetchArrangementOperation(originalIds);

        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }

        return relations.stream().map(ArrangementRelation::getArrangement)
                .filter(CommonUtils.distinctByKey(Arrangement::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Arrangement> findOriginalAndCounterArrangements(List<Arrangement> originalArrs) {

        List<Arrangement> matchingRecords = findMatchingArrangements(originalArrs);

        if (CollectionUtils.isEmpty(matchingRecords)) {
            return Collections.emptyList();
        }

        Set<Long> matchingIDs = matchingRecords.stream().map(Arrangement::getId).collect(Collectors.toSet());

        List<ArrangementRelation> relations = arrRelationRepo.findByArrangementIDFetchRelatedArrangementOperation(matchingIDs);

        return relations.stream().map(ArrangementRelation::getRelatedArrangement)
                .filter(CommonUtils.distinctByKey(Arrangement::getId)).collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<Arrangement>> findOriginalAndCounterArrangementsGroupingByMatchingID(List<Arrangement> originalArrs) {

        // find matching records first
        List<Arrangement> matchingRecords = findMatchingArrangements(originalArrs);

        if (CollectionUtils.isEmpty(matchingRecords)) {
            return Collections.emptyMap();
        }

        // collect and find relations by matching ids
        Set<Long> matchingIDs = matchingRecords.stream().map(Arrangement::getId).collect(Collectors.toSet());
        List<ArrangementRelation> relations = arrRelationRepo.findByArrangementIDFetchRelatedArrangementOperation(matchingIDs);

        // grouping by matching id
        Map<Long, List<ArrangementRelation>> relationsByMatchingID = relations.stream()
                .collect(Collectors.groupingBy(relation -> relation.getArrangement().getId()));

        // then transform to a list of arrangements
        return relationsByMatchingID.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(ArrangementRelation::getRelatedArrangement)
                                .filter(CommonUtils.distinctByKey(Arrangement::getId))
                                .collect(Collectors.toList())));
    }

    private void buildContractPathsAndPublishNotiEvent(CustomerBrokerWrapper cusBroker, OrderPlacementDTO orderDTO,
                                                       NotificationCodeEnum template, Long arrangementId, String account) {

        CompletableFuture.runAsync(() -> {
            List<String> filePaths = snapshotService.listAvailableFiles(arrangementId, account)
                    .map(file -> account + File.separator
                            + arrangementId.toString() + File.separator
                            + file.getFileCode() + ArrConstant.PDF_EXTENSION)
                    .collectList().block();
            log.info("Using file path {} as attachment paths", filePaths);
            publisher.publishEvent(new NotificationBrokerEvent(this, cusBroker, orderDTO, template,
                    filePaths));
        }, executor);
    }

    private void buildModelAndSendNoti(List<Arrangement> arrangements, CustomerDTO customer) {

        try {

            // finding rows first, this needs to be done during hibernate sessions
            List<ArrangementNotificationDTO> notiModels = Flux.fromIterable(arrangements)
                    .filter(tmpArr -> {

                        // get all rows that are not from ORGANIZATION and !matching
                        boolean fromOrganization = tmpArr.getParties().stream()
                                .anyMatch(p -> ArrangementRoleEnum.ORGANIZATION.toString().equals(p.getRole()));

                        return !fromOrganization && !ArrangementTypeEnum.MATCHING.getType().equals(tmpArr.getType());
                    })
                    .map(tmpArr -> {

                        OrderPlacementDTO orderDTO = castEntityToModelWithCustomer(tmpArr, customer, null);
                        Long brokerCustomerId = findBrokerCustomerIDIfAvailable(tmpArr);

                        return ArrangementNotificationDTO.builder().customerDTO(customer)
                                .brokerDTO(BrokerDTO.builder().customerId(brokerCustomerId).build())
                                .orderPlacementDTO(orderDTO)
                                .build();
                    })
                    .collectList().block();

            // now send event async
            if (!CollectionUtils.isEmpty(notiModels)) {
                notiModels.forEach(notiModel -> {
                    Integer type = notiModel.getOrderPlacementDTO() != null
                            && notiModel.getOrderPlacementDTO().getArrangement() != null
                            ? notiModel.getOrderPlacementDTO().getArrangement().getType() : null;
                    NotificationCodeEnum template = ArrangementTypeEnum.BUY == ArrangementTypeEnum.lookForType(type)
                            ? NotificationCodeEnum.BUY_CANCELLED
                            : NotificationCodeEnum.SELL_CANCELLED;
                    publisher.publishEvent(new NotificationBrokerEvent(this,
                            CustomerBrokerWrapper.builder()
                                    .customer(notiModel.getCustomerDTO())
                                    .broker(notiModel.getBrokerDTO())
                                    .build(),
                            notiModel.getOrderPlacementDTO(), template));
                });
            }

        } catch (Exception e) {
            log.error("Error while building model to send noti : {}", e.getMessage());
            log.error("Stack trace", e);
        }
    }

    private Long findBrokerCustomerIDIfAvailable(Arrangement arr) {
        return arr.getParties().stream()
                .filter(party -> ArrangementRoleEnum.BROKER.toString().equals(party.getRole()))
                .map(ArrangementParty::getCustomerId)
                .findAny().orElse(null);
    }

    private Arrangement createMatching(Arrangement arr) {

        // validate
        arrValidator.validateMatching(arr);

        Map<ArrangementTypeEnum, Arrangement> records = new HashMap<>();
        Map<ArrangementTypeEnum, ArrangementParty> parties = new HashMap<>();

        // generate counter arrangement
        generateCounterArrangement(arr, records, parties);

        // insert for matching
        processRecordForMatching(arr, records, parties);

        // now set to Active
        arr.setStatus(ArrangementStatusEnum.ACTIVE.getStatus());
        arr = arrRepo.save(arr);

        log.info("Done creating matching records for {}", arr.getId());

        return arr;
    }

    private OrderPlacementDTO generateCounterArrangement(
            Arrangement arr,
            Map<ArrangementTypeEnum, Arrangement> records,
            Map<ArrangementTypeEnum, ArrangementParty> parties) {

        if (arr == null) {
            throw new NeoFiatsException("Invalid arrangement record (possibly server error)");
        }

        // try to recover
        if (records == null) {
            records = new HashMap<>();
        }

        ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(arr.getType());
        ArrangementTypeEnum counterType = swapTypeForBuyAndSell(type);

        // prepare the counter arrangement model
        OrderPlacementDTO counterModel = prepareCounterModel(arr, counterType);
        log.info("Done preparing counter model for arrangement {}", arr.getId());

        // now perform real insertion (counterPartyDTO will not be inserted)
        Arrangement counterArr = new Arrangement();
        ArrangementPricing counterPri = new ArrangementPricing();
        ArrangementParty counterParty = new ArrangementParty();
        ArrangementOperation counterOp = new ArrangementOperation();
        processRecordForBuyAndSell(counterModel, counterArr, counterPri, counterParty,
                null, null, counterOp, null, null);

        log.info("Done placing counter arrangement {} for {}. Returning ...",
                arr.getId(), counterArr.getId());

        // now storing records for later usage
        records.put(type, arr);
        records.put(counterType, counterArr);

        ArrangementParty party = arr.getParty(ArrangementRoleEnum.OWNER);
        if (party == null) {
            throw new NeoFiatsException(
                    CommonUtils.format("Invalid arrangement owner party record for {0}",
                            arr.getId()));
        }

        parties.put(type, party);
        parties.put(counterType, counterParty);

        return counterModel;

    }

    private List<ArrangementRelation> generateRelations(Map<ArrangementTypeEnum, Arrangement> arrMap,
                                                        ArrangementRelationTypeEnum relationType) {

        if (CollectionUtils.isEmpty(arrMap) || relationType == null) {
            throw new NeoFiatsException("Invalid arrangement record (or type)");
        }

        Timestamp now = DateHelper.nowInTimestamp();
        List<ArrangementRelation> results = new ArrayList<>();
        if (ArrangementRelationTypeEnum.MATCHING == relationType) {

            Arrangement buy = arrMap.get(ArrangementTypeEnum.BUY);
            Arrangement sell = arrMap.get(ArrangementTypeEnum.SELL);
            Arrangement matching = arrMap.get(ArrangementTypeEnum.MATCHING);

            if (buy == null || sell == null || matching == null) {
                throw new NeoFiatsException("Invalid buy, sell or matching arrangement record");
            }

            // row for buying
            ArrangementRelation buyRelation = new ArrangementRelation();
            buyRelation.setArrangement(matching);
            buyRelation.setRelatedArrangement(buy);
            buyRelation.setType(ArrangementRelationTypeEnum.MATCHING.getType());
            buyRelation.insertExtra(now);
            buyRelation = arrRelationRepo.save(buyRelation);
            results.add(buyRelation);
            log.info("Done saved relation id {}", buyRelation.getId());

            // row for selling
            ArrangementRelation sellRelation = new ArrangementRelation();
            sellRelation.setArrangement(matching);
            sellRelation.setRelatedArrangement(sell);
            sellRelation.setType(ArrangementRelationTypeEnum.MATCHING.getType());
            sellRelation.insertExtra(now);
            sellRelation = arrRelationRepo.save(sellRelation);
            results.add(sellRelation);
            log.info("Done saved relation id {}", sellRelation.getId());
        }

        return results;

    }

    private OrderPlacementDTO prepareCounterModel(Arrangement arr,
                                                  ArrangementTypeEnum counterType) {

        ArrangementPricing pricing = arr.getPricing();
        ArrangementDTO counterArrDTO = new ArrangementDTO();
        BeanUtils.copyProperties(arr, counterArrDTO,
                "id", "createdDate", "updatedDate", "status");
        counterArrDTO.setStatus(ArrangementStatusEnum.ACTIVE.getStatus());
        counterArrDTO.setType(counterType.getType());

        ArrangementPricingDTO counterPriDTO = new ArrangementPricingDTO();
        BeanUtils.copyProperties(pricing, counterPriDTO, "id");
        counterPriDTO.setPrice(CommonUtils.bigDecimalToDoubleSilently(pricing.getPrice()));
        counterPriDTO.setUnitPrice(CommonUtils.bigDecimalToDoubleSilently(pricing.getUnitPrice()));

        CustomerDTO organization = customerService.findByAccount(ArrConstant.HDBS_ACCOUNT, Constant.INACTIVE)
                .block();
        log.info("Done finding {} account with id {}",
                ArrConstant.HDBS_ACCOUNT, organization.getId());

        // counterPurchaserPartyDTO will not be inserted
        ArrangementPartyDTO counterPartyDTO = new ArrangementPartyDTO();
        counterPartyDTO.setCustomerId(organization.getId());
        counterPartyDTO.setRole(ArrangementRoleEnum.ORGANIZATION.toString());

        ProdDerivativeDTO counterDerivativeDTO = pdService.findByID(arr.getProductDerivativeId()).block();

        // generate counter trading code
        counterArrDTO.setCode(arrCodeService.generateTradingCode(null, counterDerivativeDTO));

        Timestamp now = DateHelper.nowInTimestamp();
        ArrangementOperationDTO counterOp = ArrangementOperationDTO.builder()
                .customerStatus(RecordStatus.ACTIVE.getStatus())
                .customerStatusDate(now)
                .contractStatus(RecordStatus.ACTIVE.getStatus())
                .contractStatusDate(now)
                .build();

        return OrderPlacementDTO.builder()
                .arrangement(counterArrDTO).pricing(counterPriDTO)
                .party(counterPartyDTO)
                .derivative(counterDerivativeDTO)
                .operation(counterOp)
                .build();
    }

    private void processRecordForMatching(Arrangement originalArr,
                                          Map<ArrangementTypeEnum, Arrangement> records,
                                          Map<ArrangementTypeEnum, ArrangementParty> parties) {

        if (originalArr == null) {
            throw new NeoFiatsException("Invalid arrangement record");
        }

        String[] codes = originalArr.getCode().split("\\.");
        log.info("Done generated counter arrangement. Now creating matching for arr id {}",
                originalArr.getId());

        Timestamp now = DateHelper.nowInTimestamp();


        // manually pass in to control attributes
        Arrangement matching = new Arrangement(
                arrCodeService.generateTradingCode(codes[0], codes[1]),
                ArrangementTypeEnum.MATCHING.getType(),
                originalArr.getTradingDate(), originalArr.getExpiredDate(),
                originalArr.getVolume(), originalArr.getChannel(),
                originalArr.getProductDerivativeId(), originalArr.getProductDerivativeCode(),
                originalArr.getProductVanillaId(), originalArr.getProductVanillaCode(),
                originalArr.getProductAgreementId(), originalArr.getProductAgreementCode(),
                originalArr.getListedType(), originalArr.getAgencyId(), originalArr.getAgencyCode());
        matching.insertExtra(now);
        matching.setStatus(ArrangementStatusEnum.ACTIVE.getStatus());
        matching = arrRepo.save(matching);
        records.put(ArrangementTypeEnum.MATCHING, matching);

        Long matchingId = matching.getId();
        log.info("Done generated matching arrangement record with id {}", matchingId);

        // save relations
        List<ArrangementRelation> relations = generateRelations(records,
                ArrangementRelationTypeEnum.MATCHING);
        log.info("Done generated relations for matching {}. Converting now ...", matchingId);

        // save operations
        List<ArrangementOperation> matchingOperations = new ArrayList<>();
        for (ArrangementRelation relation : relations) {
            log.info("looping for relation id {}", relation.getId());
            ArrangementOperation matchingOperation = new ArrangementOperation();
            matchingOperation.setArrangement(matching);
            matchingOperation.setContractStatus(RecordStatus.ACTIVE.getStatus());
            matchingOperation.setContractStatusDate(now);
            matchingOperation.setCustomerStatus(RecordStatus.ACTIVE.getStatus());
            matchingOperation.setCustomerStatusDate(now);
            matchingOperation.setArrangementRelationId(relation.getId());
            matchingOperations.add(matchingOperation);
        }
        matchingOperations = arrOpRepo.saveAll(matchingOperations);
        log.info("Done generated operations for matching {}", matchingId);

        // save pricing which should be fetched in outer service already
        ArrangementPricing originalPricing = originalArr.getPrices()
                .stream().findAny()
                .orElseThrow(() -> new NeoFiatsException(ArrangementErrorCode.ARRANGEMENT_PRICING_NOT_FOUND,
                        CommonUtils.format("Pricing of ID {0} is not available while creating matching records",
                                originalArr.getId())));
        ArrangementPricing matchingPricing = new ArrangementPricing();
        BeanUtils.copyProperties(originalPricing, matchingPricing,
                "arrangement", "id");
        matchingPricing.setArrangement(matching);
        matchingPricing = arrPriRepo.save(matchingPricing);
        log.info("Done generated pricing for matching {}", matchingId);

        // should be fetched and saved before, as well as not null
        List<ArrangementParty> matchingParties = new ArrayList<>();
        ArrangementParty buyParty = parties.get(ArrangementTypeEnum.BUY);
        ArrangementParty matchingBuyParty = new ArrangementParty();
        BeanUtils.copyProperties(buyParty, matchingBuyParty,
                "arrangement", "id", "role");
        matchingBuyParty.setArrangement(matching);
        matchingBuyParty.setRole(ArrangementRoleEnum.BUYER.toString());
        matchingParties.add(matchingBuyParty);

        ArrangementParty sellParty = parties.get(ArrangementTypeEnum.SELL);
        ArrangementParty matchingSellParty = new ArrangementParty();
        BeanUtils.copyProperties(sellParty, matchingSellParty,
                "arrangement", "id", "role");
        matchingSellParty.setArrangement(matching);
        matchingSellParty.setRole(ArrangementRoleEnum.SELLER.toString());
        matchingParties.add(matchingSellParty);

        matchingParties = arrPartyRepo.saveAll(matchingParties);
        log.info("Done generated parties for matching {} and original id {}",
                matching.getId(), originalArr.getId());

    }

    private void validateJWTToken(Arrangement arr, CustomerDTO customer, String account) {
        // only OWNER can cancel their orders
        List<Long> arrCustomerIds = arr.getParties().stream()

                // most of the cases, it would be OWNER (but not sure for other arrangement types like Collateral, Release)
                .filter(party -> !ArrangementRoleEnum.BROKER.toString().equals(party.getRole())
                        && !ArrangementRoleEnum.PURCHASER.toString().equals(party.getRole()))
                .map(ArrangementParty::getCustomerId).collect(Collectors.toList());
        customerValidator.validateCustomerAccountIdEquality(customer, arrCustomerIds, account);
    }

    /**
     * Main idea is to group the buy/sell (or other arrangement type) into a single policy
     *
     * @param model
     * @param arrangement
     * @param pricing
     * @param party
     * @param operation
     */
    private void processRecordForBuyAndSell(
            OrderPlacementDTO model,
            Arrangement arrangement,
            ArrangementPricing pricing,
            ArrangementParty party,
            ArrangementParty brokerParty,
            ArrangementParty purchaserParty,
            ArrangementOperation operation,
            ArrangementRelation relation,
            BrokerDTO brokerDTO) {

        if (arrangement == null || pricing == null || party == null || operation == null) {
            throw new NeoFiatsException("Invalid entity records");
        }

        ArrangementDTO arrDTO = model.getArrangement();
        ArrangementPricingDTO priceDTO = model.getPricing();
        ArrangementPartyDTO partyDTO = model.getParty();
        ArrangementPartyDTO brokerPartyDTO = model.getBrokerParty();
        ArrangementPartyDTO purchaserPartyDTO = model.getPurchaserParty();
        if (model.getOperation() == null) {
            model.setOperation(ArrangementOperationDTO.builder().build());
        }
        ArrangementOperationDTO operationDTO = model.getOperation();
        Timestamp now = DateHelper.nowInTimestamp();

        // create Arrangement record
        BeanUtils.copyProperties(arrDTO, arrangement, "id");
        arrangement.setChannel(Constant.NORMAL_CHANNEL);
        arrangement.insertExtra(now);
        arrangement = arrRepo.save(arrangement);
        BeanUtils.copyProperties(arrangement, arrDTO);

        // create associated ArrangementPricing record
        setPricingData(pricing, priceDTO, arrangement);

        // create associated ArrangementOperation record
        BeanUtils.copyProperties(operationDTO, operation,
                CommonUtils.getNullProperties(operationDTO));
        operation.setArrangement(arrangement);
        operation = arrOpRepo.save(operation);
        BeanUtils.copyProperties(operation, operationDTO);

        // create associated ArrangementParty record
        BeanUtils.copyProperties(partyDTO, party, "id");
        party.setArrangement(arrangement);
        party = arrPartyRepo.save(party);
        partyDTO.setId(party.getId());
        BeanUtils.copyProperties(party, partyDTO);

        if (relation != null && !CollectionUtils.isEmpty(model.getRelations())) {
            ArrangementRelationDTO relationDTO = model.getRelations().stream()
                    .filter(r -> r.getType().equals(ArrangementRelationTypeEnum.SELL_ARRANGEMENT_ID.getType().intValue()))
                    .findAny().orElse(null);
            if (relationDTO != null) {
                Arrangement sellArr = arrRepo.findById(relationDTO.getRelatedArrangement().getId())
                        .orElseThrow(() -> new NeoFiatsException(ArrangementErrorCode.ARRANGEMENT_SELL_ARRANGEMENT_CODE_NOT_FOUND,
                                CommonUtils.format("Sell arrangement {0} is not available",
                                        relationDTO)));
                relation.setArrangement(arrangement);
                relation.setRelatedArrangement(sellArr);
                relation.setType(relationDTO.getType());
                relation.insertExtra(now);
                relation = arrRelationRepo.save(relation);
            }
        }

        if (brokerParty != null && brokerPartyDTO != null) {
            BeanUtils.copyProperties(brokerPartyDTO, brokerParty, "id");
            brokerParty.setArrangement(arrangement);
            brokerParty = arrPartyRepo.save(brokerParty);
            brokerPartyDTO.setId(brokerParty.getId());
            BeanUtils.copyProperties(brokerParty, brokerDTO);
        }

        // created purchaser ArrangementParty record
        if (purchaserParty != null && purchaserPartyDTO != null) {
            BeanUtils.copyProperties(purchaserPartyDTO, purchaserParty, "id");
            purchaserParty.setArrangement(arrangement);
            purchaserParty = arrPartyRepo.save(purchaserParty);
            purchaserPartyDTO.setId(purchaserParty.getId());
            BeanUtils.copyProperties(purchaserParty, purchaserPartyDTO);
        }
    }

    private void setPricingData(ArrangementPricing pricing, ArrangementPricingDTO priceDTO, Arrangement arrangement) {
        if (priceDTO != null && pricing != null & arrangement != null) {
            BeanUtils.copyProperties(priceDTO, pricing, "id"); // rate + reinvestmentRate + investmentTimeByMonth
            pricing.setArrangement(arrangement);
            pricing.setPrice(CommonUtils.doubleToBigDecimalSilently(priceDTO.getPrice()));
            pricing.setFee(CommonUtils.doubleToBigDecimalSilently(priceDTO.getFee()));
            pricing.setDiscountedAmount(CommonUtils.doubleToBigDecimalSilently(priceDTO.getDiscountedAmount()));
            pricing.setUnitPrice(CommonUtils.doubleToBigDecimalSilently(priceDTO.getUnitPrice()));
            pricing.setAgencyFee(CommonUtils.doubleToBigDecimalSilently(priceDTO.getAgencyFee()));
            ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(arrangement.getType());
            pricing.setPrincipal(CommonUtils.doubleToBigDecimalSilently(ArrangementTypeEnum.BUY == type
                    ? priceDTO.getTotalInvestAmount() : priceDTO.getTotalReceivedAmount()));
            pricing.setTotalMoneyRtm(CommonUtils.doubleToBigDecimalSilently(priceDTO.getTotalMoneyRtm()));
            pricing.setTax(CommonUtils.doubleToBigDecimalSilently(priceDTO.getTax()));
            pricing = arrPriRepo.save(pricing);
            priceDTO.setId(pricing.getId());
        }
    }

    private void decideRoleForBuyAndSell(boolean isBroker, CustomerBrokerWrapper cusBroker,
                                         OrderPlacementDTO order) {

        try {
            ArrangementPartyDTO partyDTO = order.getParty();

            // if role is BROKER, set status to REFERENCE and add 1 more BROKER party record
            if (isBroker) {

                ArrangementPartyDTO brokerParty = new ArrangementPartyDTO();
                brokerParty.setRole(ArrangementRoleEnum.BROKER.toString());
                brokerParty.setCustomerId(cusBroker.getBroker().getCustomerId());
                order.setBrokerParty(brokerParty);

                partyDTO.setRole(ArrangementRoleEnum.OWNER.toString());
                partyDTO.setCustomerId(partyDTO.getCustomerId());

            } else { // OWNER placing order himself

                partyDTO.setRole(ArrangementRoleEnum.OWNER.toString());
                partyDTO.setCustomerId(cusBroker.getCustomer().getId());
                partyDTO.setAccount(cusBroker.getCustomer().getAccount());
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ValidationException(ErrorCode.UNAVAILABLE_ITEM,
                    "Cannot find the broker {}");
        }
    }

    private boolean isBrokerReferred(BrokerDTO brokerDTO) {
        return brokerDTO != null && !CommonUtils.isInvalidPK(brokerDTO.getId());
    }

    private ArrangementTypeEnum swapTypeForBuyAndSell(ArrangementTypeEnum typeEnum) {

        if (ArrangementTypeEnum.BUY == typeEnum) {
            return ArrangementTypeEnum.SELL;
        } else if (ArrangementTypeEnum.SELL == typeEnum) {
            return ArrangementTypeEnum.BUY;
        } else {
            throw new NeoFiatsException(
                    CommonUtils.format("Invalid request. Type {0} is neither BUY or SELL",
                            typeEnum));
        }
    }

    private List<Arrangement> findArrangementBySpecification(PagingFilterBase<ArrangementFilter> pf,
                                                             Specification<Arrangement> specification) {
        List<Arrangement> arrangements;
        if (!pf.isPageable()) {
            arrangements = arrRepo.findAll(specification, Sort.by("tradingDate").descending().and(Sort.by("createdDate").descending()));
        } else {
            Page<Arrangement> page = arrRepo.findAll(specification,
                    PageRequest.of(pf.getPageNum(), pf.getPageSize(), Sort.by("tradingDate").descending().and(Sort.by("createdDate").descending())));
            arrangements = page.getContent();
            pf.getPaging().setTotalRecords(page.getTotalElements());
            pf.getPaging().setTotalPages(page.getTotalPages());
        }
        return arrangements;
    }

    private PricingParamDTO buildPricingParam(ArrangementDTO arr, CustomerDTO cus, AssetDTO asset) {
        PricingParamDTO pricingParam = PricingParamDTO.builder().build();
        pricingParam.setProductCode(arr.getProductDerivativeCode());
        pricingParam.setAction(ArrangementTypeEnum.lookForType(arr.getType()).getTypeStr());
        pricingParam.setTradingDate(arr.getTradingDate());
        pricingParam.setCustType(cus.getCustomerType());
        pricingParam.setQuantity(arr.getVolume());

        if (asset != null && arr.getType().equals(ArrangementTypeEnum.SELL.getType())) {
            pricingParam.setBuyPrice(asset.getPrice().doubleValue());
            pricingParam.setBuyVolume(asset.getVolume().doubleValue());
            pricingParam.setBuyRate(asset.getRate());
            pricingParam.setBuyDate(asset.getTradingDate());
        }

        return pricingParam;
    }

    private Long findSellArrangementId(Arrangement currentArr) {

        if (currentArr == null) {
            return null;
        }

        return currentArr.getOriginRelations().stream()
                .filter(r -> ArrangementRelationTypeEnum.SELL_ARRANGEMENT_ID.getType().equals(r.getType()))
                .map(r -> r.getRelatedArrangement().getId())
                .findAny()
                .orElse(null);
    }

    private ArrangementPricingDTO castPricingResultToArrPricingDTO(PricingBondResultDTO pricingResult) {
        ArrangementPricingDTO pricingDTO = new ArrangementPricingDTO();
        if (pricingResult != null) {
            pricingDTO.setPrice(pricingResult.getPricing().getPrice());
            pricingDTO.setFee(pricingResult.getPricing().getTransactionFee());
            pricingDTO.setDiscountedAmount(pricingResult.getPricing().getDiscountAmount());
            pricingDTO.setUnitPrice(pricingResult.getUnitPrice());
            pricingDTO.setAgencyFee(pricingResult.getPricing().getMarketFee());
            pricingDTO.setRate(pricingResult.getPricing().getRate());
            pricingDTO.setReinvestmentRate(pricingResult.getPricing().getYtmReinvested());
            pricingDTO.setTotalReceivedAmount(pricingResult.getPricing().getTotalReceivedAmount());
            pricingDTO.setTotalInvestAmount(pricingResult.getPricing().getTotalInvestAmount());
            pricingDTO.setTotalMoneyRtm(pricingResult.getPricing().getTotalMoneyRtm());
            pricingDTO.setTax(pricingResult.getPricing().getTransactionTax());
            pricingDTO.setInvestmentTimeByMonth(pricingResult.getInvestmentTimeByMonth());
            if (!CollectionUtils.isEmpty(pricingResult.getCoupons())) {
                pricingDTO.setCoupons(pricingResult.getCoupons());
            }
        }
        return pricingDTO;
    }

    private OrderPlacementDTO castEntityToModel(Arrangement a) {

        ArrangementOperation op = a.getOperation();

        ArrangementFilterStatusEnum arrStatus = ArrangementFilterStatusEnum
                .lookByValue(op.getCustomerStatus(), op.getContractStatus(),
                        op.getPaymentStatus(), op.getDeliveryStatus(),
                        op.getCollateralStatus(), op.getReleaseStatus(),
                        a.getStatus());
        log.debug("Using status {} for {}", arrStatus, a.getId());

        ArrangementDTO arrDTO = ArrangementDTO.builder()
                .id(a.getId()).code(a.getCode()).type(a.getType())
                .tradingDate(a.getTradingDate())
                .arrStatus(arrStatus != null ? arrStatus.getFilterStatus() : null)
                .createdDate(a.getCreatedDate())
                .volume(a.getVolume())
                .build();

        double principal = 0D;
        double tax = 0D;
        if (a.getPricing() != null) {

            if (a.getPricing().getPrincipal() != null) {
                principal = a.getPricing().getPrincipal().doubleValue();
            }

            if (a.getPricing().getTax() != null) {
                tax = a.getPricing().getTax().doubleValue();
            }
        }

        ArrangementPricingDTO priDTO = ArrangementPricingDTO.builder()
                .totalInvestAmount(principal).tax(tax)
                .build();

        ProdDerivativeDTO deriDTO = ProdDerivativeDTO.builder()
                .id(a.getId()).code(a.getProductDerivativeCode())
                .build();

        return OrderPlacementDTO.builder()
                .pricing(priDTO).arrangement(arrDTO)
                .derivative(deriDTO).build();
    }

    private OrderPlacementDTO castEntityToModelWithCustomer(Arrangement arrangement,
                                                            CustomerDTO customerDTO,
                                                            Long sellArrangementId) {
        OrderPlacementDTO result = new OrderPlacementDTO();

        ArrangementDTO arrDTO = new ArrangementDTO();
        BeanUtils.copyProperties(arrangement, arrDTO);
        ArrangementPricingDTO pricingDTO = castPricingToDTO(arrangement.getPricing(),
                arrangement.getType());
        ArrangementPartyDTO partyDTO = new ArrangementPartyDTO();
        partyDTO.setCustomerId(customerDTO.getId());
        partyDTO.setAccount(customerDTO.getAccount());
        partyDTO.setCustomerIdCard(customerDTO.getIdCard());
        ProdDerivativeDTO derivativeDTO = ProdDerivativeDTO.builder()
                .id(arrangement.getProductDerivativeId())
                .code(arrangement.getProductDerivativeCode())
                .prodVanilla(ProdVanillaDTO.builder()
                        .id(arrangement.getProductVanillaId())
                        .code(arrangement.getProductVanillaCode())
                        .build())
                .build();

        if (!CommonUtils.isInvalidPK(sellArrangementId)) {
            ArrangementRelationDTO relation = new ArrangementRelationDTO();
            arrDTO.setSellArrangementId(sellArrangementId);
            relation.setArrangement(arrDTO);
            relation.setRelatedArrangement(ArrangementDTO.builder()
                    .id(sellArrangementId)
                    .build());
        }

        result.setArrangement(arrDTO);
        result.setParty(partyDTO);
        result.setPricing(pricingDTO);
        result.setDerivative(derivativeDTO);

        return result;
    }

    private ArrangementPricingDTO castPricingToDTO(ArrangementPricing arrPri, Integer arrType) {

        if (arrPri != null) {
            ArrangementPricingDTO pricingDTO = new ArrangementPricingDTO();
            BeanUtils.copyProperties(arrPri, pricingDTO);
            pricingDTO.setPrice(CommonUtils.bigDecimalToDoubleSilently(arrPri.getPrice()));
            pricingDTO.setFee(CommonUtils.bigDecimalToDoubleSilently(arrPri.getFee()));
            pricingDTO.setDiscountedAmount(CommonUtils.bigDecimalToDoubleSilently(arrPri.getDiscountedAmount()));
            pricingDTO.setUnitPrice(CommonUtils.bigDecimalToDoubleSilently(arrPri.getUnitPrice()));
            pricingDTO.setAgencyFee(CommonUtils.bigDecimalToDoubleSilently(arrPri.getAgencyFee()));
            if (ArrangementTypeEnum.SELL.getType().equals(arrType)) {
                pricingDTO.setTotalReceivedAmount(CommonUtils.bigDecimalToDoubleSilently(arrPri.getPrincipal()));
            } else if (ArrangementTypeEnum.BUY.getType().equals(arrType)) {
                pricingDTO.setTotalInvestAmount(CommonUtils.bigDecimalToDoubleSilently(arrPri.getPrincipal()));
            }
            pricingDTO.setTotalMoneyRtm(CommonUtils.bigDecimalToDoubleSilently(arrPri.getTotalMoneyRtm()));
            pricingDTO.setTax(CommonUtils.bigDecimalToDoubleSilently(arrPri.getTax()));
            return pricingDTO;
        }

        return null;
    }

    private List<ArrangementRelationDTO> castRelationsToDTOs(List<ArrangementRelation> relations) {
        return relations.stream().map(r -> {
            ArrangementRelationDTO dto = ArrangementRelationDTO.builder()
                    .arrangement(ArrangementDTO.builder().build())
                    .relatedArrangement(ArrangementDTO.builder().build())
                    .build();
            BeanUtils.copyProperties(r.getArrangement(), dto.getArrangement());
            BeanUtils.copyProperties(r.getRelatedArrangement(), dto.getRelatedArrangement());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TemplateFileDTO> getContractsBrokerSupport(JWTHelper jwt, Long arrangementId) {
        CustomerDTO customerDTO = customerService.findByAccountName(jwt.getSubFromJWT(), false);
        if (customerDTO == null) {
            throw new NeoException(null, ArrangementErrorCode.BROKER_NOT_FOUND,
                    CommonUtils.format("Cannot found broker with customer acc {0}", jwt.getSubFromJWT()));
        }

        ArrangementInfoDTO arrInfo = backSupportService.retrieveDetailArrangement(arrangementId, null);
        if (arrInfo == null) {
            throw new NeoException(null, ArrangementErrorCode.ARRANGEMENT_RECORD_NOT_FOUND,
                    CommonUtils.format("Cannot found arrangement with id {0}", arrangementId));
        }

        if (arrInfo.getBrokerId() == null || !arrInfo.getBrokerId().equals(customerDTO.getId())) {
            throw new NeoException(null, ArrangementErrorCode.PERMISSION_VIOLATED,
                    CommonUtils.format("Dont have permission to get contracts of arrangement id {0}", arrangementId));
        }

        return contractService.getContractsBrokerSupport(customerDTO.getAccount(), arrangementId);
    }

    @Override
    public Object downloadContractBrokerSupport(JWTHelper jwt, Long arrangementId, String contractName) {
        CustomerDTO customerDTO = customerService.findByAccountName(jwt.getSubFromJWT(), false);
        if (customerDTO == null) {
            throw new NeoException(null, ArrangementErrorCode.BROKER_NOT_FOUND,
                    CommonUtils.format("Cannot found broker with customer acc {0}", jwt.getSubFromJWT()));
        }

        ArrangementInfoDTO arrInfo = backSupportService.retrieveDetailArrangement(arrangementId, null);
        if (arrInfo == null) {
            throw new NeoException(null, ArrangementErrorCode.ARRANGEMENT_RECORD_NOT_FOUND,
                    CommonUtils.format("Cannot found arrangement with id {0}", arrangementId));
        }

        if (arrInfo.getBrokerId() == null || !arrInfo.getBrokerId().equals(customerDTO.getId())) {
            throw new NeoException(null, ArrangementErrorCode.PERMISSION_VIOLATED,
                    CommonUtils.format("Dont have permission to get contracts of arrangement id {0}", arrangementId));
        }

        return contractService.downloadContractBrokerSupport(customerDTO.getAccount(), arrangementId, contractName);
    }
}