package com.fiats.arrangement.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationCodeEnum {

    BUY_BROKER_REFERRED("SB01", "EB01"),
    SELL_BROKER_REFERRED("SS01", "ES01"),
    BUY_CONTRACT_SIGNED("SB02", "EB02"),
    SELL_CONTRACT_SIGNED("SS02", "ES02"),
    BUY_CANCELLED("SB09", "EB09"),
    SELL_CANCELLED("SS05", "ES05"),
    BUY_AVAILABLE_FOR_CONFIRM("SB03", "EB03"),
    BUY_PAYMENT_SUCCESS("SB04", "EB04"),
    SELL_PAYMENT_SUCCESS("SS03", "ES03"),
    BUY_TRANSFER_SUCCESS("SB05", "EB05"),
    SELL_TRANSFER_SUCCESS("SS04", "ES04"),
    ;

    private String smsTemplate;
    private String emailTemplate;

    // all brokers shall be bcc with emails
}
