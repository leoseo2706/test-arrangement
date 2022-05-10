package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.payload.EmailTemplateDto;
import com.fiats.tmgcoreutils.payload.SmsTemplateDto;

public interface NotiService {

    void sendEmail(EmailTemplateDto emailTemplateDto);
    void sendSms(SmsTemplateDto smsTemplateDto);
}
