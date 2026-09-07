package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class ReportDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportBySite {
        private String siteName;
        private String periodFrom;
        private String periodTo;
        private BigDecimal producedWeight;
        private BigDecimal allowablePercent;
        private boolean exceedsAllowable;
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
            private BigDecimal defectPercent;
            private boolean exceedsAllowable;
        }
    }

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
            private String type;
            private boolean exceedsAllowable;
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
            private boolean exceedsAllowable;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportByAct {
        private String periodFrom;
        private String periodTo;
        private BigDecimal producedWeight;
        private List<ActGroup> groups;
        private Totals totals;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ActGroup {
            private String actNumber;
            private String documentType;
            private String siteName;
            private int count;
            private List<DefectRow> rows;
            private Totals groupTotals;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DefectRow {
            private String defectType;
            private String cause;
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
            private BigDecimal defectPercent;
            private int count;
        }
    }
}