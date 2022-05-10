package com.fiats.arrangement.jpa.entity;

import com.fiats.arrangement.utils.ConverterUtils;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgjpa.entity.LongIDIdentityBase;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.StringJoiner;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ACCOUNTING_ENTRY", schema = "FIATS")
public class AccountingEntry extends LongIDIdentityBase {

    @Column(name = "ENTRY_NO")
    private Integer entryNo;

    @Column(name = "ENTRY_TRANSACTION_DATE")
    private Timestamp entryTransactionDate;

    @Column(name = "ENTRY_EFFECTIVE_DATE")
    private Timestamp entryEffectiveDate;

    @Column(name = "ENTRY_DESCRIPTION")
    private String entryDescription;

    @Column(name = "ENTRY_AMOUNT")
    private BigDecimal entryAmount;

    @Column(name = "ENTRY_REMAINING_AMOUNT")
    private BigDecimal entryRemainingAmount;

    @Column(name = "ENTRY_TYPE")
    private Integer entryType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARRANGEMENT_ID")
    private Arrangement arrangement;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "NOTE")
    private String note;

    public String buildUniqueKey() {
        StringJoiner sb = new StringJoiner(Constant.COMMA);
        sb.add(ConverterUtils.castToString(entryNo));
        sb.add(ConverterUtils.castToString(DateHelper.formatDateSilently(entryTransactionDate)));
        sb.add(ConverterUtils.castToString(DateHelper.formatDateSilently(entryEffectiveDate)));
        sb.add(ConverterUtils.castToString(entryDescription));
        sb.add(ConverterUtils.formatDecimal(entryAmount));
        sb.add(ConverterUtils.castToString(entryType));
        sb.add(ConverterUtils.formatDecimal(entryRemainingAmount));
        return sb.toString();
    }
}
