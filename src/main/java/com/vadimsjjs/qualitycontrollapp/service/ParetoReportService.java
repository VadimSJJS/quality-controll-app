package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.dto.ParetoReport;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/** Диаграмма Парето. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParetoReportService {

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
    public ParetoReport getParetoReport(String siteCode, String groupingType,
                                        DefectFilterDto filter) {

        ProductionSite site = getProductionSiteByCode(siteCode);
        log.info("=== getParetoReport START: siteCode='{}', groupingType='{}', dateFrom={}, dateTo={} ===",
                siteCode, groupingType, filter.getDateFrom(), filter.getDateTo());
        
        List<NonconformingProduct> defects = findDefects(filter, site.getId());
        log.info("  defects.size() = {}", defects.size());

        List<ParetoReport.ParetoItem> items;

        if ("defect".equals(groupingType)) {
            Map<String, BigDecimal> defectMap = defects.stream()
                    .collect(Collectors.groupingBy(
                            d -> d.getDefectType() != null ? d.getDefectType().getDefectName() : "Не указан",
                            Collectors.reducing(BigDecimal.ZERO,
                                    NonconformingProduct::getWeightTonnes,
                                    BigDecimal::add)
                    ));
            
            log.info("  defectMap.size() = {}", defectMap.size());
            defectMap.forEach((k, v) -> log.info("    defect: '{}' = {}", k, v));

            items = defectMap.entrySet().stream()
                    .map(e -> ParetoReport.ParetoItem.builder()
                            .category(e.getKey())
                            .weight(e.getValue())
                            .build())
                    .sorted((a, b) -> b.getWeight().compareTo(a.getWeight()))
                    .collect(Collectors.toList());
        } else {
            Map<String, BigDecimal> causeMap = defects.stream()
                    .filter(d -> d.getDefectCause() != null && d.getDefectCause().getCauseName() != null)
                    .collect(Collectors.groupingBy(
                            d -> d.getDefectCause().getCauseName(),
                            Collectors.reducing(BigDecimal.ZERO,
                                    NonconformingProduct::getWeightTonnes,
                                    BigDecimal::add)
                    ));

            items = causeMap.entrySet().stream()
                    .map(e -> ParetoReport.ParetoItem.builder()
                            .category(e.getKey())
                            .weight(e.getValue())
                            .build())
                    .sorted((a, b) -> b.getWeight().compareTo(a.getWeight()))
                    .collect(Collectors.toList());
        }

        BigDecimal totalWeight = defectAggregator.sumWeight(defects);
        log.info("  totalWeight = {}", totalWeight);
        log.info("  items.size() = {}", items.size());
        log.info("=== getParetoReport END ===");

        BigDecimal cumulative = BigDecimal.ZERO;
        for (ParetoReport.ParetoItem item : items) {
            BigDecimal percent = totalWeight.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : item.getWeight().divide(totalWeight, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

            cumulative = cumulative.add(percent);
            item.setPercent(percent);
            item.setCumulativePercent(cumulative);
        }

        return ParetoReport.builder()
                .siteName(site.getSiteName())
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
                .groupingType(groupingType)
                .items(items)
                .totalWeight(totalWeight)
                .build();
    }

}
