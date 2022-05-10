package com.fiats.arrangement.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fiats.tmgcoreutils.constant.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.groups.Default;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortTransactionDTO implements Serializable {

    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.FORMAT_SQLSERVER_SHORT, timezone = Constant.TIMEZONE_ICT)
    private Timestamp createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.FORMAT_SQLSERVER_SHORT, timezone = Constant.TIMEZONE_ICT)
    private Timestamp updatedDate;

    private String customerId;

    private String customerCode;

    private Long accountId;

    private String accountCode;

    private String prodVanillaCode;

    private String prodDerivativeCode;

    private String assetCode;

    private String arrangementId;

    private String channel;

    private String action;

    private BigDecimal volume;

    private BigDecimal unitPrice;

    private BigDecimal price;

    private BigDecimal principal;

    private BigDecimal rate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.FORMAT_SQLSERVER_SHORT, timezone = Constant.TIMEZONE_ICT)
    private Timestamp appliedDate;

    private Integer status;

    public interface Insert extends Default {
    }

    ;
}
