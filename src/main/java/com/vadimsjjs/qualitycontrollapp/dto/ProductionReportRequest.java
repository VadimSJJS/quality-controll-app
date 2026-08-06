package com.vadimsjjs.qualitycontrollapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ProductionReportRequest {

    @NotNull(message = "Дата отчёта обязательна")
    private LocalDate reportDate;

    @NotNull(message = "Участок обязателен")
    private Long productionSiteId;

    @NotNull(message = "Произведено продукции обязательно")
    @Positive(message = "Вес должен быть положительным")
    private BigDecimal producedWeightTonnes;

    private String sourceSystem;
}
