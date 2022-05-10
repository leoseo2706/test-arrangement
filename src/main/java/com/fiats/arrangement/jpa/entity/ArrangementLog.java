package com.fiats.arrangement.jpa.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "ARRANGEMENT_LOG")
public class ArrangementLog implements Serializable {

    private static final long serialVersionUID = -771413896983695553L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Access(AccessType.PROPERTY)
    private Long id;

    @Column(name = "ARRANGEMENTID")
    private Long arrangementId;

    @Column(name = "USERID")
    private Long userId;

    @Column(name = "CUSTOMERID")
    private Long customerId;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "ARRANGEMENTCODE")
    private String arrangementCode;

    @Column(name = "ARRANGEMENTTYPE")
    private Integer arrangementType;

    @Column(name = "CONTENTS")
    private String contents;

    @Column(name = "CREATEDDATE")
    private Timestamp createdDate;

    @Column(name = "ACTIVE")
    private Integer active;

    @Column(name = "LOGTYPE")
    private String logType;
}
