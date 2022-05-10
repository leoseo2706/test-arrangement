package com.fiats.arrangement.jpa.entity;

import com.fiats.arrangement.constant.ArrangementRoleEnum;
import com.fiats.tmgjpa.entity.LongIDIdentityBase;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ARRANGEMENT")
public class Arrangement extends LongIDIdentityBase {

    @Column(name = "CODE")
    private String code;

    @Column(name = "TYPE")
    private Integer type;

    @Column(name = "TRADING_DATE")
    private Timestamp tradingDate;

    @Column(name = "EXPIRED_DATE")
    private Timestamp expiredDate;

    @Column(name = "VOLUME")
    private Integer volume;

    @Column(name = "CHANNEL")
    private String channel;

    @Column(name = "PROD_DERIVATIVE_ID")
    private Long productDerivativeId;

    @Column(name = "PROD_DERIVATIVE_CODE")
    private String productDerivativeCode;

    @Column(name = "PROD_VANILLA_ID")
    private Long productVanillaId;

    @Column(name = "PROD_AGREEMENT_CODE")
    private String productAgreementCode;

    @Column(name = "PROD_AGREEMENT_ID")
    private Long productAgreementId;

    @Column(name = "PROD_VANILLA_CODE")
    private String productVanillaCode;

    @Column(name = "LISTED_TYPE")
    private Integer listedType;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "EXCEPTION")
    private Integer exception;

    @Column(name = "AGENCY_ID")
    private Long agencyId;

    @Column(name = "AGENCY_CODE")
    private String agencyCode;

    // real @OneToOne
    @OneToMany(mappedBy = "arrangement", fetch = FetchType.LAZY)
    private Set<ArrangementOperation> operations = new HashSet<>();

    // fake @OneToOne
    @OneToMany(mappedBy = "arrangement", fetch = FetchType.LAZY)
    private Set<ArrangementPricing> prices = new HashSet<>();

    // real @OneToMany
    @OneToMany(mappedBy = "arrangement", fetch = FetchType.LAZY)
    private Set<ArrangementParty> parties = new HashSet<>();

    // real @OneToMany
    @OneToMany(mappedBy = "arrangement", fetch = FetchType.LAZY)
    private Set<ArrangementRelation> originRelations = new HashSet<>();

    // real @OneToMany
    @OneToMany(mappedBy = "relatedArrangement", fetch = FetchType.LAZY)
    private Set<ArrangementRelation> relatedArrs = new HashSet<>();

    public Arrangement(String code, Integer type, Timestamp tradingDate, Timestamp expiredDate,
                       Integer volume, String channel, Long productDerivativeId, String productDerivativeCode,
                       Long productVanillaId, String productVanillaCode, Long productAgreementId, String productAgreementCode,
                       Integer listedType, Long agencyId, String agencyCode) {
        this.code = code;
        this.type = type;
        this.tradingDate = tradingDate;
        this.expiredDate = expiredDate;
        this.volume = volume;
        this.channel = channel;
        this.productDerivativeId = productDerivativeId;
        this.productDerivativeCode = productDerivativeCode;
        this.productVanillaId = productVanillaId;
        this.productVanillaCode = productVanillaCode;
        this.productAgreementId = productAgreementId;
        this.productAgreementCode = productAgreementCode;
        this.listedType = listedType;
        this.agencyId = agencyId;
        this.agencyCode = agencyCode;
    }

    public ArrangementOperation getOperation() {
        return operations.stream().findAny().orElse(null);
    }

    public ArrangementPricing getPricing() {
        return prices.stream().findAny().orElse(null);
    }

    public ArrangementParty getParty(ArrangementRoleEnum role) {
        return this.getParties().stream()
                .filter(party -> role.toString().equals(party.getRole()))
                .findAny()
                .orElse(null);
    }
}
