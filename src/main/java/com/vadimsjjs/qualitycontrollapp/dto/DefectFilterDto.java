package com.vadimsjjs.qualitycontrollapp.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectFilterDto {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    private Long productionSiteId;

    private Long defectTypeId;

    private Long defectCauseId;

    private Long defectSubcauseId;

    private Long diameterId;

    private String steelCordConstruction;

    private Long productCode;

    private String heatNumber;

    private String steelGrade;

    private String equipmentKey;

    private Long operatorPersonalNumber;

    private Long manufacturerBrigade;

    @AssertTrue(message = "Дата «с» должна быть не позже даты «по»")
    public boolean isDateRangeValid() {
        return dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo);
    }
}
