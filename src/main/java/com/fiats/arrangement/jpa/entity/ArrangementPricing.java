package com.fiats.arrangement.jpa.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ARRANGEMENT_PRICING")
public class ArrangementPricing implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Access(AccessType.PROPERTY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARRANGEMENT_ID")
    private Arrangement arrangement;

    @Column(name = "RATE")
    private Double rate;

    @Column(name = "REINVESTMENT_RATE")
    private Double reinvestmentRate;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "UNIT_PRICE")
    private BigDecimal unitPrice;

    @Column(name = "PRINCIPAL")
    private BigDecimal principal; // PRICE + FEE

    @Column(name = "FEE")
    private BigDecimal fee;

    @Column(name = "TAX")
    private BigDecimal tax;

    @Column(name = "AGENCY_FEE")
    private BigDecimal agencyFee;

    @Column(name = "TOTAL_MONEY_RTM")
    private BigDecimal totalMoneyRtm;

    @Column(name = "INVESTMENT_TIME_BY_MONTH")
    private Double investmentTimeByMonth;

    @Column(name = "BROKER_FEE")
    private BigDecimal brokerFee;

    @Column(name = "DISCOUNTED_AMOUNT")
    private BigDecimal discountedAmount;
}
