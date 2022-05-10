package com.fiats.arrangement.redis.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fiats.tmgcoreutils.constant.Constant;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.sql.Timestamp;

@RedisHash("ARRANGEMENT_EXCEPTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ArrangementException {

    private static final long serialVersionUID = 1L;

    @Id
    @Indexed
    private String id;

    @Indexed
    private String code;

    @Indexed
    private Integer type;

    private String tradingDate;

    private String matchingDate;

    private String deliveryDate;

    @Indexed
    private Long productDerivativeId;

    @Indexed
    private String productDerivativeCode;

    @Indexed
    private Long customerId;

    private String partyName;

    private String partyIdentity;

    private String partyStockAccount;

    private Long brokerId;

    private String brokerName;

    private Integer volume;

    private Double rate;

    private Double unitPrice;

    private Double price;

    private Double principal;

    private Double transactionFee;

    private Double transactionTax;

    private Long assetId;

    private String assetCode;

    @Indexed
    private String status;
}
