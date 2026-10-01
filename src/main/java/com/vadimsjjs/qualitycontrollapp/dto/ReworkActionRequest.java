package com.vadimsjjs.qualitycontrollapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Отдельное действие по доработке: дата, вид доработки, масса в тоннах, примечание.
 *
 * <p>Одно действие = одна строка. Кнопка «Добавить» в форме создаёт новую строку,
 * поэтому по одной записи можно внести несколько доработок — например, сначала
 * восстановить часть массы, позже переназначить другую часть, затем списать остаток в брак.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReworkActionRequest {

    @NotNull(message = "Дата доработки обязательна")
    private LocalDate actionDate;

    /** 1 — восстановлено, 2 — переназначено, 3 — неисправимый брак. */
    @NotNull(message = "Вид доработки обязателен")
    private Integer actionType;

    @NotNull(message = "Масса доработки обязательна")
    @Positive(message = "Масса доработки должна быть больше нуля")
    private BigDecimal weightTonnes;

    @Size(max = 200, message = "Примечание не длиннее 200 символов")
    private String note;
}