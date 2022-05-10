package com.fiats.arrangement.jpa.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ARRANGEMENT_PARTY")
public class ArrangementParty implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @Access(AccessType.PROPERTY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARRANGEMENT_ID")
    private Arrangement arrangement;

    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @Column(name = "ROLE")
    private String role;
}
