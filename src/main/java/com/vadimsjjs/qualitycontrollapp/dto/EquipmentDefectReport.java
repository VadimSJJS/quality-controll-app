package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDefectReport {
    private String periodFrom;
    private String periodTo;
    private List<EquipmentRow> equipmentRows;
    private Summary summary;           // Задержано участком
    private Summary liSummary;         // Задержано на ЛИ
    private Summary otkSummary;        // Задержано ОТК

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquipmentRow {
        private String equipmentNumber;
        private Integer defectCount1;   // Намот
        private Integer defectCount2;   // Кольцо
        private Integer defectCount3;   // Рез. скручивание
        private Integer total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private String category;
        private Integer defectCount1;
        private Integer defectCount2;
        private Integer defectCount3;
        private Integer total;
        private String percent;
    }
}