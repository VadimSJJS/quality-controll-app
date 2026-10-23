package com.vadimsjjs.qualitycontrollapp.enums;

import java.util.Arrays;

/**
 * Результат действия по доработке задержанной продукции (значения колонки
 * {@code REWORK_ACTION.ACTION_TYPE}).
 *
 * <p>Коды зафиксированы в БД: 1 — восстановлено, 2 — переназначено,
 * 3 — неисправимый брак. Единый источник истины для сервисов, импорта
 * и отчётов — чтобы не дублировать магические числа.
 */
public enum ReworkActionType {

    RESTORED(1, "Восстановлено"),
    REASSIGNED(2, "Переназначено"),
    SCRAPPED(3, "Неисправимый брак");

    private final int code;
    private final String label;

    ReworkActionType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ReworkActionType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.code == code)
                .findFirst()
                .orElse(null);
    }
}
