package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class ReportDto {

    // Отчет по участку
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportBySite {
        private String siteName;
        private String periodFrom;
        private String periodTo;
        private List<DefectRow> rows;
        private Totals totals;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DefectRow {
            private String defectType;
            private BigDecimal total;
            private BigDecimal reworked;
            private String reworkType;
            private BigDecimal defect;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Totals {
            private BigDecimal total;
            private BigDecimal reworked;
            private BigDecimal defect;
        }
    }

    // ===== отчет по участку по виду продукции
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByProductType {
        private String siteName;
        private String periodFrom;
        private String periodTo;
        private String productTypeField;
        private List<ProductTypeGroup> groups;
        private Totals totals;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProductTypeGroup {
            private String productType;
            private List<DefectRow> rows;
            private Totals groupTotals;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DefectRow {
            private String defectType;
            private BigDecimal total;
            private BigDecimal reworked;
            private String reworkType;
            private BigDecimal defect;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Totals {
            private BigDecimal total;
            private BigDecimal reworked;
            private BigDecimal defect;
            private BigDecimal reworkedTotal;
            private BigDecimal reworkedDefect;
        }
    }

    // Отчет по участкам по виду продукци и причинам
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByProductAndCause {
        private String siteName;
        private String periodFrom;
        private String periodTo;
        private String productTypeField;
        private List<CauseGroup> groups;
        private Totals totals;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CauseGroup {
            private String productType;
            private List<DefectCauseRow> rows;
            private Totals groupTotals;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DefectCauseRow {
            private String defectType;
            private BigDecimal total;
            private String cause;
            private String subcause;
            private String note;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Totals {
            private BigDecimal total;
        }
    }

    // отчет по участку бригаде
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByBrigade {
        private String siteName;
        private String periodFrom;
        private String periodTo;
        private Long brigadeId;
        private List<ProductTypeGroup> groups;
        private Totals totals;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProductTypeGroup {
            private String productType;
            private List<DefectRow> rows;
            private Totals groupTotals;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DefectRow {
            private String defectType;
            private BigDecimal total;
            private BigDecimal reworked;
            private String reworkType;
            private BigDecimal defect;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Totals {
            private BigDecimal total;
            private BigDecimal reworked;
            private BigDecimal defect;
        }
    }

    // ===== отчет по участку оборудованию
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByEquipment {
        private String siteName;
        private String periodFrom;
        private String periodTo;
        private List<EquipmentGroup> groups;
        private Totals totals;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EquipmentGroup {
            private String equipment;
            private List<DefectRow> rows;
            private Totals groupTotals;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DefectRow {
            private String defectType;
            private BigDecimal total;
            private BigDecimal reworked;
            private String reworkType;
            private BigDecimal defect;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Totals {
            private BigDecimal total;
            private BigDecimal reworked;
            private BigDecimal defect;
        }
    }

    // отчет по участку по персоналу
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByPersonnel {
        private String siteName;
        private String periodFrom;
        private String periodTo;
        private List<PersonnelGroup> groups;
        private Totals totals;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PersonnelGroup {
            private String personnelNumber;
            private List<DefectRow> rows;
            private Totals groupTotals;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DefectRow {
            private String defectType;
            private BigDecimal total;
            private BigDecimal defect;
            private String note;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Totals {
            private BigDecimal total;
            private BigDecimal defect;
        }
    }

    // отчет по цеху сводный
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByPlant {
        private String periodFrom;
        private String periodTo;
        private List<SiteRow> rows;
        private Totals totals;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SiteRow {
            private String siteName;
            private BigDecimal produced;
            private BigDecimal nonconforming;
            private BigDecimal nonconformingPercent;
            private BigDecimal allowablePercent;
            private String type; // "total", "reworkable", "irreparable", "by_fault"
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Totals {
            private BigDecimal totalProduced;
            private BigDecimal totalNonconforming;
            private BigDecimal totalNonconformingPercent;
            private BigDecimal totalAllowable;
        }
    }

    // ===== отчет по цеху по вине
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByFault {
        private String periodFrom;
        private String periodTo;
        private List<FaultRow> rows;
        private Totals totals;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FaultRow {
            private String category;
            private String siteName;
            private BigDecimal produced;
            private BigDecimal nonconforming;
            private BigDecimal nonconformingPercent;
            private BigDecimal allowablePercent;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Totals {
            private BigDecimal totalProduced;
            private BigDecimal totalNonconforming;
        }
    }
}