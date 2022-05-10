package com.fiats.arrangement.constant;

import org.springframework.util.StringUtils;

import java.util.*;

public enum AccountingEntryStatusEnum {

    UNMAPPED, // hợp đồng chưa được map
    INSUFFICIENT_CONDITION, // hợp đồng đã được map nhưng chưa thoả mãn 1 điều kiện nào đó
    MAPPED, // hợp đồng đã được map và đã được tick
    INSUFFICIENT_AMOUNT, // hợp đồng đã được map nhưng chưa đủ tiền
    TO_RETURN, // hợp đồng đã được map nhưng cần trả tiền thừa lại


    CLOSED, // hợp đồng đã bị đóng (to-do)
    RETURNED // hợp đồng đã trả tiền lại
    ;

    private static Set<String> lookup = new HashSet<>();

    static {
        for (AccountingEntryStatusEnum e : values()) {
            lookup.add(e.toString());
        }
    }


    public static boolean isValidStatus(String e) {
        return StringUtils.hasText(e) && lookup.contains(e);
    }
}
