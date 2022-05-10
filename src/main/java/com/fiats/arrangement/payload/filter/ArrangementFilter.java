package com.fiats.arrangement.payload.filter;

import com.fiats.tmgjpa.filter.BaseFilter;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class ArrangementFilter extends BaseFilter {

    private Boolean repurchase;

    private String productCode;

    private Boolean listedStatus;

    private String listedCode;

    private Integer quantity;

    private String tradingDate;

    private List<Integer> filterStatuses;

    private List<Long> customerIds;

    private Timestamp startDate;

    private Timestamp endDate;

    private List<Integer> types;
}
