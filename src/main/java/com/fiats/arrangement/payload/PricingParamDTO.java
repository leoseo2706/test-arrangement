package com.fiats.arrangement.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fiats.tmgcoreutils.constant.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PricingParamDTO implements Serializable {

    private static final long serialVersionUID = 428559178039053458L;

    private String productCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = Constant.FORMAT_SQLSERVER_SHORT, timezone = Constant.TIMEZONE_ICT)
    private Timestamp tradingDate;

    private String action;

    private Integer quantity;

    private String custType;

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = Constant.FORMAT_SQLSERVER_SHORT, timezone = Constant.TIMEZONE_ICT)
    private Timestamp buyDate;

    private Double buyPrice;

    private Double buyVolume;

    private Double buyRate;
}
