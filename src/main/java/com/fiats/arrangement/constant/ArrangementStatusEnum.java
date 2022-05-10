package com.fiats.arrangement.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArrangementStatusEnum {

    INACTIVE(0, "Inactive record"),
    ACTIVE(1, "Active record"),
    CANCELLED(2, "Cancelled record"),
    REFERENCE(3, "Broker reference"),
    ;

    private int status;
    private String description;

}
