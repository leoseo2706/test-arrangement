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
public class ReportingFilter extends BaseFilter{

    private Timestamp tradingDate;

    private Integer arrangementType;

    private List<Long> derivativeId;

    private List<String> derivativeCode;

    private String arrangementCode;

    private Long customerId;

    private String customerCode;

    private Long brokerId;

    private String brokerCode;

    private Long agencyId;

    private String agencyCode;

    private Integer arrangementStatus;
}
