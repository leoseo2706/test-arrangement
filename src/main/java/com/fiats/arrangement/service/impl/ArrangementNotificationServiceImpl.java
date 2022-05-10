package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.arrangement.constant.AttributeEnum;
import com.fiats.arrangement.constant.NotificationCodeEnum;
import com.fiats.arrangement.internal.event.NotificationBrokerEvent;
import com.fiats.arrangement.internal.event.NotificationWrapperEvent;
import com.fiats.arrangement.service.ArrangementNotificationService;
import com.fiats.arrangement.service.AttributeService;
import com.fiats.arrangement.service.BrokerService;
import com.fiats.arrangement.service.CustomerService;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.constant.VelocityParamKeyEnum;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.service.NotificationService;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@Service
@Slf4j
public class ArrangementNotificationServiceImpl implements ArrangementNotificationService {

    @Autowired
    NotificationService notificationService;

    @Value("${fiats.internal.api.noti.url}")
    private String notiUrl;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    CustomerService customerService;

    @Autowired
    AttributeService attributeService;

    @Autowired
    BrokerService brokerService;

    @Override
    @Async(Constant.DEFAULT_THREAD_POOL)
    public void listenAndCallNoti(NotificationWrapperEvent event) {

        log.info("Received event to send noti {}", LoggingUtils.objToStringIgnoreEx(event));

        ArrangementNotificationDTO notiModel = event.getNotiModel();
        NotificationCodeEnum template = event.getTemplate();

        if (template != null && notiModel != null) {
            sendNotifications(notiModel, template.getSmsTemplate(),
                    template.getEmailTemplate(), null).subscribe();
        }

    }

    @Override
    @Async(Constant.DEFAULT_THREAD_POOL)
    public void listenFindBrokerAndCallNoti(NotificationBrokerEvent event) {

        log.info("Received event to send noti {}", LoggingUtils.objToStringIgnoreEx(event));

        OrderPlacementDTO orderDTO = event.getOrderDTO();
        CustomerBrokerWrapper cusBroker = event.getCusBroker();
        NotificationCodeEnum template = event.getTemplate();

        // send noti
        if (cusBroker != null && cusBroker.getCustomer() != null && template != null) {
            CustomerDTO customer = cusBroker.getCustomer();
            Long brokerCustomerId = cusBroker.getBroker() != null
                    ? cusBroker.getBroker().getCustomerId() : null;

            findBrokerAndSendNoti(brokerCustomerId,
                    customer, orderDTO, template.getSmsTemplate(), template.getEmailTemplate(),
                    event.getAttachmentPaths())
                    .subscribe();
        }

    }

    @Override
    public Mono<Void> findCustomerBrokerInfoAndSendNoti(OrderPlacementDTO model,
                                                        String smsTemplateCode,
                                                        String emailTemplateCode) {

        Set<Long> customerIds = new HashSet<>();

        if (model.getParty() != null
                && !CommonUtils.isInvalidPK(model.getParty().getCustomerId())) {
            customerIds.add(model.getParty().getCustomerId());
        }

        if (model.getBrokerParty() != null
                && !CommonUtils.isInvalidPK(model.getBrokerParty().getCustomerId())) {
            customerIds.add(model.getBrokerParty().getCustomerId());
        }

        if (CollectionUtils.isEmpty(customerIds)) {
            log.info("Found no customer id for {}", LoggingUtils.objToStringIgnoreEx(model));
            return Mono.empty();
        }

        log.info("searching for customer ids {}", LoggingUtils.objToStringIgnoreEx(customerIds));

        return Mono.fromCallable(() -> ArrangementNotificationDTO.builder()
                .orderPlacementDTO(model).build())
                .zipWith(customerService.findCustomerBrokerByIds(customerIds).collectList(),
                        (notiModel, cusBrokers) -> {

//                            log.info("Done finding customer broker info. Using notiModel {} and cusBroker {}",
//                                    LoggingUtils.objToStringIgnoreEx(notiModel), LoggingUtils.objToStringIgnoreEx(cusBrokers));

                            if (CollectionUtils.isEmpty(cusBrokers)) {
                                return notiModel;
                            }

                            int size = cusBrokers.size();

                            CustomerDTO customerDTO = cusBrokers.stream().filter(cusBroker -> cusBroker.getCustomer().getId().equals(notiModel.getOrderPlacementDTO().getParty().getCustomerId()))
                                    .findAny().map(CustomerBrokerWrapper::getCustomer).orElse(null);
                            BrokerDTO brokerDTO = cusBrokers.stream().filter(cusBroker -> cusBroker.getBroker().getCustomerId().equals(notiModel.getOrderPlacementDTO().getBrokerParty().getCustomerId()))
                                    .findAny().map(CustomerBrokerWrapper::getBroker).orElse(null);

                            notiModel.setCustomerDTO(customerDTO);
                            notiModel.setBrokerDTO(brokerDTO);

                            log.info("Done finding customer broker info. Using notiModel {} and cusBroker {}",
                                    LoggingUtils.objToStringIgnoreEx(notiModel), LoggingUtils.objToStringIgnoreEx(cusBrokers));
                            return notiModel;
                        })
                .zipWhen(notiModel -> {
                    ArrangementDTO arr = notiModel.getOrderPlacementDTO().getArrangement();
                    return Mono.justOrEmpty(attributeService.findSystemAttribute(AttributeEnum.DEADLINE_PAYMENT.getVariable(),
                            arr.getTradingDate(), arr.getProductDerivativeCode(), arr.getProductVanillaCode(),
                            arr.getProductAgreementCode(), ArrangementTypeEnum.lookForType(arr.getType()), arr.getChannel(),
                            notiModel.getCustomerDTO().getCustomerType()));
                })
                .map(tuple -> {
                    tuple.getT1().setDeadlinePayment(tuple.getT2());
                    return tuple.getT1();
                })
                .zipWhen(modelDTO -> {
                    log.info("Sending notification with sms template {} and email template {}",
                            smsTemplateCode, emailTemplateCode);
                    return sendNotifications(modelDTO, smsTemplateCode, emailTemplateCode, null);
                })
                .then();

    }

    @Override
    public Mono<Void> sendNotifications(ArrangementNotificationDTO arrNotification,
                                        String smsTemplateCode,
                                        String emailTemplateCode,
                                        List<String> attachmentPaths) {

        if (arrNotification == null || arrNotification.getOrderPlacementDTO() == null
                || arrNotification.getOrderPlacementDTO().getArrangement() == null
                || CommonUtils.isInvalidPK(arrNotification.getOrderPlacementDTO().getArrangement().getId())) {
            log.warn("Empty arrangement id!");
            return Mono.empty().then();
        }

        OrderPlacementDTO orderDTO = arrNotification.getOrderPlacementDTO();
        CustomerDTO customerDTO = arrNotification.getCustomerDTO();
        BrokerDTO brokerDTO = arrNotification.getBrokerDTO();
        PropsResp deadlinePayment = arrNotification.getDeadlinePayment();

        return Mono.justOrEmpty(orderDTO)
                .map(orderPlacementDTO -> {

                    Map<String, Object> mapModel = new HashMap<>();
                    if (customerDTO != null) {
                        mapModel.put(VelocityParamKeyEnum.CUSTOMER_MODEL_KEY.getKey(), customerDTO);
                    }

                    if (deadlinePayment != null) {
                        mapModel.put(VelocityParamKeyEnum.DEADLINE_PAYMENT_KEY.getKey(), deadlinePayment.getValue());
                    }

                    if (orderPlacementDTO != null) {
                        OrderPlacementDTO copyOrder = new OrderPlacementDTO();
                        BeanUtils.copyProperties(orderPlacementDTO, copyOrder,
                                "contracts"); // this attribute is useless
                        mapModel.put(VelocityParamKeyEnum.ORDER_PLACEMENT_MODEL_KEY.getKey(), copyOrder);
                    }

                    if (brokerDTO != null) {
                        mapModel.put(VelocityParamKeyEnum.BROKER_MODEL_KEY.getKey(), brokerDTO);
                    }

                    return mapModel;

                })
                .map(mapModel -> {

                    SmsTemplateDto smsTemplateDto = SmsTemplateDto.builder()
                            .destination(customerDTO.getPhone())
                            .templateCode(smsTemplateCode)
                            .arrangementId(orderDTO.getArrangement().getId())
                            .params(mapModel)
                            .build();

                    String requestId = orderDTO.getArrangement().getId() == null
                            ? null : String.valueOf(orderDTO.getArrangement().getId());

                    EmailTemplateDto emailTemplateDto = EmailTemplateDto.builder()
                            .requestId(requestId)
                            .to(customerDTO.getEmail())
                            .bcc(brokerDTO != null && StringUtils.hasText(brokerDTO.getEmail())
                                    ? brokerDTO.getEmail() : null)
                            .templateCode(emailTemplateCode)
                            .arrangementId(orderDTO.getArrangement().getId())
                            .attachments(attachmentPaths)
                            .params(mapModel)
                            .build();

                    return NotificationWrapper.builder()
                            .emailTemplateDto(emailTemplateDto)
                            .smsTemplateDto(smsTemplateDto)
                            .build();
                })
                .doOnNext(notificationWrapper -> sendSMSTemplate(notificationWrapper.getSmsTemplateDto()).subscribe())
                .doOnNext(notificationWrapper -> sendEmailTemplate(notificationWrapper.getEmailTemplateDto()).subscribe())
                .then()
                .publishOn(Schedulers.fromExecutor(executor));

    }

    @Override
    public Mono<Void> sendSMSTemplate(SmsTemplateDto smsTemplateDto) {
        return Mono.justOrEmpty(smsTemplateDto)
                .filter(notiWrapper -> smsTemplateDto != null
                        && StringUtils.hasText(smsTemplateDto.getDestination()))
                .zipWhen(notiWrapper -> notificationService.sendSMSTemplate(smsTemplateDto, notiUrl))
                .onErrorResume(e -> Mono.empty()) // ignore error
                .doOnTerminate(() -> log.info("Done sending arrangement sms template {} for arr id {}",
                        smsTemplateDto.getTemplateCode(), smsTemplateDto.getArrangementId()))
                .then()
                .publishOn(Schedulers.fromExecutor(executor));
    }

    @Override
    public Mono<Void> sendEmailTemplate(EmailTemplateDto emailTemplateDto) {
        return Mono.justOrEmpty(emailTemplateDto)
                .filter(notiWrapper -> emailTemplateDto != null
                        && StringUtils.hasText(emailTemplateDto.getTo()))
                .zipWhen(notiWrapper -> notificationService.sendEmailTemplate(notiWrapper, notiUrl))
                .onErrorResume(e -> Mono.empty()) // ignore error
                .doOnTerminate(() -> log.info("Done sending arrangement email template {} for arr id {} and attachments {}",
                        emailTemplateDto.getTemplateCode(), emailTemplateDto.getArrangementId(),
                        emailTemplateDto.getAttachments()))
                .then()
                .publishOn(Schedulers.fromExecutor(executor));
    }

    private Mono<Void> findBrokerAndSendNoti(Long brokerCustomerId,
                                             CustomerDTO customerDTO,
                                             OrderPlacementDTO orderPlacementDTO,
                                             String smsTemplateCode,
                                             String emailTemplateCode,
                                             List<String> attachmentPaths) {

        List<Long> tmpIds = CommonUtils.isInvalidPK(brokerCustomerId)
                ? null : new ArrayList<Long>() {{
            add(brokerCustomerId);
        }};

        return brokerService.findBrokerDTOByCustomerID(tmpIds, true)
                .next()
                .defaultIfEmpty(new BrokerDTO())
                .map(brokerDTO -> ArrangementNotificationDTO.builder()
                        .orderPlacementDTO(orderPlacementDTO)
                        .brokerDTO(brokerDTO)
                        .customerDTO(customerDTO)
                        .build())
                .zipWhen(modelDTO -> {
                    log.info("Sending notification with sms template {} and email template {} with attachment {}",
                            smsTemplateCode, emailTemplateCode, attachmentPaths);
                    return sendNotifications(modelDTO, smsTemplateCode, emailTemplateCode, attachmentPaths);
                })
                .then();
    }

    // don't need this as noti-service already has flatten map inside
//    private Map<String, Object> flattenMap(NotificationModelDTO notiModel) {
//        try {
//            return JsonFlattener.flattenAsMap(plainMapper.writeValueAsString(notiModel));
//        } catch (JsonProcessingException e) {
//            String err = CommonUtils.format("Failed to flatten the map {0}", notiModel);
//            log.error(err, e);
//            throw new NeoFiatsException(ArrangementErrorCode.FLATTEN_MAP_ERROR, err);
//        }
//    }
}