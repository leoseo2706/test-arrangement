package com.fiats.arrangement.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountingEntryPriceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal receivedAmount = new BigDecimal(0);

    private BigDecimal diff = new BigDecimal(0);

    private BigDecimal payback = new BigDecimal(0);

}