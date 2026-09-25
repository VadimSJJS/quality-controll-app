package com.vadimsjjs.qualitycontrollapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на создание/изменение вида несоответствия (справочник HLP_DEFECT_TYPE).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectTypeRequest {

    @NotBlank(message = "Название вида дефекта обязательно")
    @Size(max = 100, message = "Название не должно быть длиннее 100 символов")
    private String defectName;

    /** Код необязателен — если не задан, подставляется следующий свободный. */
    @Size(max = 20, message = "Код не должен быть длиннее 20 символов")
    private String defectCode;

    /** Исправимый (true) или неисправимый брак (false). */
    private Boolean reworkable;
}
