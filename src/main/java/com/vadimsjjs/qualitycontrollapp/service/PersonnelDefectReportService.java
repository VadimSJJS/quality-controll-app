package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.*;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/** Отчёт по дефектам персонала. */
@Service
@RequiredArgsConstructor
public class PersonnelDefectReportService {

    private final ReportQuerySupport support;
    private final com.vadimsjjs.qualitycontrollapp.repository.PersonalRepository personalRepository;
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
    public PersonnelDefectReport getPersonnelDefectReport(DefectFilterDto filter) {
        List<NonconformingProduct> allDefects = findDefects(filter, null);

        List<PersonnelDefectReport.PersonnelRow> rows = buildPersonnelRows(allDefects);

        PersonnelDefectReport.Summary summary = buildSummary("Задержанное участком", rows);

        List<NonconformingProduct> liDefects = allDefects.stream()
                .filter(d -> d.getDetectionSource() != null
                        && "ЛИ".equals(d.getDetectionSource().getSourceCode()))
                .collect(Collectors.toList());
        PersonnelDefectReport.Summary liSummary = buildDefectSummary("Задержанное на ЛИ", liDefects);

        List<NonconformingProduct> otkDefects = allDefects.stream()
                .filter(d -> d.getDetectionSource() != null
                        && "ОТК".equals(d.getDetectionSource().getSourceCode()))
                .collect(Collectors.toList());
        PersonnelDefectReport.Summary otkSummary = buildDefectSummary("Задержанное ОТК", otkDefects);

        return PersonnelDefectReport.builder()
                .periodFrom(filter.getDateFrom().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .periodTo(filter.getDateTo().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .personnelRows(rows)
                .summary(summary)
                .liSummary(liSummary)
                .otkSummary(otkSummary)
                .build();
    }

    private List<PersonnelDefectReport.PersonnelRow> buildPersonnelRows(List<NonconformingProduct> defects) {
        Map<Long, List<NonconformingProduct>> byPersonnel = defects.stream()
                .filter(d -> d.getOperatorPersonalNumber() != null)
                .collect(Collectors.groupingBy(NonconformingProduct::getOperatorPersonalNumber));

        List<PersonnelDefectReport.PersonnelRow> rows = new ArrayList<>();

        for (Map.Entry<Long, List<NonconformingProduct>> entry : byPersonnel.entrySet()) {
            Long personnelId = entry.getKey();
            List<NonconformingProduct> userDefects = entry.getValue();

            String fio = personalRepository.findByPersonalNo(personnelId)
                    .map(Personal::getFio)
                    .orElse("Неизвестно (таб. " + personnelId + ")");

            int count1 = defectAggregator.countByDefectName(userDefects, "Намот");
            int count2 = defectAggregator.countByDefectName(userDefects, "Кольцо");
            int count3 = defectAggregator.countByDefectName(userDefects, "Рев. скр.");

            rows.add(PersonnelDefectReport.PersonnelRow.builder()
                    .personnelId(personnelId)
                    .fio(fio)
                    .defectCount1(count1)
                    .defectCount2(count2)
                    .defectCount3(count3)
                    .total(count1 + count2 + count3)
                    .build());
        }

        rows.sort((a, b) -> b.getTotal().compareTo(a.getTotal()));
        return rows;
    }

    private PersonnelDefectReport.Summary buildSummary(String category, List<PersonnelDefectReport.PersonnelRow> rows) {
        int total1 = rows.stream().mapToInt(PersonnelDefectReport.PersonnelRow::getDefectCount1).sum();
        int total2 = rows.stream().mapToInt(PersonnelDefectReport.PersonnelRow::getDefectCount2).sum();
        int total3 = rows.stream().mapToInt(PersonnelDefectReport.PersonnelRow::getDefectCount3).sum();
        int grandTotal = total1 + total2 + total3;

        return PersonnelDefectReport.Summary.builder()
                .category(category)
                .defectCount1(total1)
                .defectCount2(total2)
                .defectCount3(total3)
                .total(grandTotal)
                .percent(grandTotal > 0 ? "100" : "0")
                .build();
    }

    private PersonnelDefectReport.Summary buildDefectSummary(String category, List<NonconformingProduct> defects) {
        int count1 = defectAggregator.countByDefectName(defects, "Намот");
        int count2 = defectAggregator.countByDefectName(defects, "Кольцо");
        int count3 = defectAggregator.countByDefectName(defects, "Рев. скр.");
        int total = count1 + count2 + count3;

        return PersonnelDefectReport.Summary.builder()
                .category(category)
                .defectCount1(count1)
                .defectCount2(count2)
                .defectCount3(count3)
                .total(total)
                .percent(total > 0 ? String.format("%.0f", (double) total / 100) : "0")
                .build();
    }

}
