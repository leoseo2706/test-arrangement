package com.fiats.arrangement.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fiats.arrangement.utils.ConverterUtils;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.ArrangementDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.StringJoiner;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountingEntryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Integer entryNo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.FORMAT_SQLSERVER_SHORT,
            timezone = Constant.TIMEZONE_ICT)
    private Timestamp entryTransactionDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.FORMAT_SQLSERVER_SHORT,
            timezone = Constant.TIMEZONE_ICT)
    private Timestamp entryEffectiveDate;

    private String entryDescription;

    private Double entryWithdrawingAmount;

    private Double entryDepositingAmount;

    private Double entryAmount; // either withdrawingAmount or depositingAmount

    private Double entryRemainingAmount;

    private String note;

    private Integer entryType;

    private String status;

    private String upperCaseCode; // hidden attr

    private ArrangementDTO arrangement;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.FORMAT_SQLSERVER_SHORT,
            timezone = Constant.TIMEZONE_ICT)
    private Timestamp createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.FORMAT_SQLSERVER_SHORT,
            timezone = Constant.TIMEZONE_ICT)
    private Timestamp updatedDate;

    /**
     * building composite key to filter duplicates inside db
     *
     * @return a string composite key
     */
    public String buildUniqueKey() {
        StringJoiner sb = new StringJoiner(Constant.COMMA);
        sb.add(ConverterUtils.castToString(entryNo));
        sb.add(ConverterUtils.castToString(DateHelper.formatDateSilently(entryTransactionDate)));
        sb.add(ConverterUtils.castToString(DateHelper.formatDateSilently(entryEffectiveDate)));
        sb.add(ConverterUtils.castToString(entryDescription));
        sb.add(ConverterUtils.formatDouble(entryAmount));
        sb.add(ConverterUtils.castToString(entryType));
        sb.add(ConverterUtils.formatDouble(entryRemainingAmount));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountingEntryDTO)) return false;
        AccountingEntryDTO that = (AccountingEntryDTO) o;
        return Objects.equals(getEntryNo(), that.getEntryNo()) &&
                Objects.equals(getEntryTransactionDate(), that.getEntryTransactionDate()) &&
                Objects.equals(getEntryEffectiveDate(), that.getEntryEffectiveDate()) &&
                Objects.equals(getEntryDescription(), that.getEntryDescription()) &&
                Objects.equals(getEntryWithdrawingAmount(), that.getEntryWithdrawingAmount()) &&
                Objects.equals(getEntryDepositingAmount(), that.getEntryDepositingAmount()) &&
                Objects.equals(getEntryRemainingAmount(), that.getEntryRemainingAmount());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEntryNo(), getEntryTransactionDate(), getEntryEffectiveDate(), getEntryDescription(), getEntryWithdrawingAmount(), getEntryDepositingAmount(), getEntryRemainingAmount());
    }
}