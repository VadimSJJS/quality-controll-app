package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "PRODUCTION_REPORT")
@Getter
@Setter
@NoArgsConstructor
public class ProductionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "production_report_seq")
    @SequenceGenerator(name = "production_report_seq", sequenceName = "SEQ_PRODUCTION_REPORT", allocationSize = 1)
    @Column(name = "ID_PRODUCTION_REPORT", nullable = false)
    private Long id;

    @Column(name = "REPORT_DATE", nullable = false)
    private LocalDate reportDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_PRODUCTION_SITE", nullable = false)
    private ProductionSite productionSite;

    @Column(name = "PRODUCED_WEIGHT_TONNES", nullable = false, precision = 10, scale = 3)
    private BigDecimal producedWeightTonnes;

    @Column(name = "SOURCE_SYSTEM", length = 50)
    private String sourceSystem;
}
