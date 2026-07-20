package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NonconformingProductResponse {

    private Long id;
    private LocalDate detectionDate;
    private String productionSiteName;
    private String detectionSourceName;
    private BigDecimal weightTonnes;
    private BigDecimal irreparableWeightTonnes;
    private String defectTypeName;
    private String defectCauseName;
    private String defectSubcauseName;
    private String note;
    private Long productCode;
    private String reelNumber;
    private String heatNumber;
    private Long manufacturerBrigade;
    private String bundleNumber;
    private String manufacturerWorkshop;
    private String equipmentKey;
    private LocalDate reworkDate;
    private String reworkTypeName;
    private BigDecimal reworkWeightTonnes;
    private String status; // "REWORKED", "NOT_REWORKED", "DEFECT"
}