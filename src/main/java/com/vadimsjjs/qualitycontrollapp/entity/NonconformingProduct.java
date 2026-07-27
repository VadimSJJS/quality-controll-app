package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "NONCONFORMING_PRODUCT")
@Getter
@Setter
@NoArgsConstructor
public class NonconformingProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nonconforming_product_seq")
    @SequenceGenerator(name = "nonconforming_product_seq", sequenceName = "SEQ_NONCONFORMING_PRODUCT", allocationSize = 1)
    @Column(name = "ID_NONCONFORMING_PRODUCT", nullable = false)
    private Long id;

    @Column(name = "DETECTION_DATE", nullable = false)
    private LocalDate detectionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_PRODUCTION_SITE", nullable = false)
    private ProductionSite productionSite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_DETECTION_SOURCE", nullable = false)
    private DetectionSource detectionSource;

    @Column(name = "WEIGHT_TONNES", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightTonnes;

    @Column(name = "IRREPARABLE_WEIGHT_TONNES", precision = 10, scale = 3)
    private BigDecimal irreparableWeightTonnes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_DEFECT_TYPE", nullable = false)
    private DefectType defectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_DEFECT_CAUSE")
    private DefectCause defectCause;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_DEFECT_SUBCAUSE")
    private DefectCause defectSubcause;

    @Column(name = "NOTE", length = 4000)
    private String note;

    @Column(name = "PRODUCT_CODE")
    private Long productCode;

    @Column(name = "REEL_NUMBER", length = 50)
    private String reelNumber;

    @Column(name = "HEAT_NUMBER", length = 50)
    private String heatNumber;

    @Column(name = "MANUFACTURER_BRIGADE")
    private Long manufacturerBrigade;

    @Column(name = "BUNDLE_NUMBER", length = 50)
    private String bundleNumber;

    @Column(name = "MANUFACTURER_WORKSHOP", length = 100)
    private String manufacturerWorkshop;

    @Column(name = "EQUIPMENT_KEY", length = 100)
    private String equipmentKey;

    @Column(name = "DIAMETER")
    private Double diameter;

    @Column(name = "STEEL_CORD_CONSTRUCTION", length = 50)
    private String steelCordConstruction;

    @Column(name = "OPERATOR_PERSONAL_NUMBER")
    private Long operatorPersonalNumber;

    @Column(name = "BRIGADE")
    private Long brigade;

    @Column(name = "STEEL_GRADE", length = 50)
    private String steelGrade;

    @Column(name = "WORKPIECE_KEY", length = 100)
    private String workpieceKey;

    @Column(name = "QUANTITY")
    private Integer quantity;

    @Column(name = "REWORK_QUANTITY")
    private Integer reworkQuantity;

    @Column(name = "REWORK_DATE")
    private LocalDate reworkDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_REWORK_TYPE")
    private ReworkType reworkType;

    @Column(name = "REWORK_WEIGHT_TONNES", precision = 10, scale = 3)
    private BigDecimal reworkWeightTonnes;
}