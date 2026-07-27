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
public class ExcelImportRequest {
    private LocalDate detectionDate;
    private Long brigade;
    private Double diameter;
    private Long productCode;
    private String reelNumber;
    private String heatNumber;
    private String steelGrade;
    private String unitNumber;
    private String workpieceKey;
    private Long operatorPersonalNumber;
    private Long manufacturerBrigade;
    private Integer quantity;
    private String note;
    private BigDecimal weightTonnes;
    private String defectTypeName;
    private String causeName;
    private String subcauseName;
    private String detectionSourceName;
    private LocalDate reworkDate;
    private String reworkTypeName;
    private Integer reworkQuantity;
    private BigDecimal reworkWeightTonnes;
    private String reworkNote;
}