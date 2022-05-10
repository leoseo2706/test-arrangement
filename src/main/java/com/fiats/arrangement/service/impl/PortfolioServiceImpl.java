package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.common.ArrangementEventCode;
import com.fiats.arrangement.constant.*;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import com.fiats.arrangement.jpa.entity.ArrangementParty;
import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.payload.PortfolioResponseWrapper;
import com.fiats.arrangement.service.*;
import com.fiats.arrangement.utils.ConverterUtils;
import com.fiats.arrangement.validator.ArrangementValidator;
import com.fiats.exception.NeoFiatsException;
import com.fiats.tmgcoreutils.common.ErrorCode;
import com.fiats.tmgcoreutils.constant.*;
import com.fiats.tmgcoreutils.constant.AttributeEnum;
import com.fiats.tmgcoreutils.event.PortfolioWrapperEvent;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgcoreutils.utils.ReactiveClientUtils;
import com.fiats.tmgjpa.payload.ResponseMessage;
import com.neo.exception.LoggingUtils;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaFailureCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PortfolioServiceImpl implements PortfolioService {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${fiats.internal.api.portfolio.url}")
    private String portfolioUrl;

    @Autowired
    private CustomerService customerService;

    @Value("${retail.source.account}")
    private String retailSourceAccount;

    @Autowired
    private ProductDerivativeService product;

    @Autowired
    AttributeService attributeService;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Value("${custom.kafka.topic.prefix}")
    String KAFKA_TOPIC_PREFIX;

    @Value("${spring.application.name}")
    String applicationName;

    @Value("${fiats.internal.api.crm.url}")
    private String crmUrl;

    @Autowired
    @Qualifier("customWebClient")
    WebClient webClient;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    @Qualifier("customMapper")
    ObjectMapper customMapper;

    @Autowired
    ArrangementRepo arrRepo;

    @Autowired
    ArrangementValidator arrValidator;

    @Autowired
    ArrangementService arrService;

    @Autowired
    ArrangementTickingService arrTickingService;

    @Autowired
    ProductDerivativeService productDerivativeService;

    @Autowired
    MatchingService matchingService;

    @Autowired
    ArrangementNotificationService arrNotificationService;

    @Autowired
    private VanillaService vanillaService;

    @Autowired
    private ArrangementLogService arrangementLogService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Override
    public List<PortViewArrangementDTO> getPortfolioByAccountAndAsset(String customerCode, String account, String assetCode) {
        try {
            StringBuilder url = new StringBuilder().append(portfolioUrl).append("/arrangement?cc={customerCode}&ac={account}&arr={assetCode}");

            Map<String, Object> params = new HashMap<>();
            params.put("customerCode", customerCode);
            params.put("account", account);
            params.put("assetCode", assetCode);

            ResponseEntity<String> response = restTemplate.getForEntity(
                    url.toString(), String.class, params);

            log.info("Customer({}, {}) get asset ({}) result: {}", customerCode, account, assetCode, response.getBody());

            JSONObject mapData = new JSONObject(response.getBody());


            if (response.getStatusCode().is2xxSuccessful()) {
                JSONArray dataJSON = mapData.getJSONArray("data");

                if (dataJSON != null) {
                    List<PortViewArrangementDTO> data = mapper.readValue(dataJSON.toString(), new TypeReference<List<PortViewArrangementDTO>>() {
                    });

                    return data;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    public PortTransactionDTO makePortfolioTransaction(PortTransactionDTO dto) {
        try {
            StringBuilder url = new StringBuilder().append(portfolioUrl).append("/trans");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PortTransactionDTO> request = new HttpEntity<>(dto, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url.toString(), request, String.class);

            JSONObject mapData = new JSONObject(resp.getBody());

            if (resp.getStatusCode().is2xxSuccessful()) {
                JSONObject data = mapData.getJSONObject("data");
                PortTransactionDTO result = mapper.readValue(data.toString(), PortTransactionDTO.class);

                return result;
            } else {
                throw new NeoException(null, ArrangementErrorCode.PORTFOLIO_TRANSACTION_ERROR, mapData.get("message"));
            }
        } catch (Exception e) {
            throw new NeoException(e, ArrangementErrorCode.APPROVE_COLLATERAL_ERROR, "Approve arrangement collateral has error!!!");
        }
    }

    @Override
    public List<PortViewDerivativeDTO> getPortfolioByDerivativeCode(String derivativeCode) {
        log.info("Get data from retail source: {}", derivativeCode);
        StringBuilder url = new StringBuilder(portfolioUrl).append("/retail/derivative");
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(url.toString())
                .queryParam("dc", derivativeCode)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 100);

        try {
            log.info("URL: {}", urlBuilder.toUriString());

            ResponseEntity<String> responseEntity = restTemplate.getForEntity(urlBuilder.toUriString(), String.class);
            log.info("responseEntity: {}", responseEntity.getBody());

            JSONObject mapData = new JSONObject(responseEntity.getBody());
            JSONArray dataJSON = mapData.getJSONArray("data");

            if (dataJSON != null) {
                List<PortViewDerivativeDTO> data = mapper.readValue(dataJSON.toString(), new TypeReference<List<PortViewDerivativeDTO>>() {
                });

                return data;
            }

        } catch (Exception e) {
            throw new NeoException(e, ErrorCode.SERVER_ERROR, new StringBuilder("Error get retail derivative: ").append(derivativeCode));
        }

        return null;
    }

    @Override
    public OrderPlacementDTO getLatestAfc(String derivativeCode) {
        log.info("Get latest AFC, derivativeCode: {}", derivativeCode);
        List<PortViewDerivativeDTO> data = getPortfolioByDerivativeCode(derivativeCode);

        log.info("Data from Portfolio: {}", data);

        return afcToBooking(data, derivativeCode);
    }

    @Override
    public OrderPlacementDTO getLatestAft(String vanillaCode) {
        log.info("Get latest AFT, vanillaCode {}", vanillaCode);
        List<PortViewDerivativeDTO> data = getPortfolioByVanillaCode(vanillaCode);

        log.info("Data from portfolio: {}", data);
        return aftToBooking(data, vanillaCode);
    }

    public List<PortViewDerivativeDTO> getPortfolioByVanillaCode(String vanillaCode) {
        log.info("Get data from retail source: {}", vanillaCode);
        StringBuilder url = new StringBuilder(portfolioUrl).append("/retail/vanilla");
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(url.toString())
                .queryParam("vc", vanillaCode);

        try {
            log.info("URL: {}", urlBuilder.toUriString());

            ResponseEntity<String> responseEntity = restTemplate.getForEntity(urlBuilder.toUriString(), String.class);
            log.info("responseEntity: {}", responseEntity.getBody());

            JSONObject mapData = new JSONObject(responseEntity.getBody());
            JSONArray dataJSON = mapData.getJSONArray("data");

            if (dataJSON != null) {
                List<PortViewDerivativeDTO> data = mapper.readValue(dataJSON.toString(), new TypeReference<List<PortViewDerivativeDTO>>() {
                });

                return data;
            }

        } catch (Exception e) {
            throw new NeoException(e, ErrorCode.SERVER_ERROR, new StringBuilder("Error get retail derivative: ").append(vanillaCode));
        }

        return null;
    }

    @Override
    public void sendEventToPortfolioQueue(OrderPlacementDTO order, PortfolioRetailAction action) {
        ArrangementDTO arrDTO = order.getArrangement();

        // send to Portfolio queue
        PortfolioWrapperEvent event = new PortfolioWrapperEvent();
        event.setModel(order);
        event.setEventCode(ArrangementEventCode.PORTFOLIO_CONFIRMING_ACTION.getCode());
        event.setActionTime(DateHelper.nowInTimestamp());
        event.setArrangementCode(order.getArrangement().getCode());
        event.setArrangementId(order.getArrangement().getId());
        event.setAction(action);
        String topic = ArrangementEventCode.PORTFOLIO_CONFIRMING_ACTION
                .getTopicName(KAFKA_TOPIC_PREFIX, applicationName);
        log.info("Sending event {} to {}", LoggingUtils.objToStringIgnoreEx(event), topic);

        // send and wait for call back
        ListenableFuture<SendResult<Integer, String>> future = kafkaTemplate.send(topic, event);
        future.addCallback(result -> {
            log.info("Done sending model of arr id {}", arrDTO.getId());
        }, (KafkaFailureCallback<Integer, String>) ex -> {
            // ProducerRecord<Integer, String> failed = ex.getFailedProducerRecord();
            log.error("Failed to send model of arr id {}", arrDTO.getId());
            log.error(ex.getMessage(), ex.getCause());
        });
    }

    @Override
    public void handlePortfolioEvent(PortfolioWrapperEvent event) {

        // prepare model
        OrderPlacementDTO orderModel = event.getModel();
        ArrangementDTO arrDTO = orderModel.getArrangement();
        ArrangementPartyDTO partyDTO = orderModel.getParty();
        ArrangementPricingDTO pricingDTO = orderModel.getPricing();

        ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(arrDTO.getType());

        String assetCode = StringUtils.hasText(arrDTO.getSellArrangementCode())
                ? arrDTO.getSellArrangementCode() : arrDTO.getCode();

        PortTransactionDTO model = PortTransactionDTO.builder()
                .id(arrDTO.getId())
                .assetCode(assetCode) // note that in HOLD/UNHOLD this would be original sell id
                .customerId(partyDTO.getCustomerId() != null ? String.valueOf(partyDTO.getCustomerId()) : null)
                .customerCode(partyDTO.getCustomer().getAccount())
                .accountCode(partyDTO.getCustomer().getAccount())
                .prodVanillaCode(arrDTO.getProductVanillaCode())
                .prodDerivativeCode(arrDTO.getProductDerivativeCode())
                .arrangementId(String.valueOf(arrDTO.getId()))
                .action(event.getAction().content())
                .volume(new BigDecimal(String.valueOf(arrDTO.getVolume())))
                .appliedDate(DateHelper.nowInTimestamp())
                .channel(Constant.NORMAL_CHANNEL)
                .arrangementType(type != null ? type.getTypeStr() : null)
                .price(pricingDTO != null ? CommonUtils.doubleToBigDecimalSilently(pricingDTO.getPrice()) : null)
                .rate(pricingDTO != null ? CommonUtils.doubleToBigDecimalSilently(pricingDTO.getRate()) : null)
                .unitPrice(pricingDTO != null ? CommonUtils.doubleToBigDecimalSilently(pricingDTO.getUnitPrice()) : null)
                .principal(pricingDTO != null ? CommonUtils.doubleToBigDecimalSilently(pricingDTO.getTotalMoneyRtm()) : null)
                .userAccount(orderModel.getPurchaserAccount())
                .build();

        // send the first one for HDBS
        ResponseMessage res = ReactiveClientUtils.postForMonoWithModel(webClient, portfolioUrl,
                "/trans", executor, ResponseMessage.class, log, model).block();
        log.info("handlePaymentConfirmationEvent - Done sending signal to portfolio service for arr id {} " +
                        "with account {} and response {}", arrDTO.getId(), partyDTO.getCustomer().getAccount(),
                LoggingUtils.objToStringIgnoreEx(res));
    }

    @Override
    public void handlePortfolioResponseEventForPay(PortfolioResponseMessageDTO event) {

        log.info("handlePortfolioResponseEventForPay - Handling portfolio event {}", LoggingUtils.objToStringIgnoreEx(event));
        validatePortfolioEvent(event);

        Arrangement arr = findAndValidateArrangement(event);
        PortfolioResponseWrapper resWrapper = getPortfolioResponse(event, arr);
        PortfolioRetailAction action = resWrapper.getAction();
        Long customerId = resWrapper.getCustomerId();
        boolean isOrganizationRow = resWrapper.isOrganizationRow();

        switch (action) {

            case PAY: // 2.1.2 Step xác nhận thanh toán

                log.info("Received event {} for arr id {}. Marking payment status",
                        action, arr.getId());

                Arrangement finalArr = arr;
                // tick for this order + all matching orders
                List<Arrangement> updatedArrs = arrTickingService.tickingPaymentStatusByOperations(findEquivalentAndMatchingOperations(arr));
                arr = updatedArrs.stream().filter(tmp -> tmp.getId().equals(finalArr.getId()))
                        .findAny().orElseThrow(() -> new NeoFiatsException(CommonUtils.format("Empty arrangement id " +
                                "after updating delivery status {0}", finalArr.getId())));

                log.info("Done ticking Payment status for action {} arr id {}", action, arr.getId());

                CustomerDTO customerDTO = customerService.retrieveCustomerInfo(customerId);
                log.info("Got customer info {}", LoggingUtils.objToStringIgnoreEx(customerDTO));

                OrderPlacementDTO model = ConverterUtils.castArrangementToOrder(arr, customerDTO);
                model.setPurchaserAccount(loadUserAccountFromRedis(arr.getId(), action));

                // now send AFT + noti only if !isOrganizationRow
                if (!isOrganizationRow) {
                    log.info("Not organization row {}. Building model before sending AFT signal & notification ...", arr.getId());

                    ProdDerivativeDTO derivativeDTO = productDerivativeService.findFullInfoByID(new ArrayList<Long>() {{
                        add(finalArr.getProductDerivativeId());
                    }}).blockFirst();
                    log.info("Got derivative info {}", LoggingUtils.objToStringIgnoreEx(derivativeDTO));

                    matchingService.sendEventToAFTQueue(model);

                    //Create arrangement log
                    arrangementLogService.createArrangementLog(model, ArrangementAction.PAYMENT);
                    sendNotification(model, NotificationCodeEnum.BUY_PAYMENT_SUCCESS);
                } else {
                    log.info("Organization row {}. Building model before sending RECEIVE portfolio signal ...", arr.getId());
                    sendEventToPortfolioQueue(model, PortfolioRetailAction.RECEIVE);
                }
                break;

            default:
                log.info("handlePortfolioResponseEventForPay - Unrecognized event {}", action);

        }
        log.info("handlePortfolioResponseEventForPay - Done handling event {} for arr id {}", action, arr.getId());
    }

    @Override
    public void handlePortfolioResponseEventForHold(PortfolioResponseMessageDTO event) {

        log.info("handlePortfolioResponseEventForHold - Handling portfolio event {}", LoggingUtils.objToStringIgnoreEx(event));
        validatePortfolioEvent(event);

        // prepare
        Optional<Arrangement> arrOptional = arrRepo.findByIdFetchOperationPartyPricing(Long.parseLong(event.getArrangementId().trim()));
        Arrangement arr = arrValidator.validateExistence(arrOptional, Constant.ACTIVE);
        PortfolioResponseWrapper resWrapper = getPortfolioResponse(event, arr);
        PortfolioRetailAction action = resWrapper.getAction();

        switch (action) {

            case HOLD: // 2.2.1 Step ký hợp đồng
                log.info("Received event {} for arr id {}. Creating matching records", action, arr.getId());
                OrderPlacementDTO model = arrService.createMatchingRecords(arr);
                //Create arrangement log
                arrangementLogService.createArrangementLog(model, ArrangementAction.HOLD);
                break;

            default:
                log.info("handlePortfolioResponseEventForHold - Unrecognized event {}", action);

        }
        log.info("handlePortfolioResponseEventForHold - Done handling event {} for arr id {}", action, arr.getId());
    }

    @Override
    public void handlePortfolioResponseEventForPaid(PortfolioResponseMessageDTO event) {

        log.info("handlePortfolioResponseEventForPaid - Handling portfolio event {}", LoggingUtils.objToStringIgnoreEx(event));
        validatePortfolioEvent(event);

        // prepare
        Optional<Arrangement> arrOptional = arrRepo.findByIdFetchOperationPartyPricing(Long.parseLong(event.getArrangementId().trim()));
        Arrangement arr = arrValidator.validateExistence(arrOptional, Constant.ACTIVE);
        PortfolioResponseWrapper resWrapper = getPortfolioResponse(event, arr);
        PortfolioRetailAction action = resWrapper.getAction();
        Long customerId = resWrapper.getCustomerId();
        boolean isOrganizationRow = resWrapper.isOrganizationRow();

        switch (action) {

            case PAID: // 2.2.2 Step xác nhận chuyển tiền (cả khách hàng và tổ chức)
                log.info("Received event {} for arr id {}. Marking payment status ..", action, arr.getId());

                // tick payment first
                List<Arrangement> updatedArrs = arrTickingService.tickingPaymentStatusByOperations(findEquivalentAndMatchingOperations(arr));
                Arrangement finalArr = arr;
                arr = updatedArrs.stream().filter(tmp -> tmp.getId().equals(finalArr.getId()))
                        .findAny().orElseThrow(() -> new NeoFiatsException(CommonUtils.format("Empty arrangement id " +
                                "after updating delivery status {0}", finalArr.getId())));
                log.info("Done ticking Payment status for action {} arr id {}", action, arr.getId());

                if (!isOrganizationRow) {

                    log.info("Not organization row. Sending portfolio signal {} and notification for arr id {}",
                            PortfolioRetailAction.DELIVERY, arr.getId());

                    // then find more info
                    CustomerDTO customerDTO = customerService.retrieveCustomerInfo(customerId);
                    log.info("Got customer info {}", LoggingUtils.objToStringIgnoreEx(customerDTO));

                    OrderPlacementDTO model = ConverterUtils.castArrangementToOrder(arr, customerDTO);
                    model.setPurchaserAccount(loadUserAccountFromRedis(arr.getId(), action));
                    log.info("Done casting to model {}", LoggingUtils.objToStringIgnoreEx(model));

                    //Create arrangement log
                    arrangementLogService.createArrangementLog(model, ArrangementAction.PAYMENT);

                    sendNotification(model, NotificationCodeEnum.SELL_PAYMENT_SUCCESS);

                    Map<Long, Arrangement> originalSellMap = arrService.findOriginalSellOrders(new ArrayList<Arrangement>() {{
                        add(finalArr);
                    }});
                    Arrangement originalSell;
                    if (CollectionUtils.isEmpty(originalSellMap) || (originalSell = originalSellMap.get(arr.getId())) == null) {
                        throw new NeoFiatsException(ArrangementErrorCode.ARRANGEMENT_SELL_ARRANGEMENT_CODE_NOT_FOUND,
                                CommonUtils.format("Cannot find sell arrangement code for arr id {0}", arr.getId()));
                    }
                    model.getArrangement().setSellArrangementCode(originalSell.getCode());

                    // send to portfolio
                    sendEventToPortfolioQueue(model, PortfolioRetailAction.DELIVERY);
                }

                break;

            default:
                log.info("handlePortfolioResponseEventForPaid - Unrecognized event {}", action);

        }
        log.info("handlePortfolioResponseEventForPaid - Done handling event {} for arr id {}", action, arr.getId());

    }

    @Override
    public void handlePortfolioResponseEventForDelivery(PortfolioResponseMessageDTO event) {

        log.info("handlePortfolioResponseEventForDelivery - Handling portfolio event {}", LoggingUtils.objToStringIgnoreEx(event));
        validatePortfolioEvent(event);

        // prepare
        Optional<Arrangement> arrOptional = arrRepo.findByIdFetchOperationPartyPricing(Long.parseLong(event.getArrangementId().trim()));
        Arrangement arr = arrValidator.validateExistence(arrOptional, Constant.ACTIVE);
        PortfolioResponseWrapper resWrapper = getPortfolioResponse(event, arr);
        PortfolioRetailAction action = resWrapper.getAction();
        Long customerId = resWrapper.getCustomerId();
        boolean isOrganizationRow = resWrapper.isOrganizationRow();

        switch (action) {

            case DELIVERY: // 2.2.3 Step chuyển nhượng (account của khách hàng)
                Arrangement finalArr = arr;
                log.info("Received event {} for arr id {}. Marking delivery status and call noti (customer)", action, arr.getId());
                Collection<ArrangementOperation> all = arrService.findAllRelatedOperations(new ArrayList<Arrangement>(){{
                    add(finalArr);
                }});
                if (CollectionUtils.isEmpty(all)) {
                    throw new NeoFiatsException(ArrangementErrorCode.ARRANGEMENT_RECORD_NOT_FOUND,
                            CommonUtils.format("Empty ID while finding original id {0}", arr.getId()));
                }
                // perform ticking
                List<Arrangement> updatedArrs = arrTickingService.tickingDeliveryStatusByOperations(all);
                arr = updatedArrs.stream().filter(tmp -> tmp.getId().equals(finalArr.getId()))
                        .findAny().orElseThrow(() -> new NeoFiatsException(CommonUtils.format("Empty arrangement id " +
                                "after updating delivery status {0}", finalArr.getId())));

                log.info("Done ticking Delivery status for action {} arr id {}", action, arr.getId());
                CustomerDTO customerDTO = customerService.retrieveCustomerInfo(customerId);
                log.info("Got customer info {}", LoggingUtils.objToStringIgnoreEx(customerDTO));
                OrderPlacementDTO model = ConverterUtils.castArrangementToOrder(arr, customerDTO);

                //Create arrangement log
                arrangementLogService.createArrangementLog(model, ArrangementAction.DELIVERY);

                sendNotification(model, NotificationCodeEnum.SELL_TRANSFER_SUCCESS);
                break;

            default:
                log.info("handlePortfolioResponseEventForDelivery - Unrecognized event {}", action);

        }
        log.info("Done handling event {} for arr id {}", action, arr.getId());

    }

    private void validatePortfolioEvent(PortfolioResponseMessageDTO event) {
        if (!StringUtils.hasText(event.getStatus()) || !StringUtils.hasText(event.getAction())
                || !StringUtils.hasText(event.getArrangementId())
                || PortfolioResponseStatus.FAILED.toString().equals(event.getStatus())) {
            log.error("Status of event is {}", event.getStatus());
            throw new NeoFiatsException(ArrangementErrorCode.PORTFOLIO_TRANSACTION_ERROR,
                    CommonUtils.format("Failed to handle portfolio transaction {0}",
                            event.getArrangementId()));
        }
    }

    private String loadUserAccountFromRedis(Long arrId, PortfolioRetailAction action) {
        try {

            String key = CommonUtils.format(ArrConstant.USER_KEY_FORMAT, String.valueOf(arrId), action);
            String user =  redisTemplate.opsForValue().get(key);
            redisTemplate.delete(key);
            log.info("Done loading user {} from key {}", user, key);
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    private Arrangement findAndValidateArrangement(PortfolioResponseMessageDTO event) {
        Optional<Arrangement> arrOptional = arrRepo.findByIdFetchOperationPartyPricing(Long.parseLong(event.getArrangementId().trim()));
        Arrangement arr = arrValidator.validateExistence(arrOptional, Constant.ACTIVE);
        return arr;
    }

    private List<ArrangementOperation> findEquivalentAndMatchingOperations(Arrangement arr) {
        List<ArrangementOperation> equivalentOperations = arrService.findEquivalentMatchingOperations(
                new ArrayList<Arrangement>() {{
                    add(arr);
                }});
        equivalentOperations.add(arr.getOperation());
        log.info("For {}, Got operations IDs {}", LoggingUtils.objToStringIgnoreEx(arr.getId()),
                LoggingUtils.objToStringIgnoreEx(
                        equivalentOperations.stream()
                                .map(ArrangementOperation::getId)
                                .collect(Collectors.toList())
                ));
        return equivalentOperations;
    }

    private void sendNotification(OrderPlacementDTO model, NotificationCodeEnum template) {
        try {
            // now send noti
            arrNotificationService.findCustomerBrokerInfoAndSendNoti(model,
                    template.getSmsTemplate(),
                    template.getEmailTemplate()).subscribe();
        } catch (Exception e) {
            log.error("sendNotification - Failed to send noti while mapping accounting entry" +
                            " for arr id {} with template sms {}, email {}",
                    model.getArrangement().getId(),
                    template.getSmsTemplate(), template.getEmailTemplate());
            log.error("sendNotification - Stack trace : ", e);
        }
    }

    private MultiValueMap<String, Object> getStringObjectMap(PortTransactionDTO dto) {
        MultiValueMap<String, Object> params = mapper.convertValue(dto, MultiValueMap.class);
        return params;
    }

    private OrderPlacementDTO afcToBooking(List<PortViewDerivativeDTO> data, String derivativeCode) {
        if (data != null || data.size() > 0) {
            CustomerDTO customerDTO = customerService.findByAccount(retailSourceAccount, Constant.INACTIVE).block();

            ProdDerivativeDTO prodDerivativeDTO = product.findByCode(derivativeCode);

            // now find Listed or OTC or Frozen for further usages
            PropsResp bondStatus = attributeService.findSystemAttribute(AttributeEnum.BOND_STATUS.getVariable(),
                    DateHelper.nowInTimestamp(), prodDerivativeDTO.getCode(), prodDerivativeDTO.getProdVanilla().getCode(),
                    prodDerivativeDTO.getProdAgreement().getCode(), ArrangementTypeEnum.SELL, Constant.NORMAL_CHANNEL, customerDTO.getCustomerType());

            BondType listedStatus = BondType.lookForBondCode(bondStatus.getValue());

            prodDerivativeDTO.setListedType(listedStatus);

            OrderPlacementDTO booking = OrderPlacementDTO.builder()
                    .matching(MatchingDto.builder()
                            .subMatchMechanism(MechanismType.AFC)
                            .priority(0)
                            .tradingStartDate(new Timestamp(System.currentTimeMillis()))
                            .tradingEndDate(new Timestamp(System.currentTimeMillis()))
                            .remainingVolume(data.get(0).getAvailable().doubleValue())
                            .status(MatchingStatus.SUBMITTING.getCode())
                            .build())
                    .arrangement(ArrangementDTO.builder()
                            .id(-1l)
                            .type(ArrangementTypeEnum.SELL.getType())
                            .code("afc")
                            .volume(data.get(0).getAvailable().intValue())
                            .createdDate(new Timestamp(System.currentTimeMillis()))
                            .build())
                    .party(ArrangementPartyDTO.builder()
                            .account(customerDTO.getAccount())
                            .customerId(customerDTO.getId())
                            .customerName(customerDTO.getName())
                            .build())
                    .derivative(prodDerivativeDTO)
                    .pricing(ArrangementPricingDTO.builder()
                            .price(data.get(0).getAvailable().doubleValue() * prodDerivativeDTO.getProdVanilla().getPar())
                            .rate(0d)
                            .build())
                    .build();
            return booking;
        } else {
            return null;
        }
    }

    private OrderPlacementDTO aftToBooking(List<PortViewDerivativeDTO> data, String vanillaCode) {
        if (data != null || data.size() > 0) {
            CustomerDTO customerDTO = customerService.findByAccount(retailSourceAccount, Constant.INACTIVE).block();
            ProdVanillaDTO prodVanillaDTO = vanillaService.findVanillaByCode(vanillaCode); //Call api
            // now find Listed or OTC or Frozen for further usages
            PropsResp bondStatus = attributeService.findSystemAttribute(AttributeEnum.BOND_STATUS.getVariable(),
                    DateHelper.nowInTimestamp(), null, vanillaCode,
                    null, ArrangementTypeEnum.SELL, Constant.NORMAL_CHANNEL, customerDTO.getCustomerType());

            BondType listedStatus = BondType.lookForBondCode(bondStatus.getValue());

            ProdDerivativeDTO prodDerivativeDTO = ProdDerivativeDTO.builder()
                    .listedType(listedStatus)
                    .prodVanilla(prodVanillaDTO)
                    .build();

            OrderPlacementDTO booking = OrderPlacementDTO.builder()
                    .matching(MatchingDto.builder()
                            .subMatchMechanism(MechanismType.AFT)
                            .priority(0)
                            .remainingVolume(data.get(0).getOutstanding().doubleValue())
                            .status(MatchingStatus.SUBMITTING.getCode())
                            .tradingStartDate(new Timestamp(System.currentTimeMillis()))
                            .tradingEndDate(new Timestamp(System.currentTimeMillis()))
                            .build())
                    .arrangement(ArrangementDTO.builder()
                            .id(-1l)
                            .type(ArrangementTypeEnum.SELL.getType())
                            .code("aft")
                            .volume(data.get(0).getReal().intValue())
                            .createdDate(new Timestamp(System.currentTimeMillis()))
                            .build())
                    .party(ArrangementPartyDTO.builder()
                            .account(customerDTO.getAccount())
                            .customerId(customerDTO.getId())
                            .customerName(customerDTO.getName())
                            .build())
                    .derivative(prodDerivativeDTO)
                    .pricing(ArrangementPricingDTO.builder()
                            .price(data.get(0).getAvailable().doubleValue() * prodDerivativeDTO.getProdVanilla().getPar())
                            .rate(0d)
                            .build())
                    .build();
            return booking;
        } else {
            return null;
        }
    }

    private PortfolioResponseWrapper getPortfolioResponse(PortfolioResponseMessageDTO event,
                                                          Arrangement arr) {

        // find customer id + mark isOrganizationRow
        PortfolioRetailAction action = PortfolioRetailAction.get(event.getAction());
        PortfolioResponseWrapper wrapper = new PortfolioResponseWrapper();
        boolean isOrganizationRow = Constant.INACTIVE;
        Long customerId = null;
        for (ArrangementParty p : arr.getParties()) {

            if (ArrangementRoleEnum.ORGANIZATION.toString().equals(p.getRole())) {
                isOrganizationRow = Constant.ACTIVE;
            }

            if (isOrganizationRow || ArrangementRoleEnum.OWNER.toString().equals(p.getRole())) {
                customerId = p.getCustomerId();
                break;
            }
        }
        if (CommonUtils.isInvalidPK(customerId)) {
            throw new NeoFiatsException(ArrangementErrorCode.CUSTOMER_NOT_FOUND,
                    CommonUtils.format("Customer for arr id {0} is N/A", arr.getId()));
        }

        wrapper.setOrganizationRow(isOrganizationRow);
        wrapper.setCustomerId(customerId);
        wrapper.setAction(action);

        return wrapper;
    }
}