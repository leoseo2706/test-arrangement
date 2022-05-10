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
public class ArrangementExceptionFilter extends BaseFilter {

    private Timestamp fromDate;

    private Timestamp toDate;

    private Long partyId;

    private String arrangementCode;

    private List<Long> derivativeId;

    private Integer type;

    private Integer status;

    private String channel;
}
