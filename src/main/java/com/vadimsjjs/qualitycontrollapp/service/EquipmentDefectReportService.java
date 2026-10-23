package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.dto.EquipmentDefectReport;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/** Отчёт по дефектам оборудования. */
@Service
@RequiredArgsConstructor
public class EquipmentDefectReportService {

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
    public EquipmentDefectReport getEquipmentDefectReport(DefectFilterDto filter) {
        List<NonconformingProduct> allDefects = findDefects(filter, null);

        List<EquipmentDefectReport.EquipmentRow> rows = buildEquipmentRows(allDefects);

        EquipmentDefectReport.Summary summary = buildEquipmentSummary("Задержано участком", rows);

        List<NonconformingProduct> liDefects = allDefects.stream()
                .filter(d -> d.getDetectionSource() != null
                        && "ЛИ".equals(d.getDetectionSource().getSourceCode()))
                .collect(Collectors.toList());
        EquipmentDefectReport.Summary liSummary = buildDefectSummaryEquipment("Задержано на ЛИ", liDefects);

        List<NonconformingProduct> otkDefects = allDefects.stream()
                .filter(d -> d.getDetectionSource() != null
                        && "ОТК".equals(d.getDetectionSource().getSourceCode()))
                .collect(Collectors.toList());
        EquipmentDefectReport.Summary otkSummary = buildDefectSummaryEquipment("Задержано ОТК", otkDefects);

        return EquipmentDefectReport.builder()
                .periodFrom(filter.getDateFrom().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .periodTo(filter.getDateTo().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .equipmentRows(rows)
                .summary(summary)
                .liSummary(liSummary)
                .otkSummary(otkSummary)
                .build();
    }

    private List<EquipmentDefectReport.EquipmentRow> buildEquipmentRows(List<NonconformingProduct> defects) {
        Map<String, List<NonconformingProduct>> byEquipment = defects.stream()
                .filter(d -> d.resolveEquipment() != null && !d.resolveEquipment().isEmpty())
                .collect(Collectors.groupingBy(NonconformingProduct::resolveEquipment));

        List<EquipmentDefectReport.EquipmentRow> rows = new ArrayList<>();

        for (Map.Entry<String, List<NonconformingProduct>> entry : byEquipment.entrySet()) {
            String equipmentNumber = entry.getKey();
            List<NonconformingProduct> equipDefects = entry.getValue();

            int count1 = defectAggregator.countByDefectName(equipDefects, "Намот");
            int count2 = defectAggregator.countByDefectName(equipDefects, "Кольцо");
            int count3 = defectAggregator.countByDefectName(equipDefects, "Рез. скручивание");

            rows.add(EquipmentDefectReport.EquipmentRow.builder()
                    .equipmentNumber(equipmentNumber)
                    .defectCount1(count1)
                    .defectCount2(count2)
                    .defectCount3(count3)
                    .total(count1 + count2 + count3)
                    .build());
        }

        rows.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(a.getEquipmentNumber()),
                        Integer.parseInt(b.getEquipmentNumber()));
            } catch (NumberFormatException e) {
                return a.getEquipmentNumber().compareTo(b.getEquipmentNumber());
            }
        });

        return rows;
    }

    private EquipmentDefectReport.Summary buildEquipmentSummary(String category,
                                                                List<EquipmentDefectReport.EquipmentRow> rows) {
        int total1 = rows.stream().mapToInt(EquipmentDefectReport.EquipmentRow::getDefectCount1).sum();
        int total2 = rows.stream().mapToInt(EquipmentDefectReport.EquipmentRow::getDefectCount2).sum();
        int total3 = rows.stream().mapToInt(EquipmentDefectReport.EquipmentRow::getDefectCount3).sum();
        int grandTotal = total1 + total2 + total3;

        return EquipmentDefectReport.Summary.builder()
                .category(category)
                .defectCount1(total1)
                .defectCount2(total2)
                .defectCount3(total3)
                .total(grandTotal)
                .percent(grandTotal > 0 ? "100" : "0")
                .build();
    }

    private EquipmentDefectReport.Summary buildDefectSummaryEquipment(String category,
                                                                      List<NonconformingProduct> defects) {
        int count1 = defectAggregator.countByDefectName(defects, "Намот");
        int count2 = defectAggregator.countByDefectName(defects, "Кольцо");
        int count3 = defectAggregator.countByDefectName(defects, "Рез. скручивание");
        int total = count1 + count2 + count3;

        return EquipmentDefectReport.Summary.builder()
                .category(category)
                .defectCount1(count1)
                .defectCount2(count2)
                .defectCount3(count3)
                .total(total)
                .percent(total > 0 ? String.format("%.0f", (double) total / 100) : "0")
                .build();
    }

}
