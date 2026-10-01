package com.vadimsjjs.qualitycontrollapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Запрос на регистрацию несоответствующей продукции.
 *
 * <p>Лишние поля, пришедшие от клиента, игнорируются: форма отправляет разные
 * наборы полей в зависимости от участка, и лишнее поле не должно ломать сохранение
 * (ранее это давало ошибку «Unrecognized field ... not marked as ignorable»).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NonconformingProductRequest {

    @NotNull(message = "Дата выявления обязательна")
    private LocalDate detectionDate;

    @NotNull(message = "Участок обязателен")
    private Long productionSiteId;

    @NotNull(message = "Источник выявления обязателен")
    private Long detectionSourceId;

    @NotNull(message = "Вес обязателен")
    @Positive(message = "Вес должен быть положительным")
    private BigDecimal weightTonnes;

    @PositiveOrZero(message = "Вес неисправимого брака должен быть ≥ 0")
    private BigDecimal irreparableWeightTonnes;

    /**
     * Идентификатор вида несоответствия из справочника.
     * Можно не указывать, если передан defectTypeName.
     */
    private Long defectTypeId;

    /**
     * Название вида несоответствия, введённое пользователем.
     * Если такого названия нет в справочнике — оно автоматически добавляется
     * в справочник HLP_DEFECT_TYPE, чтобы отчёты и диаграммы Парето работали.
     */
    @jakarta.validation.constraints.Size(max = 100, message = "Название дефекта не длиннее 100 символов")
    private String defectTypeName;

    private Long defectCauseId;
    private Long defectSubcauseId;

    /** Причина, введённая вручную. Если такой причины нет в справочнике — она создаётся. */
    @jakarta.validation.constraints.Size(max = 100, message = "Название причины не длиннее 100 символов")
    private String defectCauseName;

    /** Подпричина, введённая вручную. Требует выбранной или введённой причины. */
    @jakarta.validation.constraints.Size(max = 100, message = "Название подпричины не длиннее 100 символов")
    private String defectSubcauseName;
    private String note;
    private Long productCode;
    private String reelNumber;
    private String heatNumber;
    private Long manufacturerBrigade;
    private String bundleNumber;
    private String manufacturerWorkshop;
    private String equipmentKey;
    private Long diameterId;

    /**
     * Табельный номер оператора, который работал на участке (4–6 цифр).
     * Вводится вручную: справочник персонала пользователям не доступен.
     */
    @Min(value = 1000, message = "Табельный номер оператора должен содержать от 4 до 6 цифр")
    @Max(value = 999999, message = "Табельный номер оператора должен содержать от 4 до 6 цифр")
    private Long operatorPersonalNumber;

    private LocalDate reworkDate;
    private Long reworkTypeId;

    /**
     * Результаты доработки в тоннах. Задержанную продукцию можно распределить
     * между тремя исходами: восстановлено, переназначено и неисправимый брак.
     */
    @PositiveOrZero(message = "Восстановлено, т должно быть ≥ 0")
    private BigDecimal restoredWeightTonnes;

    @PositiveOrZero(message = "Переназначено, т должно быть ≥ 0")
    private BigDecimal reassignedWeightTonnes;

    @PositiveOrZero(message = "Брак, т должен быть ≥ 0")
    private BigDecimal scrappedWeightTonnes;

    /** Проверка: сумма результатов доработки не может превышать задержанную массу. */
    @jakarta.validation.constraints.AssertTrue(message = "Сумма восстановлено + переназначено + брак не должна превышать задержанную массу")
    public boolean isReworkSumValid() {
        BigDecimal restored = nz(restoredWeightTonnes);
        BigDecimal reassigned = nz(reassignedWeightTonnes);
        BigDecimal scrapped = nz(scrappedWeightTonnes);
        BigDecimal total = restored.add(reassigned).add(scrapped);
        if (total.signum() == 0) {
            return true;
        }
        return weightTonnes != null && total.compareTo(weightTonnes) <= 0;
    }

    /**
     * Отдельные действия по доработке этой записи: дата, вид, масса, примечание.
     * Пустой список — доработок ещё не было.
     */
    @Valid
    private List<ReworkActionRequest> reworkActions;

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long steelGradeId;
    private String unitNumber;
    private Integer quantity;
    private Integer reworkQuantity;
    private String workpieceKey;
    private String steelCordConstruction;
    private Long brigade;
}