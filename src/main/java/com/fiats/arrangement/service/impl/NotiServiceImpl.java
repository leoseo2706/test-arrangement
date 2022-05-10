package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.service.NotiService;
import com.fiats.tmgcoreutils.payload.EmailTemplateDto;
import com.fiats.tmgcoreutils.payload.SmsTemplateDto;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotiServiceImpl implements NotiService {

    @Value("${topic.kafka.notification.send-sms}")
    private String smsTopic;

    @Value("${topic.kafka.notification.send-email}")
    private String emailTopic;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Override
    public void sendEmail(EmailTemplateDto emailTemplateDto) {
        log.info("Send email to topic: {} - {}", emailTopic, LoggingUtils.objToStringIgnoreEx(emailTemplateDto));
        kafkaTemplate.send(emailTopic, emailTemplateDto);
    }

    @Override
    public void sendSms(SmsTemplateDto smsTemplateDto) {
        log.info("Send sms to topic: {} - {}", smsTopic, LoggingUtils.objToStringIgnoreEx(smsTemplateDto));
        kafkaTemplate.send(smsTopic, smsTemplateDto);
    }
}
