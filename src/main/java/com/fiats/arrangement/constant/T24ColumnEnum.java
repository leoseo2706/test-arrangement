package com.fiats.arrangement.constant;

import com.fiats.tmgcoreutils.constant.Constant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.*;

@AllArgsConstructor
@Getter
public enum T24ColumnEnum {

    ENTRY_NO("STT", Constant.INACTIVE),
    TRANSACTION_DATE("Ngày giao dịch", Constant.INACTIVE),
    EFFECTIVE_DATE("Ngày hiệu lực", Constant.ACTIVE), // mandatory
    DESCRIPTION("Giao dịch", Constant.ACTIVE), // mandatory
    WITHDRAWING_AMOUNT("Số tiền rút", Constant.INACTIVE), // one of these must be mandatory
    DEPOSITING_AMOUNT("Số tiền gửi", Constant.INACTIVE), // one of these must be mandatory
    REMAINING_AMOUNT("Số dư", Constant.INACTIVE)
    ;

    private String description;
    private boolean mandatory;

    private static Map<String, T24ColumnEnum> lookupDescription = new HashMap<>();
    public static List<T24ColumnEnum> mandatoryColumns = new ArrayList<>();

    static {
        EnumSet.allOf(T24ColumnEnum.class).forEach(t -> {
            lookupDescription.put(t.getDescription().toLowerCase(), t);
            if (t.isMandatory()) {
                mandatoryColumns.add(t);
            }
        });
    }

    public static boolean isInvalidMandatory(Map<Integer, T24ColumnEnum> header) {

        if (CollectionUtils.isEmpty(mandatoryColumns)) {
            return true;
        }

        // find any elements that the header map does not contain but the mandatory list contain
        return mandatoryColumns.stream()
                .filter(e -> !header.containsValue(e))
                .findAny().orElse(null) != null;
    }

    public static T24ColumnEnum lookForColNo(String description) {

        if (description == null) {
            return null;
        }

        return lookupDescription.get(description.trim().toLowerCase());
    }

}
