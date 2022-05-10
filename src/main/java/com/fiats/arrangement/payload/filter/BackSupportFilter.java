package com.fiats.arrangement.payload.filter;

import com.fiats.tmgjpa.filter.BaseFilter;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class BackSupportFilter extends BaseFilter {

    private Long arrangementId;

    private Timestamp tradingDateFrom;

    private Timestamp tradingDateTo;

    private Timestamp matchingDateFrom;

    private Timestamp matchingDateTo;

    private Timestamp paymentDateFrom;

    private Timestamp paymentDateTo;

    private Timestamp deliveryDateFrom;

    private Timestamp deliveryDateTo;

    private Long partyId;

    private Long counterId;

    private String arrangementCode;

    private List<Integer> arrangementType;

    private Integer listedStatus;

    private Boolean exception;

    private List<Long> derivativeId;

    private List<Integer> status;

    private Map<String, List<Integer>> operationStatus;
}
