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
public class PersonnelDefectReport {
    private String periodFrom;
    private String periodTo;
    private List<PersonnelRow> personnelRows;
    private Summary summary;           // Задержанное участком
    private Summary liSummary;         // Задержанное на ЛИ
    private Summary otkSummary;        // Задержанное ОТК

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonnelRow {
        private Long personnelId;
        private String fio;
        private Integer defectCount1;   // Намот
        private Integer defectCount2;   // Кольцо
        private Integer defectCount3;   // Рев. скр.
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