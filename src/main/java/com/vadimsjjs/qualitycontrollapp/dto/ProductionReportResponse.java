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
public class ProductionReportResponse {

    private Long id;
    private LocalDate reportDate;
    private Long productionSiteId;
    private String productionSiteName;
    private String productionSiteCode;
    private BigDecimal producedWeightTonnes;
    private String sourceSystem;
}
