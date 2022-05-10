package com.fiats.arrangement.service;

import com.fiats.arrangement.internal.event.NotificationBrokerEvent;
import com.fiats.arrangement.internal.event.NotificationWrapperEvent;
import com.fiats.tmgcoreutils.payload.ArrangementNotificationDTO;
import com.fiats.tmgcoreutils.payload.EmailTemplateDto;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import com.fiats.tmgcoreutils.payload.SmsTemplateDto;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ArrangementNotificationService {

    Mono<Void> sendNotifications(ArrangementNotificationDTO arrNotification,
                                 String smsTemplateCode, String emailTemplateCode,
                                 List<String> attachmentPaths);
    Mono<Void> sendSMSTemplate(SmsTemplateDto smsTemplateDto);
    Mono<Void> sendEmailTemplate(EmailTemplateDto emailTemplateDto);

    Mono<Void> findCustomerBrokerInfoAndSendNoti(OrderPlacementDTO order, String smsTemplateCode,
                                                 String emailTemplateCode);

    @EventListener
    void listenFindBrokerAndCallNoti(NotificationBrokerEvent event);

    @EventListener
    void listenAndCallNoti(NotificationWrapperEvent event);
}
