package com.fiats.arrangement.jpa.entity;

import com.fiats.tmgjpa.entity.LongIDIdentityBase;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "SNAPSHOT_CONTRACT_DETAIL")
public class SnapshotContractDetail extends LongIDIdentityBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SNAPSHOT_ID")
    private SnapshotContract snapshotContract;

    @Column(name = "GROUP_DOC_HISTORY_ID")
    private Long groupDocHistoryId;

    @Column(name = "TEMPLATE_VERSION_ID")
    private Long templateVersionId;

    @Column(name = "PARAM")
    private String parameters;

    @Column(name = "TEMPLATE_CONTENT")
    private String templateContent;

    @Column(name = "CONTENT")
    private String content;

    @Column(name = "SNAPSHOT_DATE")
    private Timestamp snapshotDate;

    @Column(name = "TEMPLATE_PATH")
    private String templatePath;
}
