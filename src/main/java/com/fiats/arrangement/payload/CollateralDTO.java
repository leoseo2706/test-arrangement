package com.fiats.arrangement.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fiats.tmgcoreutils.constant.Constant;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CollateralDTO {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull(groups = {Insert.class, Update.class}, message = "Arrangement type can not be null!")
    private Integer type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.FORMAT_SQLSERVER_SHORT, timezone = Constant.TIMEZONE_ICT)
    private Timestamp collateralDate;

    @NotNull(groups = {Insert.class, Update.class}, message = "Arrangement party can not be null!")
    private Long partyId;

    private String partyAccount;

    private String partyName;

    private String partyIdentity;

    private String partyStockAccount;

    private String assetCode;

    @NotNull(groups = {Insert.class, Update.class}, message = "Arrangement asset can not be null!")
    private Long assetId;

    private Long vanillaId;

    private String vanillaCode;

    private Long derivativeId;

    private String derivativeCode;

    private Integer volume;

    private BigDecimal price;

    private Integer mortgageVolume;

    private BigDecimal mortgagePrice;

    private Long userId;

    private String userName;

    private String bankAccountName;

    private String bankAccountNumber;

    private String bankId;

    private String bankBranchId;

    private String status;

    public interface Insert extends Default {
    }

    ;

    public interface Update extends Default {
    }

    ;
}
