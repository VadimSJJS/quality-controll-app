package com.vadimsjjs.qualitycontrollapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
public class NonconformingProductRequest {

    @NotNull(message = "Дата выявления обязательна")
    private LocalDate detectionDate;

    @NotNull(message = "Участок обязателен")
    private Long productionSiteId;

    @NotNull(message = "Источник выявления обязателен")
    private Long detectionSourceId;

    @NotNull(message = "Вес обязателен")
    @Positive(message = "Вес должен быть положительным")
    private BigDecimal weightTonnes;

    @PositiveOrZero(message = "Вес неисправимого брака должен быть ≥ 0")
    private BigDecimal irreparableWeightTonnes;

    @NotNull(message = "Вид несоответствия обязателен")
    private Long defectTypeId;

    private Long steelGradeId;
    private Long defectCauseId;
    private Long defectSubcauseId;
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
    private Long reworkTypeId;
    @PositiveOrZero(message = "Вес доработки должен быть ≥ 0")
    private BigDecimal reworkWeightTonnes;
}