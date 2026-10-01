package com.vadimsjjs.qualitycontrollapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Отдельное действие по доработке задержанной продукции.
 *
 * <p>Запись о несоответствии может дорабатываться не один раз: часть восстановлена,
 * другая переназначена, третья признана неисправимым браком, позже по каждой части
 * выполняются новые действия. Каждое такое действие хранится отдельной строкой здесь —
 * это даёт прослеживаемость: что, когда и в каком объёме сделано с каждой катушкой.
 *
 * <p>Сумма масс всех действий не может превышать задержанную массу — проверяется
 * в сервисе при сохранении.
 */
@Entity
@Table(name = "REWORK_ACTION")
@Getter
@Setter
@NoArgsConstructor
public class ReworkAction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rework_action_seq")
    @SequenceGenerator(name = "rework_action_seq", sequenceName = "SEQ_REWORK_ACTION", allocationSize = 1)
    @Column(name = "ID_REWORK_ACTION", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_NONCONFORMING_PRODUCT", nullable = false)
    private NonconformingProduct nonconformingProduct;

    /** Дата выполнения действия. */
    @Column(name = "ACTION_DATE", nullable = false)
    private LocalDate actionDate;

    /** Вид доработки: 1 — восстановлено, 2 — переназначено, 3 — неисправимый брак. */
    @Column(name = "ACTION_TYPE", nullable = false)
    private Integer actionType;

    /** Объём продукции по этому действию, т. */
    @Column(name = "WEIGHT_TONNES", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightTonnes;

    /** Примечание к действию, вводится вручную. */
    @Column(name = "NOTE", length = 200)
    private String note;
}