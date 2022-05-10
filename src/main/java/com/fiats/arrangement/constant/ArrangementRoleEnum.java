package com.fiats.arrangement.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArrangementRoleEnum {

    PURCHASER("Purchaser role"),
    OWNER("Owner role"),
    BROKER("Broker role"),
    BUYER("Buyer role"),
    SELLER("Seller role"),
    ORGANIZATION("Organization role")
    ;

    private String description;
}
