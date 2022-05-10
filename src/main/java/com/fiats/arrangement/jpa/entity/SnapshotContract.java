package com.fiats.arrangement.jpa.entity;

import com.fiats.tmgjpa.entity.LongIDIdentityBase;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "SNAPSHOT_CONTRACT")
public class SnapshotContract extends LongIDIdentityBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARRANGEMENT_ID")
    private Arrangement arrangement;

    @Column(name = "ARRANGEMENT_CODE")
    private String arrangementCode;

    @Column(name = "CONT_GROUP_DOC_ID")
    private Long contGroupDocId;

    @Column(name = "SNAPSHOT_DATE")
    private Timestamp snapshotDate;

    @Column(name = "PARAM")
    private String parameters;

}
