package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    private Long diameterId;
    private String diameterValue;
    private Long operatorPersonalNumber;
    private LocalDate reworkDate;
    private BigDecimal reworkWeightTonnes;

    /** Восстановлено, т. */
    private BigDecimal restoredWeightTonnes;

    /** Переназначено, т. */
    private BigDecimal reassignedWeightTonnes;

    /** Неисправимый брак по результатам доработки, т. */
    private BigDecimal scrappedWeightTonnes;
    private String status;

    private Long steelGradeId;
    private String steelGrade;
    private String unitNumber;
    private Integer quantity;
    private Integer reworkQuantity;
    private String workpieceKey;
    private String steelCordConstruction;
    private Long brigade;

    /** Отдельные действия по доработке: дата, вид, масса, примечание. */
    private List<ReworkActionResponse> reworkActions;
}