package com.fiats.arrangement.jpa.entity;

import com.fiats.tmgjpa.entity.LongIDIdentityBase;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ARRANGEMENT_RELATION")
public class ArrangementRelation extends LongIDIdentityBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARRANGEMENT_ID")
    private Arrangement arrangement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RELATED_ARRANGEMENT_ID")
    private Arrangement relatedArrangement;

    @Column(name = "TYPE")
    private Integer type;
}
