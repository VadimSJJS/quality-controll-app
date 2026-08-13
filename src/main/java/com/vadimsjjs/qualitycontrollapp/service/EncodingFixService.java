package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.entity.*;
import com.vadimsjjs.qualitycontrollapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncodingFixService {

    private final DefectTypeRepository defectTypeRepository;
    private final DefectCauseRepository defectCauseRepository;
    private final ProductionSiteRepository productionSiteRepository;
    private final ReworkTypeRepository reworkTypeRepository;
    private final DetectionSourceRepository detectionSourceRepository;
    private final SteelGradeRepository steelGradeRepository;

    @Transactional
    public int fixAll() {
        int total = 0;
        total += fixDefectTypes();
        total += fixDefectCauses();
        total += fixReworkTypes();
        total += fixProductionSites();
        total += fixDetectionSources();
        total += fixSteelGrades();
        log.info("ИТОГО исправлено записей: {}", total);
        return total;
    }

    private int fixDefectTypes() {
        return fixRecords(defectTypeRepository.findAll(),
                DefectType::getDefectName, DefectType::setDefectName,
                HLP_DEFECT_TYPE_NAMES, "HLP_DEFECT_TYPE");
    }

    private int fixDefectCauses() {
        return fixRecords(defectCauseRepository.findAll(),
                DefectCause::getCauseName, DefectCause::setCauseName,
                HLP_DEFECT_CAUSE_NAMES, "HLP_DEFECT_CAUSE");
    }

    private int fixReworkTypes() {
        return fixRecords(reworkTypeRepository.findAll(),
                ReworkType::getReworkName, ReworkType::setReworkName,
                HLP_REWORK_TYPE_NAMES, "HLP_REWORK_TYPE");
    }

    private int fixProductionSites() {
        return fixRecords(productionSiteRepository.findAll(),
                ProductionSite::getSiteName, ProductionSite::setSiteName,
                HLP_PRODUCTION_SITE_NAMES, "HLP_PRODUCTION_SITE");
    }

    private int fixDetectionSources() {
        return fixRecords(detectionSourceRepository.findAll(),
                DetectionSource::getSourceName, DetectionSource::setSourceName,
                HLP_DETECTION_SOURCE_NAMES, "HLP_DETECTION_SOURCE");
    }

    private int fixSteelGrades() {
        return fixRecords(steelGradeRepository.findAll(),
                SteelGrade::getSteelGrade, SteelGrade::setSteelGrade,
                HLP_STEEL_GRADE_NAMES, "HLP_STEEL_GRADE");
    }

    private <T> int fixRecords(List<T> records,
                               Function<T, String> getter,
                               BiConsumer<T, String> setter,
                               Map<Long, String> correctNames,
                               String tableName) {
        int fixed = 0;
        for (T record : records) {
            Long id = recordId(record);
            String original = getter.apply(record);
            String corrected = fixEncoding(original, id, correctNames);

            if (!original.equals(corrected)) {
                setter.accept(record, corrected);
                fixed++;
                log.info("[{}] Запись #{}: '{}' -> '{}'", tableName, id, original, corrected);
            }
        }
        log.info("[{}] Обработано: {}, исправлено: {}", tableName, records.size(), fixed);
        return fixed;
    }

    private String fixEncoding(String value, Long id, Map<Long, String> knownCorrect) {
        if (value == null || value.isEmpty()) return value;

        String known = knownCorrect.get(id);
        if (known != null) {
            return known;
        }

        if (containsCyrillic(value)) return value;

        String candidate;
        try {
            candidate = new String(value.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
        if (containsCyrillic(candidate)) {
            return candidate;
        }

        candidate = new String(value.getBytes(java.nio.charset.Charset.forName("windows-1252")),
                java.nio.charset.StandardCharsets.UTF_8);
        if (containsCyrillic(candidate)) {
            return candidate;
        }

        return value;
    }

    private boolean containsCyrillic(String s) {
        return s != null && s.chars().anyMatch(c -> c >= 0x0400 && c <= 0x04FF);
    }

    private Long recordId(Object record) {
        try {
            java.lang.reflect.Method m = record.getClass().getMethod("getId");
            return (Long) m.invoke(record);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static final Map<Long, String> HLP_DEFECT_TYPE_NAMES = Map.ofEntries(
            Map.entry(7L, "Сбой в укладчике"),
            Map.entry(8L, "Дефект покрытия"),
            Map.entry(9L, "Дефект травления"),
            Map.entry(10L, "Пересорт"),
            Map.entry(11L, "Брак неисправимый"),
            Map.entry(12L, "Износ оборудования")
    );

    private static final Map<Long, String> HLP_DEFECT_CAUSE_NAMES = Map.ofEntries(
            Map.entry(3L, "Разбило чистовую волоку"),
            Map.entry(7L, "Несоответствие заготовки"),
            Map.entry(71L, "Несоответствие диаметра"),
            Map.entry(8L, "Человеческий фактор"),
            Map.entry(82L, "Неправильная сборка")
    );

    private static final Map<Long, String> HLP_REWORK_TYPE_NAMES = Map.ofEntries(
            Map.entry(1L, "Восстановлено")
    );

    private static final Map<Long, String> HLP_PRODUCTION_SITE_NAMES = Map.of(
            7L, "Участок тонкого волочения (сварочная проволока)"
    );

    private static final Map<Long, String> HLP_DETECTION_SOURCE_NAMES = Map.of(
            5L, "Возврат с последующего передела"
    );

    private static final Map<Long, String> HLP_STEEL_GRADE_NAMES = new LinkedHashMap<>();
}