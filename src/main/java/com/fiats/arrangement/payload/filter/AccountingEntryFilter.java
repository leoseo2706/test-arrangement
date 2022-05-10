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
public class AccountingEntryFilter extends BaseFilter {

    private String arrangementCode;

    private Timestamp startDate;

    private Timestamp endDate;

    private List<String> statuses;

    private Integer type;

    private String description;

}
