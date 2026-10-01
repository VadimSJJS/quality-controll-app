package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Distribution of non-conforming product weight by a dimension and defect type. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionReport {
    private String dimension;
    private String periodFrom;
    private String periodTo;
    private List<String> defectTypes;
    private List<Row> rows;
    private BigDecimal totalWeight;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private String key;
        private BigDecimal total;
        private Map<String, BigDecimal> byDefect;
    }
}
