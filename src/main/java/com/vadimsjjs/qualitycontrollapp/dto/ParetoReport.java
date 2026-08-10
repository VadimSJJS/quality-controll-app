package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParetoReport {
    private String siteName;
    private String periodFrom;
    private String periodTo;
    private String groupingType;
    private List<ParetoItem> items;
    private BigDecimal totalWeight;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParetoItem {
        private String category;
        private BigDecimal weight;
        private BigDecimal percent;
        private BigDecimal cumulativePercent; // накопительный процент
    }
}
