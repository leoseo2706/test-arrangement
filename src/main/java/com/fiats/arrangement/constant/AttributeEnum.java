package com.fiats.arrangement.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public enum AttributeEnum {

    DEADLINE_PAYMENT("deadlinePayment"),
    LIMITED_DAY("limitedDay"),
    BOND_STATUS("bondStatus"),
    LOCK("lock"),
    MIN_TRADING_AMOUNT("minTradingAmt")
    ;

    private String variable;
}
