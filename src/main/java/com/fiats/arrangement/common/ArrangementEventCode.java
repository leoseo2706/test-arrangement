package com.fiats.arrangement.common;

import com.fiats.tmgcoreutils.common.ErrorCode;
import com.fiats.tmgcoreutils.common.EventCode;
import com.neo.exception.NeoException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public enum ArrangementEventCode {
    MATCHING_CONFIRMING_AFC("afc.confirming", "Send event to confirming AFC"),
    MATCHING_CONFIRMING_AFT("aft.confirming", "Send event to confirming AFT"),


    // tmp queue to hold events before firing post to portfolio
    PORTFOLIO_CONFIRMING_ACTION("portfolio.confirming.action", "Send event to confirm paid order to portfolio")
    ;

    private String code;
    private String description;

    // lookup table to be used to find enum for conversion
    private static final Map<String, ArrangementEventCode> lookupData = new HashMap<String, ArrangementEventCode>();

    static {
        for (ArrangementEventCode e : EnumSet.allOf(ArrangementEventCode.class)) {
            lookupData.put(e.getCode(), e);
        }
    }

    public static ArrangementEventCode lookup(String code) {
        ArrangementEventCode ms = lookupData.get(code);
        if (ms == null) {
            throw new NeoException(null, ErrorCode.INVALID_PARAMETER,
                    new StringBuilder("Not found Bond event code with code:").append(code));
        } else {
            return ms;
        }
    }

    ArrangementEventCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getTopicName(String prefix, String applicationName) {
        return new StringBuilder(prefix).append(".")
                .append(applicationName).append(".")
                .append(code).toString();
    }
}
