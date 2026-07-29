package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NonconformingProductResponse {

    private Long id;
    private LocalDate detectionDate;

    private Long productionSiteId;
    private String productionSiteName;

    private Long detectionSourceId;
    private String detectionSourceName;

    private Long defectTypeId;
    private String defectTypeName;

    private Long defectCauseId;
    private String defectCauseName;

    private Long defectSubcauseId;
    private String defectSubcauseName;

    private Long reworkTypeId;
    private String reworkTypeName;

    private BigDecimal weightTonnes;
    private BigDecimal irreparableWeightTonnes;
    private String note;
    private Long productCode;
    private String reelNumber;
    private String heatNumber;
    private Long manufacturerBrigade;
    private String bundleNumber;
    private String manufacturerWorkshop;
    private String equipmentKey;
    private Long operatorPersonalNumber;
    private LocalDate reworkDate;
    private BigDecimal reworkWeightTonnes;
    private String status;
}