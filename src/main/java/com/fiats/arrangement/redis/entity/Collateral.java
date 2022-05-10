package com.fiats.arrangement.redis.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fiats.arrangement.payload.CollateralDTO;
import com.fiats.tmgcoreutils.constant.Constant;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;

@RedisHash("ARRANGEMENT_EXCEPTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Collateral {

    private static final long serialVersionUID = 1L;

    @Id
    @Indexed
    private String id;

    private Integer type;

    private String collateralDate;

    @NotNull(groups = {CollateralDTO.Insert.class, CollateralDTO.Update.class}, message = "Arrangement party can not be null!")
    @Indexed
    private Long partyId;

    @Indexed
    private String partyAccount;

    @Indexed
    private String partyName;

    private String partyIdentity;

    private String partyStockAccount;

    @Indexed
    private String assetCode;

    @NotNull(groups = {CollateralDTO.Insert.class, CollateralDTO.Update.class}, message = "Arrangement asset can not be null!")
    @Indexed
    private Long assetId;

    private Long vanillaId;

    private String vanillaCode;

    private Long derivativeId;

    private String derivativeCode;

    private Integer volume;

    private BigDecimal price;

    @NotNull(groups = {CollateralDTO.Insert.class, CollateralDTO.Update.class}, message = "Collatetal volume can not be null!")
    private Integer mortgageVolume;

    private BigDecimal mortgagePrice;

    private Long userId;

    private String userName;

    private String bankAccountName;

    private String bankAccountNumber;

    private String bankId;

    private String bankBranchId;

    @Indexed
    private String status;
}
