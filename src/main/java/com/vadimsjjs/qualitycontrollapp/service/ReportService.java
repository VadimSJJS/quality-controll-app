package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.EquipmentDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.PersonnelDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.ReportDto;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionReport;
import com.vadimsjjs.qualitycontrollapp.repository.DetectionSourceRepository;
import com.vadimsjjs.qualitycontrollapp.repository.NonconformingProductRepository;
import com.vadimsjjs.qualitycontrollapp.repository.PersonalRepository;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final NonconformingProductRepository nonconformingRepository;
    private final ProductionReportRepository productionRepository;
    private final PersonalRepository personalRepository;
    private final DetectionSourceRepository detectionSourceRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Transactional(readOnly = true)
    public ReportDto.ReportBySite getReportBySite(String siteName, LocalDate dateFrom, LocalDate dateTo) {
        List<NonconformingProduct> defects = getDefectsBySite(siteName, dateFrom, dateTo);

        List<ReportDto.ReportBySite.DefectRow> rows = defects.stream()
                .collect(Collectors.groupingBy(d -> d.getDefectType().getDefectName()))
                .entrySet().stream()
                .map(entry -> {
                    String defectType = entry.getKey();
                    List<NonconformingProduct> items = entry.getValue();

                    BigDecimal total = sumWeight(items);
                    BigDecimal reworked = sumReworked(items);
                    BigDecimal defect = sumIrreparable(items);
                    String reworkType = items.stream()
                            .filter(d -> d.getReworkType() != null)
                            .findFirst()
                            .map(d -> d.getReworkType().getReworkName())
                            .orElse("");

                    return ReportDto.ReportBySite.DefectRow.builder()
                            .defectType(defectType)
                            .total(total)
                            .reworked(reworked)
                            .reworkType(reworkType)
                            .defect(defect)
                            .build();
                })
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .collect(Collectors.toList());

        BigDecimal totalAll = sumWeight(defects);
        BigDecimal reworkedAll = sumReworked(defects);
        BigDecimal defectAll = sumIrreparable(defects);

        return ReportDto.ReportBySite.builder()
                .siteName(siteName)
                .periodFrom(dateFrom.format(DATE_FORMATTER))
                .periodTo(dateTo.format(DATE_FORMATTER))
                .rows(rows)
                .totals(ReportDto.ReportBySite.Totals.builder()
                        .total(totalAll)
                        .reworked(reworkedAll)
                        .defect(defectAll)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByProductType getReportByProductType(
            String siteName, String productTypeField, LocalDate dateFrom, LocalDate dateTo) {

        List<NonconformingProduct> defects = getDefectsBySite(siteName, dateFrom, dateTo);

        Map<String, List<NonconformingProduct>> groups = defects.stream()
                .collect(Collectors.groupingBy(d -> extractProductType(d, productTypeField)));

        List<ReportDto.ReportByProductType.ProductTypeGroup> groupList = groups.entrySet().stream()
                .map(entry -> {
                    String productType = entry.getKey();
                    List<NonconformingProduct> items = entry.getValue();

                    List<ReportDto.ReportByProductType.DefectRow> rows = items.stream()
                            .collect(Collectors.groupingBy(d -> d.getDefectType().getDefectName()))
                            .entrySet().stream()
                            .map(e -> {
                                List<NonconformingProduct> defectItems = e.getValue();
                                return ReportDto.ReportByProductType.DefectRow.builder()
                                        .defectType(e.getKey())
                                        .total(sumWeight(defectItems))
                                        .reworked(sumReworked(defectItems))
                                        .reworkType(defectItems.stream()
                                                .filter(d -> d.getReworkType() != null)
                                                .findFirst()
                                                .map(d -> d.getReworkType().getReworkName())
                                                .orElse(""))
                                        .defect(sumIrreparable(defectItems))
                                        .build();
                            })
                            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                            .collect(Collectors.toList());

                    BigDecimal groupTotal = sumWeight(items);
                    BigDecimal groupReworked = sumReworked(items);
                    BigDecimal groupDefect = sumIrreparable(items);

                    return ReportDto.ReportByProductType.ProductTypeGroup.builder()
                            .productType(productType)
                            .rows(rows)
                            .groupTotals(ReportDto.ReportByProductType.Totals.builder()
                                    .total(groupTotal)
                                    .reworked(groupReworked)
                                    .defect(groupDefect)
                                    .build())
                            .build();
                })
                .sorted((a, b) -> b.getGroupTotals().getTotal().compareTo(a.getGroupTotals().getTotal()))
                .collect(Collectors.toList());

        BigDecimal totalAll = sumWeight(defects);
        BigDecimal reworkedAll = sumReworked(defects);
        BigDecimal defectAll = sumIrreparable(defects);

        return ReportDto.ReportByProductType.builder()
                .siteName(siteName)
                .periodFrom(dateFrom.format(DATE_FORMATTER))
                .periodTo(dateTo.format(DATE_FORMATTER))
                .productTypeField(productTypeField)
                .groups(groupList)
                .totals(ReportDto.ReportByProductType.Totals.builder()
                        .total(totalAll)
                        .reworked(reworkedAll)
                        .defect(defectAll)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByProductAndCause getReportByProductAndCause(
            String siteName, String productTypeField, LocalDate dateFrom, LocalDate dateTo) {

        List<NonconformingProduct> defects = getDefectsBySite(siteName, dateFrom, dateTo);

        Map<String, List<NonconformingProduct>> groups = defects.stream()
                .collect(Collectors.groupingBy(d -> extractProductType(d, productTypeField)));

        List<ReportDto.ReportByProductAndCause.CauseGroup> groupList = groups.entrySet().stream()
                .map(entry -> {
                    String productType = entry.getKey();
                    List<NonconformingProduct> items = entry.getValue();

                    List<ReportDto.ReportByProductAndCause.DefectCauseRow> rows = items.stream()
                            .map(d -> ReportDto.ReportByProductAndCause.DefectCauseRow.builder()
                                    .defectType(d.getDefectType().getDefectName())
                                    .total(d.getWeightTonnes())
                                    .cause(d.getDefectCause() != null ? d.getDefectCause().getCauseName() : "")
                                    .subcause(d.getDefectSubcause() != null ? d.getDefectSubcause().getCauseName() : "")
                                    .note(d.getNote())
                                    .build())
                            .collect(Collectors.toList());

                    BigDecimal groupTotal = sumWeight(items);

                    return ReportDto.ReportByProductAndCause.CauseGroup.builder()
                            .productType(productType)
                            .rows(rows)
                            .groupTotals(ReportDto.ReportByProductAndCause.Totals.builder()
                                    .total(groupTotal)
                                    .build())
                            .build();
                })
                .sorted((a, b) -> b.getGroupTotals().getTotal().compareTo(a.getGroupTotals().getTotal()))
                .collect(Collectors.toList());

        return ReportDto.ReportByProductAndCause.builder()
                .siteName(siteName)
                .periodFrom(dateFrom.format(DATE_FORMATTER))
                .periodTo(dateTo.format(DATE_FORMATTER))
                .productTypeField(productTypeField)
                .groups(groupList)
                .totals(ReportDto.ReportByProductAndCause.Totals.builder()
                        .total(sumWeight(defects))
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByBrigade getReportByBrigade(
            String siteName, Long brigadeId, LocalDate dateFrom, LocalDate dateTo) {

        List<NonconformingProduct> defects = getDefectsBySiteAndBrigade(siteName, brigadeId, dateFrom, dateTo);

        Map<String, List<NonconformingProduct>> groups = defects.stream()
                .collect(Collectors.groupingBy(d -> extractDiameter(d)));

        List<ReportDto.ReportByBrigade.ProductTypeGroup> groupList = groups.entrySet().stream()
                .map(entry -> {
                    String productType = entry.getKey();
                    List<NonconformingProduct> items = entry.getValue();

                    List<ReportDto.ReportByBrigade.DefectRow> rows = items.stream()
                            .collect(Collectors.groupingBy(d -> d.getDefectType().getDefectName()))
                            .entrySet().stream()
                            .map(e -> {
                                List<NonconformingProduct> defectItems = e.getValue();
                                return ReportDto.ReportByBrigade.DefectRow.builder()
                                        .defectType(e.getKey())
                                        .total(sumWeight(defectItems))
                                        .reworked(sumReworked(defectItems))
                                        .reworkType(defectItems.stream()
                                                .filter(d -> d.getReworkType() != null)
                                                .findFirst()
                                                .map(d -> d.getReworkType().getReworkName())
                                                .orElse(""))
                                        .defect(sumIrreparable(defectItems))
                                        .build();
                            })
                            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                            .collect(Collectors.toList());

                    BigDecimal groupTotal = sumWeight(items);
                    BigDecimal groupReworked = sumReworked(items);
                    BigDecimal groupDefect = sumIrreparable(items);

                    return ReportDto.ReportByBrigade.ProductTypeGroup.builder()
                            .productType(productType)
                            .rows(rows)
                            .groupTotals(ReportDto.ReportByBrigade.Totals.builder()
                                    .total(groupTotal)
                                    .reworked(groupReworked)
                                    .defect(groupDefect)
                                    .build())
                            .build();
                })
                .sorted((a, b) -> b.getGroupTotals().getTotal().compareTo(a.getGroupTotals().getTotal()))
                .collect(Collectors.toList());

        BigDecimal totalAll = sumWeight(defects);
        BigDecimal reworkedAll = sumReworked(defects);
        BigDecimal defectAll = sumIrreparable(defects);

        return ReportDto.ReportByBrigade.builder()
                .siteName(siteName)
                .periodFrom(dateFrom.format(DATE_FORMATTER))
                .periodTo(dateTo.format(DATE_FORMATTER))
                .brigadeId(brigadeId)
                .groups(groupList)
                .totals(ReportDto.ReportByBrigade.Totals.builder()
                        .total(totalAll)
                        .reworked(reworkedAll)
                        .defect(defectAll)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByEquipment getReportByEquipment(
            String siteName, LocalDate dateFrom, LocalDate dateTo) {

        List<NonconformingProduct> defects = getDefectsBySite(siteName, dateFrom, dateTo);

        Map<String, List<NonconformingProduct>> groups = defects.stream()
                .filter(d -> d.getEquipmentKey() != null)
                .collect(Collectors.groupingBy(NonconformingProduct::getEquipmentKey));

        List<ReportDto.ReportByEquipment.EquipmentGroup> groupList = groups.entrySet().stream()
                .map(entry -> {
                    String equipment = entry.getKey();
                    List<NonconformingProduct> items = entry.getValue();

                    List<ReportDto.ReportByEquipment.DefectRow> rows = items.stream()
                            .collect(Collectors.groupingBy(d -> d.getDefectType().getDefectName()))
                            .entrySet().stream()
                            .map(e -> {
                                List<NonconformingProduct> defectItems = e.getValue();
                                return ReportDto.ReportByEquipment.DefectRow.builder()
                                        .defectType(e.getKey())
                                        .total(sumWeight(defectItems))
                                        .reworked(sumReworked(defectItems))
                                        .reworkType(defectItems.stream()
                                                .filter(d -> d.getReworkType() != null)
                                                .findFirst()
                                                .map(d -> d.getReworkType().getReworkName())
                                                .orElse(""))
                                        .defect(sumIrreparable(defectItems))
                                        .build();
                            })
                            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                            .collect(Collectors.toList());

                    BigDecimal groupTotal = sumWeight(items);
                    BigDecimal groupReworked = sumReworked(items);
                    BigDecimal groupDefect = sumIrreparable(items);

                    return ReportDto.ReportByEquipment.EquipmentGroup.builder()
                            .equipment(equipment)
                            .rows(rows)
                            .groupTotals(ReportDto.ReportByEquipment.Totals.builder()
                                    .total(groupTotal)
                                    .reworked(groupReworked)
                                    .defect(groupDefect)
                                    .build())
                            .build();
                })
                .sorted((a, b) -> b.getGroupTotals().getTotal().compareTo(a.getGroupTotals().getTotal()))
                .collect(Collectors.toList());

        BigDecimal totalAll = sumWeight(defects);
        BigDecimal reworkedAll = sumReworked(defects);
        BigDecimal defectAll = sumIrreparable(defects);

        return ReportDto.ReportByEquipment.builder()
                .siteName(siteName)
                .periodFrom(dateFrom.format(DATE_FORMATTER))
                .periodTo(dateTo.format(DATE_FORMATTER))
                .groups(groupList)
                .totals(ReportDto.ReportByEquipment.Totals.builder()
                        .total(totalAll)
                        .reworked(reworkedAll)
                        .defect(defectAll)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public PersonnelDefectReport getPersonnelDefectReportV2(LocalDate dateFrom, LocalDate dateTo) {
        List<NonconformingProduct> defects = nonconformingRepository.findWithFilters(dateFrom, dateTo, null, null);

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

            int count1 = countByDefectName(userDefects, "Намот");
            int count2 = countByDefectName(userDefects, "Кольцо");
            int count3 = countByDefectName(userDefects, "Рев. скр.");
            int total = count1 + count2 + count3;
            int defect = (int) userDefects.stream()
                    .filter(d -> d.getIrreparableWeightTonnes() != null && d.getIrreparableWeightTonnes().compareTo(BigDecimal.ZERO) > 0)
                    .count();

            String note = userDefects.stream()
                    .findFirst()
                    .map(NonconformingProduct::getNote)
                    .orElse("");

            rows.add(PersonnelDefectReport.PersonnelRow.builder()
                    .personnelId(personnelId)
                    .fio(fio)
                    .defectCount1(count1)
                    .defectCount2(count2)
                    .defectCount3(count3)
                    .total(total)
                    .defect(defect)
                    .note(note)
                    .build());
        }

        rows.sort((a, b) -> b.getTotal().compareTo(a.getTotal()));

        int total1 = rows.stream().mapToInt(PersonnelDefectReport.PersonnelRow::getDefectCount1).sum();
        int total2 = rows.stream().mapToInt(PersonnelDefectReport.PersonnelRow::getDefectCount2).sum();
        int total3 = rows.stream().mapToInt(PersonnelDefectReport.PersonnelRow::getDefectCount3).sum();
        int grandTotal = total1 + total2 + total3;
        int grandDefect = rows.stream().mapToInt(PersonnelDefectReport.PersonnelRow::getDefect).sum();

        PersonnelDefectReport.Summary summary = PersonnelDefectReport.Summary.builder()
                .category("Итого по участку")
                .defectCount1(total1)
                .defectCount2(total2)
                .defectCount3(total3)
                .total(grandTotal)
                .defect(grandDefect)
                .note("")
                .build();

        return PersonnelDefectReport.builder()
                .periodFrom(dateFrom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .periodTo(dateTo.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .personnelRows(rows)
                .summary(summary)
                .build();
    }

    @Transactional(readOnly = true)
    public PersonnelDefectReport getPersonnelDefectReport(LocalDate dateFrom, LocalDate dateTo) {
        List<NonconformingProduct> allDefects = nonconformingRepository.findWithFilters(dateFrom, dateTo, null, null);

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
                .periodFrom(dateFrom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .periodTo(dateTo.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
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

            int count1 = countByDefectName(userDefects, "Намот");
            int count2 = countByDefectName(userDefects, "Кольцо");
            int count3 = countByDefectName(userDefects, "Рев. скр.");

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
        int count1 = countByDefectName(defects, "Намот");
        int count2 = countByDefectName(defects, "Кольцо");
        int count3 = countByDefectName(defects, "Рев. скр.");
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

    private int countByDefectName(List<NonconformingProduct> defects, String defectName) {
        return (int) defects.stream()
                .filter(d -> d.getDefectType() != null
                        && defectName.equals(d.getDefectType().getDefectName()))
                .count();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByPersonnel getReportByPersonnel(
            String siteName, LocalDate dateFrom, LocalDate dateTo) {

        List<NonconformingProduct> defects = getDefectsBySite(siteName, dateFrom, dateTo);

        Map<String, List<NonconformingProduct>> groups = defects.stream()
                .filter(d -> d.getOperatorPersonalNumber() != null)
                .collect(Collectors.groupingBy(d -> String.valueOf(d.getOperatorPersonalNumber())));

        List<ReportDto.ReportByPersonnel.PersonnelGroup> groupList = groups.entrySet().stream()
                .map(entry -> {
                    String personnelNumber = entry.getKey();
                    List<NonconformingProduct> items = entry.getValue();

                    List<ReportDto.ReportByPersonnel.DefectRow> rows = items.stream()
                            .collect(Collectors.groupingBy(d -> d.getDefectType().getDefectName()))
                            .entrySet().stream()
                            .map(e -> {
                                List<NonconformingProduct> defectItems = e.getValue();
                                return ReportDto.ReportByPersonnel.DefectRow.builder()
                                        .defectType(e.getKey())
                                        .total(sumWeight(defectItems))
                                        .defect(sumIrreparable(defectItems))
                                        .note(defectItems.stream()
                                                .findFirst()
                                                .map(NonconformingProduct::getNote)
                                                .orElse(""))
                                        .build();
                            })
                            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                            .collect(Collectors.toList());

                    BigDecimal groupTotal = sumWeight(items);
                    BigDecimal groupDefect = sumIrreparable(items);

                    return ReportDto.ReportByPersonnel.PersonnelGroup.builder()
                            .personnelNumber(personnelNumber)
                            .rows(rows)
                            .groupTotals(ReportDto.ReportByPersonnel.Totals.builder()
                                    .total(groupTotal)
                                    .defect(groupDefect)
                                    .build())
                            .build();
                })
                .sorted((a, b) -> b.getGroupTotals().getTotal().compareTo(a.getGroupTotals().getTotal()))
                .collect(Collectors.toList());

        BigDecimal totalAll = sumWeight(defects);
        BigDecimal defectAll = sumIrreparable(defects);

        return ReportDto.ReportByPersonnel.builder()
                .siteName(siteName)
                .periodFrom(dateFrom.format(DATE_FORMATTER))
                .periodTo(dateTo.format(DATE_FORMATTER))
                .groups(groupList)
                .totals(ReportDto.ReportByPersonnel.Totals.builder()
                        .total(totalAll)
                        .defect(defectAll)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByPlant getReportByPlant(LocalDate dateFrom, LocalDate dateTo) {
        List<NonconformingProduct> allDefects = nonconformingRepository.findWithFilters(dateFrom, dateTo, null, null);
        List<ProductionReport> productions = productionRepository.findByReportDateBetween(dateFrom, dateTo);

        Map<String, List<NonconformingProduct>> defectsBySite = allDefects.stream()
                .collect(Collectors.groupingBy(d -> d.getProductionSite().getSiteName()));

        Map<String, BigDecimal> producedBySite = productions.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getProductionSite().getSiteName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                ProductionReport::getProducedWeightTonnes,
                                BigDecimal::add)
                ));

        List<ReportDto.ReportByPlant.SiteRow> rows = new ArrayList<>();

        for (Map.Entry<String, List<NonconformingProduct>> entry : defectsBySite.entrySet()) {
            String siteName = entry.getKey();
            List<NonconformingProduct> defectList = entry.getValue();

            BigDecimal defectWeight = sumWeight(defectList);
            BigDecimal produced = producedBySite.getOrDefault(siteName, BigDecimal.ZERO);
            BigDecimal percent = calcPercent(defectWeight, produced);

            BigDecimal allowable = defectList.stream()
                    .findFirst()
                    .map(d -> d.getProductionSite().getAllowableDefectPercent())
                    .orElse(BigDecimal.ZERO);

            rows.add(ReportDto.ReportByPlant.SiteRow.builder()
                    .siteName(siteName)
                    .produced(produced)
                    .nonconforming(defectWeight)
                    .nonconformingPercent(percent)
                    .allowablePercent(allowable)
                    .type("total")
                    .build());
        }

        BigDecimal totalProduced = sumProduced(productions);
        BigDecimal totalDefect = sumWeight(allDefects);
        BigDecimal totalPercent = calcPercent(totalDefect, totalProduced);

        return ReportDto.ReportByPlant.builder()
                .periodFrom(dateFrom.format(DATE_FORMATTER))
                .periodTo(dateTo.format(DATE_FORMATTER))
                .rows(rows)
                .totals(ReportDto.ReportByPlant.Totals.builder()
                        .totalProduced(totalProduced)
                        .totalNonconforming(totalDefect)
                        .totalNonconformingPercent(totalPercent)
                        .totalAllowable(BigDecimal.valueOf(0.55)) // 0.55% из ТЗ
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByFault getReportByFault(LocalDate dateFrom, LocalDate dateTo) {
        List<NonconformingProduct> allDefects = nonconformingRepository.findWithFilters(dateFrom, dateTo, null, null);
        List<ProductionReport> productions = productionRepository.findByReportDateBetween(dateFrom, dateTo);

        List<ReportDto.ReportByFault.FaultRow> rows = new ArrayList<>();

        addFaultRow(rows, "Итого по СтПЦ-2", null, allDefects, productions);

        List<NonconformingProduct> byFault = allDefects.stream()
                .filter(d -> d.getDefectCause() != null && d.getDefectCause().getCauseName() != null)
                .collect(Collectors.toList());
        addFaultRow(rows, "Несоответствующая по вине цеха", null, byFault, productions);

        List<NonconformingProduct> reworkable = allDefects.stream()
                .filter(d -> d.getDefectType().getReworkable() != null && d.getDefectType().getReworkable())
                .collect(Collectors.toList());
        addFaultRow(rows, "Несоответствующая исправимая, в т.ч", null, reworkable, productions);

        Map<String, List<NonconformingProduct>> reworkableBySite = reworkable.stream()
                .collect(Collectors.groupingBy(d -> d.getProductionSite().getSiteName()));

        for (Map.Entry<String, List<NonconformingProduct>> entry : reworkableBySite.entrySet()) {
            addFaultRow(rows, entry.getKey(), "reworkable", entry.getValue(), productions);
        }

        List<NonconformingProduct> irreparable = allDefects.stream()
                .filter(d -> d.getIrreparableWeightTonnes() != null
                        && d.getIrreparableWeightTonnes().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        addFaultRow(rows, "Несоответствующая неисправимая всего, в т.ч", null, irreparable, productions);

        Map<String, List<NonconformingProduct>> irreparableBySite = irreparable.stream()
                .collect(Collectors.groupingBy(d -> d.getProductionSite().getSiteName()));

        for (Map.Entry<String, List<NonconformingProduct>> entry : irreparableBySite.entrySet()) {
            addFaultRow(rows, entry.getKey(), "irreparable", entry.getValue(), productions);
        }

        BigDecimal totalReworked = sumReworked(allDefects);
        rows.add(ReportDto.ReportByFault.FaultRow.builder()
                .category("Восстановлено")
                .siteName(null)
                .produced(null)
                .nonconforming(totalReworked)
                .nonconformingPercent(null)
                .allowablePercent(null)
                .build());

        return ReportDto.ReportByFault.builder()
                .periodFrom(dateFrom.format(DATE_FORMATTER))
                .periodTo(dateTo.format(DATE_FORMATTER))
                .rows(rows)
                .totals(ReportDto.ReportByFault.Totals.builder()
                        .totalProduced(sumProduced(productions))
                        .totalNonconforming(sumWeight(allDefects))
                        .build())
                .build();
    }

    private List<NonconformingProduct> getDefectsBySite(String siteName, LocalDate dateFrom, LocalDate dateTo) {
        return nonconformingRepository.findWithFilters(dateFrom, dateTo, null, null).stream()
                .filter(d -> d.getProductionSite().getSiteName().equals(siteName))
                .collect(Collectors.toList());
    }

    private List<NonconformingProduct> getDefectsBySiteAndBrigade(String siteName, Long brigadeId, LocalDate dateFrom, LocalDate dateTo) {
        return getDefectsBySite(siteName, dateFrom, dateTo).stream()
                .filter(d -> d.getManufacturerBrigade() != null && d.getManufacturerBrigade().equals(brigadeId))
                .collect(Collectors.toList());
    }


    private String extractProductType(NonconformingProduct d, String field) {
        if ("диаметр".equals(field) && d.getDiameter() != null) {
            return String.valueOf(d.getDiameter());
        }
        if ("конструкция".equals(field) && d.getSteelCordConstruction() != null) {
            return d.getSteelCordConstruction();
        }
        return "не определено";
    }

    private String extractDiameter(NonconformingProduct d) {
        return d.getDiameter() != null ? String.valueOf(d.getDiameter()) : "не определено";
    }

    private BigDecimal sumWeight(List<NonconformingProduct> items) {
        return items.stream()
                .map(NonconformingProduct::getWeightTonnes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumReworked(List<NonconformingProduct> items) {
        return items.stream()
                .filter(d -> d.getReworkDate() != null)
                .map(d -> d.getReworkWeightTonnes() != null ? d.getReworkWeightTonnes() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumIrreparable(List<NonconformingProduct> items) {
        return items.stream()
                .filter(d -> d.getIrreparableWeightTonnes() != null)
                .map(NonconformingProduct::getIrreparableWeightTonnes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumProduced(List<ProductionReport> items) {
        return items.stream()
                .map(ProductionReport::getProducedWeightTonnes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcPercent(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return part.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private void addFaultRow(List<ReportDto.ReportByFault.FaultRow> rows, String category,
                             String type, List<NonconformingProduct> defects,
                             List<ProductionReport> productions) {
        if (defects.isEmpty()) {
            rows.add(ReportDto.ReportByFault.FaultRow.builder()
                    .category(category)
                    .siteName(null)
                    .produced(BigDecimal.ZERO)
                    .nonconforming(BigDecimal.ZERO)
                    .nonconformingPercent(BigDecimal.ZERO)
                    .allowablePercent(type != null ? BigDecimal.valueOf(0.01) : BigDecimal.ZERO)
                    .build());
            return;
        }

        boolean isByFault = "reworkable".equals(type) || "irreparable".equals(type);

        BigDecimal defectWeight = sumWeight(defects);
        BigDecimal produced = BigDecimal.ZERO;

        if (isByFault) {
            String siteName = defects.get(0).getProductionSite().getSiteName();
            produced = productions.stream()
                    .filter(p -> p.getProductionSite().getSiteName().equals(siteName))
                    .map(ProductionReport::getProducedWeightTonnes)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal percent = calcPercent(defectWeight, produced);
        BigDecimal allowable = BigDecimal.ZERO;

        if (isByFault) {
            allowable = defects.get(0).getProductionSite().getAllowableDefectPercent();
        }

        rows.add(ReportDto.ReportByFault.FaultRow.builder()
                .category(category)
                .siteName(defects.get(0).getProductionSite().getSiteName())
                .produced(produced)
                .nonconforming(defectWeight)
                .nonconformingPercent(percent)
                .allowablePercent(allowable)
                .build());
    }

    @Transactional(readOnly = true)
    public EquipmentDefectReport getEquipmentDefectReport(LocalDate dateFrom, LocalDate dateTo) {
        List<NonconformingProduct> allDefects = nonconformingRepository.findWithFilters(dateFrom, dateTo, null, null);

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
                .periodFrom(dateFrom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .periodTo(dateTo.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .equipmentRows(rows)
                .summary(summary)
                .liSummary(liSummary)
                .otkSummary(otkSummary)
                .build();
    }


    private List<EquipmentDefectReport.EquipmentRow> buildEquipmentRows(List<NonconformingProduct> defects) {
        Map<String, List<NonconformingProduct>> byEquipment = defects.stream()
                .filter(d -> d.getEquipmentKey() != null && !d.getEquipmentKey().isEmpty())
                .collect(Collectors.groupingBy(NonconformingProduct::getEquipmentKey));

        List<EquipmentDefectReport.EquipmentRow> rows = new ArrayList<>();

        for (Map.Entry<String, List<NonconformingProduct>> entry : byEquipment.entrySet()) {
            String equipmentNumber = entry.getKey();
            List<NonconformingProduct> equipDefects = entry.getValue();

            int count1 = countByDefectName(equipDefects, "Намот");
            int count2 = countByDefectName(equipDefects, "Кольцо");
            int count3 = countByDefectName(equipDefects, "Рез. скручивание");

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
        int count1 = countByDefectName(defects, "Намот");
        int count2 = countByDefectName(defects, "Кольцо");
        int count3 = countByDefectName(defects, "Рез. скручивание");
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