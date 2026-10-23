package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionReport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Чистые функции агрегации и группировки для отчётов; вынесены из {@link ReportService},
 * чтобы логику расчётов можно было тестировать изолированно без загрузки всего сервиса.
 */
@Component
public class DefectAggregator {

    /** Суммарная масса записей (в тоннах). */
    public BigDecimal sumWeight(List<NonconformingProduct> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(NonconformingProduct::getWeightTonnes)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Суммарная масса неисправимого брака (в тоннах). */
    public BigDecimal sumIrreparable(List<NonconformingProduct> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .filter(d -> d.getIrreparableWeightTonnes() != null)
                .map(NonconformingProduct::getIrreparableWeightTonnes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Суммарная масса доработанной продукции (в тоннах). */
    public BigDecimal sumReworked(List<NonconformingProduct> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .filter(d -> d.getReworkDate() != null)
                .map(d -> d.getReworkWeightTonnes() != null ? d.getReworkWeightTonnes() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Суммарное производство (в тоннах) по отчётам выпуска. */
    public BigDecimal sumProduced(List<ProductionReport> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(ProductionReport::getProducedWeightTonnes)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Процент part/total. Возвращает 0, если total == 0 или null.
     * Округление до 4 знака после запятой (HALF_UP).
     */
    public BigDecimal calcPercent(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (part == null) {
            part = BigDecimal.ZERO;
        }
        return part.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Подсчёт записей с именем вида дефекта, равным заданному.
     */
    public int countByDefectName(List<NonconformingProduct> defects, String defectName) {
        if (defects == null || defectName == null) {
            return 0;
        }
        return (int) defects.stream()
                .filter(d -> d.getDefectType() != null
                        && defectName.equals(d.getDefectType().getDefectName()))
                .count();
    }

    /**
     * Масса записи за вычетом неисправимого брака (исправимая часть).
     * Отрицательный результат обнуляется.
     */
    public BigDecimal reworkableWeight(NonconformingProduct d) {
        if (d == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal weight = d.getWeightTonnes() != null ? d.getWeightTonnes() : BigDecimal.ZERO;
        BigDecimal irreparable = d.getIrreparableWeightTonnes() != null ? d.getIrreparableWeightTonnes() : BigDecimal.ZERO;
        BigDecimal result = weight.subtract(irreparable);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }
}
