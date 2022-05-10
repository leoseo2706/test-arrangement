package com.fiats.arrangement.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
@Slf4j
public enum ArrangementTypeEnum {

    BUY(1, "buy" ,"Buy type"),
    SELL(2, "sell", "Sell type"),
    MATCHING(3, "matching", "Matching type"),
    COLLATERAL(4,  "collateral", "Collateral type"),
    RELEASE(5, "release","Release type"),
    ;

    // lookup hashMap
    private static Map<Integer, ArrangementTypeEnum> lookup = new HashMap<>();

    static {
        EnumSet.allOf(ArrangementTypeEnum.class).forEach(e -> lookup.put(e.getType(), e));
    }

    private Integer type;

    private String typeStr;

    private String description;

    public static ArrangementTypeEnum lookForType(Integer type) {
        return type != null ? lookup.get(type) : null;
    }

    public boolean isBuyOrsell() {
        return this == BUY || this == SELL;
    }
}
