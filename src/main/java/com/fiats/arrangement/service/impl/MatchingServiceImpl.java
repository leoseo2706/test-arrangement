package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.common.ArrangementEventCode;
import com.fiats.arrangement.constant.ArrangementAction;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.constant.AttributeEnum;
import com.fiats.arrangement.constant.NotificationCodeEnum;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.service.*;
import com.fiats.arrangement.utils.ConverterUtils;
import com.fiats.arrangement.validator.ArrangementValidator;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.common.EventCode;
import com.fiats.tmgcoreutils.constant.*;
import com.fiats.tmgcoreutils.event.MatchingBaseEvent;
import com.fiats.tmgcoreutils.event.PaymentBuySuccessEvent;
import com.fiats.tmgcoreutils.event.SignBuyContractEvent;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.core.KafkaFailureCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class MatchingServiceImpl implements MatchingService {

    @Autowired
    ArrangementValidator arrValidator;

    @Autowired
    ArrangementRepo arrRepo;

    @Autowired
    AttributeService attributeService;

    @Autowired
    CustomerService customerService;

    @Autowired
    ArrangementNotificationService arrNotiService;

    @Autowired
    ArrangementService arrService;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Value("${fiats.internal.api.matching.url}")
    String matchingUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Value("${custom.kafka.topic.prefix}")
    String KAFKA_TOPIC_PREFIX;

    @Value("${spring.application.name}")
    String applicationName;

    @Autowired
    AccountingEntryService entryService;

    @Autowired
    ArrangementTickingService arrangementTickingService;

    @Autowired
    private ArrangementLogService arrangementLogService;

    @Override
    public void handleConfirmedRecord(MatchingBaseEvent event) {

        OrderPlacementDTO order = event.getOrderPlacementDto();
        String mechanism = event.getMechanism();

        if (MechanismType.AFC.equals(mechanism)) {
            log.info("handleConfirmedRecord - Receiving signal AFC {} from matching service", event);
            handleAFCMechanismResultAndSendSignal(order);
        } else if (MechanismType.AFT.equals(mechanism)) {
            log.info("handleConfirmedRecord - Receiving signal AFT {} from matching service", event);
            handleAFTMechanismResultAndSendSignal(order);
        } else {
            // this record shall go to DLQ
            throw new NeoFiatsException(ArrangementErrorCode.UNSUPPORTED_MATCHING_MECHANISM,
                    CommonUtils.format("Unsupported mechanism {0}", mechanism));
        }
    }

    @Override
    public void confirmingAFC(SignBuyContractEvent event) {
        log.info("After sign buy contract success, submit data to confirm AFC: {}", LoggingUtils.objToStringIgnoreEx(event));
        OrderPlacementDTO afcOrder = createMatchingOrder(event.getOrder(), MechanismType.AFC);
        OrderResponseDto orderResponseDto = submit(afcOrder);
        log.info("Submit data to confirm AFC Status: {}", orderResponseDto);
    }

    @Override
    public void sendEventToAFCQueue(OrderPlacementDTO order) {
        SignBuyContractEvent event = new SignBuyContractEvent();
        event.setOrder(order);
        event.setEventCode(EventCode.MATCHING_CONFIRMING_AFC.getCode());
        event.setActionTime(DateHelper.nowInTimestamp());
        event.setArrangementCode(order.getArrangement().getCode());
        event.setArrangementId(order.getArrangement().getId());
        event.setCustomerCode(order.getParty().getAccount());
        ListenableFuture<SendResult<Integer, String>> future = kafkaTemplate.send(
                ArrangementEventCode.MATCHING_CONFIRMING_AFC
                        .getTopicName(KAFKA_TOPIC_PREFIX, applicationName),
                event
        );

        future.addCallback(result -> {
            log.info("Done sending model of arr id {}", order.getArrangement().getId());
        }, (KafkaFailureCallback<Integer, String>) ex -> {
            // ProducerRecord<Integer, String> failed = ex.getFailedProducerRecord();
            log.error("Failed to send model of arr id {}", order.getArrangement().getId());
            log.error(ex.getMessage(), ex.getCause());
        });
    }

    @Override
    public void confirmingAFT(PaymentBuySuccessEvent event) {
        log.info("Payment success, submit data to confirm AFT: {}", LoggingUtils.objToStringIgnoreEx(event));
        OrderPlacementDTO aftOrder = createMatchingOrder(event.getOrder(), MechanismType.AFT);
        OrderResponseDto orderResponseDto = submit(aftOrder);
        log.info("Submit data to confirm AFT Status: {}", orderResponseDto);
    }

    @Override
    public void sendEventToAFTQueue(OrderPlacementDTO order) {

        ArrangementDTO arrDTO = order.getArrangement();

        // send to AFT queue
        PaymentBuySuccessEvent event = new PaymentBuySuccessEvent();
        event.setOrder(order);
        event.setEventCode(EventCode.MATCHING_CONFIRMING_AFT.getCode());
        event.setActionTime(DateHelper.nowInTimestamp());
        event.setArrangementCode(order.getArrangement().getCode());
        event.setArrangementId(order.getArrangement().getId());
        event.setCustomerCode(order.getParty().getAccount());
        ListenableFuture<SendResult<Integer, String>> future = kafkaTemplate.send(
                ArrangementEventCode.MATCHING_CONFIRMING_AFT
                        .getTopicName(KAFKA_TOPIC_PREFIX, applicationName),
                event
        );

        future.addCallback(result -> {
            log.info("Done sending model of arr id {}", arrDTO.getId());
        }, (KafkaFailureCallback<Integer, String>) ex -> {
            // ProducerRecord<Integer, String> failed = ex.getFailedProducerRecord();
            log.error("Failed to send model of arr id {}", arrDTO.getId());
            log.error(ex.getMessage(), ex.getCause());
        });
    }

    private OrderPlacementDTO createMatchingOrder(OrderPlacementDTO orderPlacementDTO, String mechanism) {
        //Lay thong tin attribute
        Map<String, Object> props = new HashMap<>();
        String workingDays = getListWorkingDays(orderPlacementDTO); //Tim danh sach ngay working tu ngay dat lenh den ngay tradingDate
        String deadlinePayment = getDeadlinePayment(orderPlacementDTO);

        props.put(Constant.ATTR_NAME.DEADLINE_PAYMENT, deadlinePayment);
        props.put(Constant.ATTR_NAME.WORKING_DAYS, workingDays);

        orderPlacementDTO.setMatching((MatchingDto.builder()
                        .priority(Integer.MAX_VALUE)
                        .subMatchMechanism(mechanism)
                        .props(props)
                        .tradingStartDate(orderPlacementDTO.getArrangement().getTradingDate())
                        .status(MatchingStatus.SUBMITTING.getCode())
                        .remainingVolume(orderPlacementDTO.getArrangement().getVolume().doubleValue())
                        .build()));
        return orderPlacementDTO;
    }


    private String getDeadlinePayment(OrderPlacementDTO orderPlacementDTO) {
        log.info("Getting deadline payment by model {}", LoggingUtils.objToStringIgnoreEx(orderPlacementDTO));
        Map<String, Object> filters = new HashMap<>();
        filters.put(PropsReqFilterEnum.CHANNEL.getFilterAttr(), orderPlacementDTO.getArrangement().getChannel());

        CustomerDTO customerDTO = orderPlacementDTO.getParty().getCustomer();
        filters.put(PropsReqFilterEnum.CUSTOMER_TYPE.getFilterAttr(), customerDTO.getCustomerType());
        filters.put(PropsReqFilterEnum.PROD_AGREEMENT.getFilterAttr(), orderPlacementDTO.getDerivative().getProdAgreement().getCode());
        filters.put(PropsReqFilterEnum.PROD_VANILLA.getFilterAttr(), orderPlacementDTO.getDerivative().getProdVanilla().getCode());
        filters.put(PropsReqFilterEnum.PROD_DERIVATIVE.getFilterAttr(), orderPlacementDTO.getDerivative().getCode());
        filters.put(PropsReqFilterEnum.TRANSACTION_TYPE.getFilterAttr(), orderPlacementDTO.getArrangement().getType());
        filters.put(PropsReqFilterEnum.LISTED_STATUS.getFilterAttr(), BondType.lookForValue(orderPlacementDTO.getArrangement().getListedType()));

        PropsResp propsResp = attributeService.findAttribute(AttributeEnum.DEADLINE_PAYMENT.getVariable(),
                orderPlacementDTO.getArrangement().getTradingDate(), filters);

        return propsResp != null ? propsResp.getValue() : "";
    }

    private String getListWorkingDays(OrderPlacementDTO orderPlacementDTO) {
        return holidayService.getListWorkingDays(orderPlacementDTO.getArrangement().getCreatedDate(),
                orderPlacementDTO.getArrangement().getTradingDate());
    }


    private OrderResponseDto submit(OrderPlacementDTO orderPlacementDTO) {
        StringBuilder url = new StringBuilder(matchingUrl).append("/orders");
        log.info("Submit data to matching service url [{}] with params: {}",
                url.toString(), LoggingUtils.objToStringIgnoreEx(orderPlacementDTO));
        return restTemplate.postForObject(url.toString(), orderPlacementDTO, OrderResponseDto.class);
    }

    @Override
    @Transactional // -> open transaction
    public OrderPlacementDTO handleAFCMechanismResult(OrderPlacementDTO order) {

        if (order == null || order.getArrangement() == null
                || CommonUtils.isInvalidPK(order.getArrangement().getId())) {
            throw new ValidationException("Empty payload or arrangement id for AFC flow!");
        }

        // prepare
        Long arrangementID = order.getArrangement().getId();
        Optional<Arrangement> arrOptional = arrRepo.findByIdFetchOperationPartyPricing(arrangementID);
        Arrangement arr = arrValidator.validateExistence(arrOptional, Constant.ACTIVE);

        // creating counter order + matching records
        OrderPlacementDTO updatedModel = arrService.createMatchingRecords(arr);
        log.info("Done creating matching records for {}", arrangementID);

        return updatedModel;

    }

    @Override
    public void handleAFCMechanismResultAndSendSignal(OrderPlacementDTO order) {

        OrderPlacementDTO updatedModel = handleAFCMechanismResult(order);
        log.info("Done creating matching records for {}", order.getArrangement().getId());

        //Create arrangement log

        arrangementLogService.createArrangementLog(updatedModel, ArrangementAction.CONFIRM);

        // now send noti (don't care about result since @Transactional finished already)
        arrNotiService.findCustomerBrokerInfoAndSendNoti(
                updatedModel, NotificationCodeEnum.BUY_AVAILABLE_FOR_CONFIRM.getSmsTemplate(),
                NotificationCodeEnum.BUY_AVAILABLE_FOR_CONFIRM.getEmailTemplate()).subscribe();
    }

    @Override
    @Transactional // --> open transaction
    public OrderPlacementDTO handleAFTMechanismResult(OrderPlacementDTO order) {

        if (order == null || order.getArrangement() == null
                || CommonUtils.isInvalidPK(order.getArrangement().getId())) {
            throw new ValidationException("Empty payload or arrangement id for AFC flow!");
        }

        // prepare
        Long arrangementID = order.getArrangement().getId();
        if (CommonUtils.isInvalidPK(arrangementID)) {
            throw new NeoFiatsException(ArrangementErrorCode.ARRANGEMENT_RECORD_NOT_FOUND,
                    CommonUtils.format("Empty ID while finding original id {0}", arrangementID));
        }

        Optional<Arrangement> optional = arrRepo.findByIDFetchPartyOperationPricing(arrangementID);
        arrValidator.validateExistence(optional, Constant.ACTIVE);

        Arrangement arr = optional.get();
        log.info("AFT - Done finding original arrangement {}", arr.getId());

        Arrangement finalArr = arr;
        Collection<ArrangementOperation> all = arrService.findAllRelatedOperations(new ArrayList<Arrangement>(){{
            add(finalArr);
        }});
        if (CollectionUtils.isEmpty(all)) {
            throw new NeoFiatsException(ArrangementErrorCode.ARRANGEMENT_RECORD_NOT_FOUND,
                    CommonUtils.format("Empty ID while finding original id {0}", arr.getId()));
        }
        // perform ticking
        List<Arrangement> updatedArrs = arrangementTickingService.tickingDeliveryStatusByOperations(all);
        arr = updatedArrs.stream().filter(tmp -> tmp.getId().equals(finalArr.getId()))
                .findAny().orElseThrow(() -> new NeoFiatsException(CommonUtils.format("Empty arrangement id " +
                        "after updating delivery status {0}", finalArr.getId())));

        log.info("Done updating delivery status of {}", arr.getId());
        return ConverterUtils.castArrangementToOrder(arr, order.getParty().getCustomer());
    }

    @Override
    public void handleAFTMechanismResultAndSendSignal(OrderPlacementDTO order) {

        OrderPlacementDTO updatedModel = handleAFTMechanismResult(order);
        log.info("Using model {} to send delivery notification", LoggingUtils.objToStringIgnoreEx(updatedModel));

        //Create arrangement log
        arrangementLogService.createArrangementLog(updatedModel, ArrangementAction.DELIVERY);

        // now send noti (don't care about result since @Transactional finished already)
        try {
            arrNotiService.findCustomerBrokerInfoAndSendNoti(
                    updatedModel, NotificationCodeEnum.BUY_TRANSFER_SUCCESS.getSmsTemplate(),
                    NotificationCodeEnum.BUY_TRANSFER_SUCCESS.getEmailTemplate())
                    .subscribe();
        } catch (Exception e) {
            // catching to avoid throw to DLQ
            log.error("Error sending noti for {} while handling AFT mechanism", order.getArrangement().getId());
            log.error(e.getMessage(), e);
        }
    }
}