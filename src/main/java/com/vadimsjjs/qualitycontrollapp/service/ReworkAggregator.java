package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.ReworkAction;
import com.vadimsjjs.qualitycontrollapp.enums.ReworkActionType;
import com.vadimsjjs.qualitycontrollapp.repository.ReworkTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Единый расчёт итогов доработки по действиям записи.
 *
 * <p>Действия по доработке — это что, когда и в каком объёме восстановлено,
 * переназначено или списано в брак. Сводные поля записи (восстановлено, переназначено,
 * доработано, неисправимый брак, дата и вид доработки) пересчитываются из них здесь,
 * поэтому веб-форма и импорт из Excel приводят запись к одинаковому виду и отчёты
 * считают одинаково независимо от способа ввода.
 */
@Component
@RequiredArgsConstructor
public class ReworkAggregator {

    private final ReworkTypeRepository reworkTypeRepository;

    /**
     * Пересчитывает сводные поля записи по её действиям.
     *
     */
    public void recomputeTotals(NonconformingProduct entity) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal restored = BigDecimal.ZERO;
        BigDecimal reassigned = BigDecimal.ZERO;
        BigDecimal scrapped = BigDecimal.ZERO;
        LocalDate lastDate = null;
        ReworkAction latestAction = null;

        for (ReworkAction action : entity.getReworkActions()) {
            if (action.getActionDate() == null || action.getWeightTonnes() == null
                    || action.getWeightTonnes().signum() <= 0) {
                continue;
            }
            BigDecimal weight = action.getWeightTonnes();
            total = total.add(weight);
            int type = action.getActionType() == null ? 0 : action.getActionType();
            if (type == ReworkActionType.RESTORED.getCode()) {
                restored = restored.add(weight);
            } else if (type == ReworkActionType.REASSIGNED.getCode()) {
                reassigned = reassigned.add(weight);
            } else if (type == ReworkActionType.SCRAPPED.getCode()) {
                scrapped = scrapped.add(weight);
            }
            if (lastDate == null || action.getActionDate().isAfter(lastDate)) {
                lastDate = action.getActionDate();
            }
            if (latestAction == null || !action.getActionDate().isBefore(latestAction.getActionDate())) {
                latestAction = action;
            }
        }

        if (total.signum() == 0) {
            return;
        }
        if (entity.getWeightTonnes() != null && total.compareTo(entity.getWeightTonnes()) > 0) {
            throw new RuntimeException("Сумма доработок ("
                    + total.stripTrailingZeros().toPlainString()
                    + " т) превышает задержанную массу ("
                    + entity.getWeightTonnes().stripTrailingZeros().toPlainString() + " т)");
        }

        // Поле «Вид доработки» в форме убрано — выводим вид записи из последнего действия.
        if (latestAction != null && latestAction.getActionType() != null) {
            reworkTypeRepository.findById(latestAction.getActionType().longValue())
                    .ifPresent(entity::setReworkType);
        }

        entity.setRestoredWeightTonnes(restored.signum() == 0 ? null : restored);
        entity.setReassignedWeightTonnes(reassigned.signum() == 0 ? null : reassigned);
        entity.setReworkWeightTonnes(restored.add(reassigned));
        entity.setIrreparableWeightTonnes(scrapped.signum() == 0 ? null : scrapped);
        if (lastDate != null) {
            entity.setReworkDate(lastDate);
        }
    }
}
