package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.DistributionReport;
import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.function.Function;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/** Распределение несоответствий по оборудованию и бригадам. */
@Service
@RequiredArgsConstructor
public class DistributionReportService {

    private final ReportQuerySupport support;
    private final ProductionReportRepository productionRepository;
    private final DefectAggregator defectAggregator;

    private static final DateTimeFormatter DATE_FORMATTER = ReportQuerySupport.DATE_FORMATTER;

    private List<NonconformingProduct> findDefects(DefectFilterDto filter, Long forcedSiteId) {
        return support.findDefects(filter, forcedSiteId);
    }

    private ProductionSite getProductionSiteByCode(String siteCode) {
        return support.getProductionSiteByCode(siteCode);
    }

    private static String normalizeSiteCode(String siteCode) {
        return ReportQuerySupport.normalizeSiteCode(siteCode);
    }

    @Transactional(readOnly = true)
    public DistributionReport getDistributionReport(String dimension, DefectFilterDto filter) {
        if (!"equipment".equals(dimension) && !"brigade".equals(dimension)) {
            throw new IllegalArgumentException("Поддерживаются только группировки equipment и brigade");
        }
        if (filter.getDateFrom() == null || filter.getDateTo() == null) {
            throw new IllegalArgumentException("Укажите период отчёта");
        }

        List<NonconformingProduct> defects = findDefects(filter, null).stream()
                .filter(d -> d.getDefectType() != null)
                .filter(d -> "equipment".equals(dimension)
                        ? d.resolveEquipment() != null && !d.resolveEquipment().isBlank()
                        : d.getManufacturerBrigade() != null)
                .toList();

        List<String> defectTypes = defects.stream()
                .map(d -> d.getDefectType().getDefectName())
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        Function<NonconformingProduct, String> keyExtractor = "equipment".equals(dimension)
                ? NonconformingProduct::resolveEquipment
                : d -> String.valueOf(d.getManufacturerBrigade());

        Map<String, List<NonconformingProduct>> groups = defects.stream()
                .collect(Collectors.groupingBy(keyExtractor));

        Comparator<String> keyComparator = (left, right) -> {
            long leftNumber = parseSortNumber(left);
            long rightNumber = parseSortNumber(right);
            int numeric = Long.compare(leftNumber, rightNumber);
            return numeric != 0 ? numeric : left.compareToIgnoreCase(right);
        };

        List<DistributionReport.Row> rows = groups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(keyComparator))
                .map(entry -> {
                    Map<String, BigDecimal> byDefect = entry.getValue().stream()
                            .collect(Collectors.groupingBy(
                                    d -> d.getDefectType().getDefectName(),
                                    Collectors.reducing(BigDecimal.ZERO,
                                            NonconformingProduct::getWeightTonnes,
                                            BigDecimal::add)));
                    defectTypes.forEach(type -> byDefect.putIfAbsent(type, BigDecimal.ZERO));
                    return DistributionReport.Row.builder()
                            .key(entry.getKey())
                            .total(defectAggregator.sumWeight(entry.getValue()))
                            .byDefect(byDefect)
                            .build();
                })
                .toList();

        return DistributionReport.builder()
                .dimension(dimension)
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
                .defectTypes(defectTypes)
                .rows(rows)
                .totalWeight(defectAggregator.sumWeight(defects))
                .build();
    }

    private long parseSortNumber(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return Long.MAX_VALUE;
        }
    }

}
