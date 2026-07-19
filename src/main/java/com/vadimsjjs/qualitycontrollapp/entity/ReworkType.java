package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "HLP_REWORK_TYPE")
@Getter
@Setter
@NoArgsConstructor
public class ReworkType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hlp_rework_type_seq")
    @SequenceGenerator(name = "hlp_rework_type_seq", sequenceName = "SEQ_HLP_REWORK_TYPE", allocationSize = 1)
    @Column(name = "ID_REWORK_TYPE", nullable = false)
    private Long id;

    @Column(name = "REWORK_CODE", nullable = false, length = 20)
    private String reworkCode;

    @Column(name = "REWORK_NAME", nullable = false, length = 100)
    private String reworkName;
}
