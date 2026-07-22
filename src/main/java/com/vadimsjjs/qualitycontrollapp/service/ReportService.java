package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.DefectReportResponse;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.repository.NonconformingProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final NonconformingProductRepository repository;

    public DefectReportResponse getDefectReport(LocalDate dateFrom, LocalDate dateTo) {
        List<NonconformingProduct> defects = repository.findWithFilters(dateFrom, dateTo, null, null);

        // Сводка
        BigDecimal totalWeight = sumWeight(defects);
        BigDecimal totalReworked = sumReworked(defects);
        BigDecimal totalIrreparable = sumIrreparable(defects);
        long totalRecords = defects.size();

        // Отчёты
        List<DefectReportResponse.SiteReport> siteReports = buildSiteReports(defects, totalWeight);
        List<DefectReportResponse.DefectTypeReport> defectTypeReports = buildDefectTypeReports(defects, totalWeight);
        List<DefectReportResponse.CauseReport> causeReports = buildCauseReports(defects, totalWeight);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        return DefectReportResponse.builder()
                .period(DefectReportResponse.PeriodInfo.builder()
                        .dateFrom(dateFrom.format(formatter))
                        .dateTo(dateTo.format(formatter))
                        .build())
                .summary(DefectReportResponse.Summary.builder()
                        .totalDefectWeight(totalWeight)
                        .totalReworkedWeight(totalReworked)
                        .totalIrreparableWeight(totalIrreparable)
                        .totalRecords(totalRecords)
                        .build())
                .siteReports(siteReports)
                .defectTypeReports(defectTypeReports)
                .causeReports(causeReports)
                .build();
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    private BigDecimal sumWeight(List<NonconformingProduct> defects) {
        return defects.stream()
                .map(NonconformingProduct::getWeightTonnes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumReworked(List<NonconformingProduct> defects) {
        return defects.stream()
                .filter(d -> d.getReworkDate() != null)
                .map(d -> d.getReworkWeightTonnes() != null ? d.getReworkWeightTonnes() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumIrreparable(List<NonconformingProduct> defects) {
        return defects.stream()
                .filter(d -> d.getIrreparableWeightTonnes() != null)
                .map(NonconformingProduct::getIrreparableWeightTonnes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePercent(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return part.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private <T> Map<String, BigDecimal> groupByKey(List<NonconformingProduct> defects,
                                                   Function<NonconformingProduct, String> keyExtractor,
                                                   Function<NonconformingProduct, BigDecimal> valueExtractor) {
        return defects.stream()
                .filter(d -> keyExtractor.apply(d) != null)
                .collect(Collectors.groupingBy(
                        keyExtractor,
                        Collectors.reducing(BigDecimal.ZERO,
                                valueExtractor,
                                BigDecimal::add)
                ));
    }

    private List<DefectReportResponse.SiteReport> buildSiteReports(List<NonconformingProduct> defects, BigDecimal totalWeight) {
        Map<String, BigDecimal> weightBySite = groupByKey(defects,
                d -> d.getProductionSite().getSiteName(),
                NonconformingProduct::getWeightTonnes);

        Map<String, BigDecimal> reworkBySite = groupByKey(defects.stream()
                        .filter(d -> d.getReworkDate() != null)
                        .collect(Collectors.toList()),
                d -> d.getProductionSite().getSiteName(),
                d -> d.getReworkWeightTonnes() != null ? d.getReworkWeightTonnes() : BigDecimal.ZERO);

        Map<String, BigDecimal> irreparableBySite = groupByKey(defects.stream()
                        .filter(d -> d.getIrreparableWeightTonnes() != null)
                        .collect(Collectors.toList()),
                d -> d.getProductionSite().getSiteName(),
                NonconformingProduct::getIrreparableWeightTonnes);

        return weightBySite.keySet().stream()
                .map(siteName -> {
                    BigDecimal weight = weightBySite.getOrDefault(siteName, BigDecimal.ZERO);
                    BigDecimal rework = reworkBySite.getOrDefault(siteName, BigDecimal.ZERO);
                    BigDecimal irreparable = irreparableBySite.getOrDefault(siteName, BigDecimal.ZERO);
                    BigDecimal percent = calculatePercent(weight, totalWeight);

                    // Находим допустимый процент (берём из первой записи по этому участку)
                    BigDecimal allowable = defects.stream()
                            .filter(d -> d.getProductionSite().getSiteName().equals(siteName))
                            .findFirst()
                            .map(d -> d.getProductionSite().getAllowableDefectPercent())
                            .orElse(null);

                    return DefectReportResponse.SiteReport.builder()
                            .siteName(siteName)
                            .defectWeight(weight)
                            .reworkWeight(rework)
                            .irreparableWeight(irreparable)
                            .defectPercent(percent)
                            .allowablePercent(allowable)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<DefectReportResponse.DefectTypeReport> buildDefectTypeReports(List<NonconformingProduct> defects, BigDecimal totalWeight) {
        Map<String, BigDecimal> weightByType = groupByKey(defects,
                d -> d.getDefectType().getDefectName(),
                NonconformingProduct::getWeightTonnes);

        return weightByType.entrySet().stream()
                .map(entry -> DefectReportResponse.DefectTypeReport.builder()
                        .defectType(entry.getKey())
                        .weight(entry.getValue())
                        .percent(calculatePercent(entry.getValue(), totalWeight))
                        .build())
                .collect(Collectors.toList());
    }

    private List<DefectReportResponse.CauseReport> buildCauseReports(List<NonconformingProduct> defects, BigDecimal totalWeight) {
        Map<String, BigDecimal> weightByCause = groupByKey(defects,
                d -> d.getDefectCause() != null ? d.getDefectCause().getCauseName() : null,
                NonconformingProduct::getWeightTonnes);

        return weightByCause.entrySet().stream()
                .map(entry -> DefectReportResponse.CauseReport.builder()
                        .cause(entry.getKey())
                        .weight(entry.getValue())
                        .percent(calculatePercent(entry.getValue(), totalWeight))
                        .build())
                .collect(Collectors.toList());
    }
}