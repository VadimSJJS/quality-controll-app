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
public class DefectReportResponse {
    private PeriodInfo period;
    private Summary summary;
    private List<SiteReport> siteReports;
    private List<DefectTypeReport> defectTypeReports;
    private List<CauseReport> causeReports;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodInfo {
        private String dateFrom;
        private String dateTo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalDefectWeight;
        private BigDecimal totalReworkedWeight;
        private BigDecimal totalIrreparableWeight;
        private BigDecimal defectPercent;
        private long totalRecords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SiteReport {
        private String siteName;
        private BigDecimal defectWeight;
        private BigDecimal reworkWeight;
        private BigDecimal irreparableWeight;
        private BigDecimal defectPercent;
        private BigDecimal allowablePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefectTypeReport {
        private String defectType;
        private BigDecimal weight;
        private BigDecimal percent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CauseReport {
        private String cause;
        private BigDecimal weight;
        private BigDecimal percent;
    }
}