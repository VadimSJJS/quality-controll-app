package com.vadimsjjs.qualitycontrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Одно действие по доработке в ответе API: дата, вид, масса, примечание. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReworkActionResponse {

    private Long id;
    private LocalDate actionDate;

    /** 1 — восстановлено, 2 — переназначено, 3 — неисправимый брак. */
    private Integer actionType;

    /** Название вида доработки из справочника — для показа в таблице. */
    private String actionTypeName;

    private BigDecimal weightTonnes;
    private String note;
}