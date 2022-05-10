package com.fiats.arrangement.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public enum ArrangementRelationTypeEnum {

    MATCHING(1), // two MATCHING rows inside ArrangementRelation (when inserting to matching)
    // ARRANGEMENT_ID = MATCHING, RELATED_ARRANGEMENT_ID = BUY/SELL (2 RECORDS)

    SELL_ARRANGEMENT_ID(2), // match sell arrangement to which owned sell arrangement
    // ARRANGEMENT_ID = SELL, RELATED_ARRANGEMENT_ID = ORIGINAL_SELL (1 RECORD)


    // for future development -> collateral
    ;

    private Integer type;
}
