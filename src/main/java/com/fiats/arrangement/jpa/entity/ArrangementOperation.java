package com.fiats.arrangement.jpa.entity;

import com.fiats.tmgjpa.entity.RecordStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "ARRANGEMENT_OPERATION")
public class ArrangementOperation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Access(AccessType.PROPERTY)
    private Long id;

    @Column(name = "DESCRIPTION")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARRANGEMENT_ID")
    private Arrangement arrangement;

    @Column(name = "CUSTOMER_STATUS")
    private Integer customerStatus;

    @Column(name = "CUSTOMER_STATUS_DATE")
    private Timestamp customerStatusDate;

    @Column(name = "CONTRACT_STATUS")
    private Integer contractStatus;

    @Column(name = "CONTRACT_STATUS_DATE")
    private Timestamp contractStatusDate;

    @Column(name = "PAYMENT_STATUS")
    private Integer paymentStatus;

    @Column(name = "PAYMENT_STATUS_DATE")
    private Timestamp paymentStatusDate;

    @Column(name = "DELIVERY_STATUS")
    private Integer deliveryStatus;

    @Column(name = "DELIVERY_STATUS_DATE")
    private Timestamp deliveryStatusDate;

    @Column(name = "COLLATERAL_STATUS")
    private Integer collateralStatus;

    @Column(name = "COLLATERAL_STATUS_DATE")
    private Timestamp collateralStatusDate;

    @Column(name = "RELEASE_STATUS")
    private Integer releaseStatus;

    @Column(name = "RELEASE_STATUS_DATE")
    private Timestamp releaseStatusDate;

    @Column(name = "ARRANGEMENT_RELATION_ID")
    private Long arrangementRelationId;

    public ArrangementOperation() {
        customerStatus = RecordStatus.INACTIVE.getStatus();
        contractStatus = RecordStatus.INACTIVE.getStatus();
        paymentStatus = RecordStatus.INACTIVE.getStatus();
        deliveryStatus = RecordStatus.INACTIVE.getStatus();
        collateralStatus = RecordStatus.INACTIVE.getStatus();
        releaseStatus = RecordStatus.INACTIVE.getStatus();
    }
}
