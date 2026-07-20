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

import java.math.BigDecimal;

@Entity
@Table(name = "HLP_PRODUCTION_SITE")
@Getter
@Setter
@NoArgsConstructor
public class ProductionSite {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hlp_production_site_seq")
    @SequenceGenerator(name = "hlp_production_site_seq", sequenceName = "SEQ_HLP_PRODUCTION_SITE", allocationSize = 1)
    @Column(name = "ID_PRODUCTION_SITE", nullable = false)
    private Long id;

    @Column(name = "SITE_CODE", nullable = false, length = 10)
    private String siteCode;

    @Column(name = "SITE_NAME", nullable = false, length = 100)
    private String siteName;

    @Column(name = "ALLOWABLE_DEFECT_PERCENT", precision = 10, scale = 3)
    private BigDecimal allowableDefectPercent;
}
