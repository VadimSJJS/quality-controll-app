package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.dto.EquipmentDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.ParetoReport;
import com.vadimsjjs.qualitycontrollapp.dto.PersonnelDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.ReportDto;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionReport;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.DetectionSourceRepository;
import com.vadimsjjs.qualitycontrollapp.repository.NonconformingProductRepository;
import com.vadimsjjs.qualitycontrollapp.repository.PersonalRepository;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionReportRepository;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionSiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ProductionSiteRepository productionSiteRepository;
    private final DetectionSourceRepository detectionSourceRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final BigDecimal PLANT_ALLOWABLE_TOTAL = new BigDecimal("0.55");

    private static final BigDecimal PLANT_ALLOWABLE_IRREPARABLE_TOTAL = new BigDecimal("0.04");

    private static final Map<String, BigDecimal> SITE_ALLOWABLE_IRREPARABLE = Map.of(
            "ГСВ", new BigDecimal("0.00"),
            "ТТГУ", new BigDecimal("0.01"),
            "УТВ-МК", new BigDecimal("0.01"),
            "УТВ-РМЛ", new BigDecimal("0.01"),
            "КУ-1", new BigDecimal("0.07"),
            "КУ-2", new BigDecimal("0.07")
    );

    private ProductionSite getProductionSiteByCode(String siteCode) {
        return productionSiteRepository.findBySiteCode(siteCode)
                .orElseThrow(() -> new IllegalArgumentException("Участок с кодом '" + siteCode + "' не найден"));
    }

    private List<NonconformingProduct> findDefects(DefectFilterDto filter, Long forcedSiteId) {
        Long siteId = forcedSiteId != null ? forcedSiteId : filter.getProductionSiteId();
        return nonconformingRepository.findWithFilter(
                filter.getDateFrom(),
                filter.getDateTo(),
                siteId,
                filter.getDefectTypeId(),
                filter.getDefectCauseId(),
                filter.getDefectSubcauseId(),
                filter.getDiameterId(),
                filter.getSteelCordConstruction(),
                filter.getProductCode(),
                filter.getHeatNumber(),
                filter.getSteelGrade(),
                filter.getEquipmentKey(),
                filter.getOperatorPersonalNumber(),
                filter.getManufacturerBrigade());
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportBySite getReportBySite(String siteCode, DefectFilterDto filter) {
        ProductionSite site = getProductionSiteByCode(siteCode);
        List<NonconformingProduct> defects = findDefects(filter, site.getId());

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

        BigDecimal producedWeight = BigDecimal.ZERO;
        BigDecimal allowablePercent = BigDecimal.ZERO;
        boolean exceedsAllowable = false;

        if (!defects.isEmpty()) {
            NonconformingProduct sample = defects.get(0);
            Long siteId = sample.getProductionSite().getId();
            producedWeight = productionRepository.sumProducedBySiteAndDateRange(filter.getDateFrom(), filter.getDateTo(), siteId);
            allowablePercent = site.getAllowableDefectPercent() != null
                    ? site.getAllowableDefectPercent()
                    : BigDecimal.ZERO;
            exceedsAllowable = calcPercent(totalAll, producedWeight).compareTo(allowablePercent) > 0;
        }

        BigDecimal defectPercent = calcPercent(totalAll, producedWeight);

        return ReportDto.ReportBySite.builder()
                .siteName(site.getSiteName())
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
                .producedWeight(producedWeight)
                .allowablePercent(allowablePercent)
                .exceedsAllowable(exceedsAllowable)
                .rows(rows)
                .totals(ReportDto.ReportBySite.Totals.builder()
                        .total(totalAll)
                        .reworked(reworkedAll)
                        .defect(defectAll)
                        .defectPercent(defectPercent)
                        .exceedsAllowable(exceedsAllowable)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByProductType getReportByProductType(
            String siteCode, String productTypeField, DefectFilterDto filter) {

        ProductionSite site = getProductionSiteByCode(siteCode);
        List<NonconformingProduct> defects = findDefects(filter, site.getId());

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
                .siteName(site.getSiteName())
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
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
            String siteCode, String productTypeField, DefectFilterDto filter) {

        ProductionSite site = getProductionSiteByCode(siteCode);
        List<NonconformingProduct> defects = findDefects(filter, site.getId());

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
                .siteName(site.getSiteName())
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
                .productTypeField(productTypeField)
                .groups(groupList)
                .totals(ReportDto.ReportByProductAndCause.Totals.builder()
                        .total(sumWeight(defects))
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByBrigade getReportByBrigade(
            String siteCode, Long brigadeId, DefectFilterDto filter) {

        filter.setManufacturerBrigade(brigadeId);
        ProductionSite site = getProductionSiteByCode(siteCode);
        List<NonconformingProduct> defects = findDefects(filter, site.getId());

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
                .siteName(site.getSiteName())
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
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
            String siteCode, DefectFilterDto filter) {

        ProductionSite site = getProductionSiteByCode(siteCode);
        List<NonconformingProduct> defects = findDefects(filter, site.getId());

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
                .siteName(site.getSiteName())
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
                .groups(groupList)
                .totals(ReportDto.ReportByEquipment.Totals.builder()
                        .total(totalAll)
                        .reworked(reworkedAll)
                        .defect(defectAll)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public PersonnelDefectReport getPersonnelDefectReportV2(DefectFilterDto filter) {
        List<NonconformingProduct> defects = findDefects(filter, null);

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
                .periodFrom(filter.getDateFrom().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .periodTo(filter.getDateTo().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .personnelRows(rows)
                .summary(summary)
                .build();
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
            String siteCode, DefectFilterDto filter) {

        ProductionSite site = getProductionSiteByCode(siteCode);
        List<NonconformingProduct> defects = findDefects(filter, site.getId());

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
                .siteName(site.getSiteName())
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
                .groups(groupList)
                .totals(ReportDto.ReportByPersonnel.Totals.builder()
                        .total(totalAll)
                        .defect(defectAll)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByPlant getReportByPlant(DefectFilterDto filter) {
        List<NonconformingProduct> allDefects = findDefects(filter, null);
        List<ProductionReport> productions = productionRepository.findByReportDateBetween(filter.getDateFrom(), filter.getDateTo());

        Map<String, BigDecimal> producedBySite = productions.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getProductionSite().getSiteName(),
                        Collectors.reducing(BigDecimal.ZERO, ProductionReport::getProducedWeightTonnes, BigDecimal::add)));
        BigDecimal totalProduced = sumProduced(productions);
        BigDecimal totalDefect = sumWeight(allDefects);
        BigDecimal totalIrreparable = sumIrreparable(allDefects);
        BigDecimal totalReworkable = totalDefect.subtract(totalIrreparable);
        BigDecimal totalReworked = sumReworked(allDefects);

        Map<String, BigDecimal> reworkableAllowableBySiteName = productionSiteRepository.findAll().stream()
                .collect(Collectors.toMap(ProductionSite::getSiteName,
                        s -> s.getAllowableDefectPercent() != null ? s.getAllowableDefectPercent() : BigDecimal.ZERO));
        Map<String, String> siteCodeByName = productionSiteRepository.findAll().stream()
                .collect(Collectors.toMap(ProductionSite::getSiteName, ProductionSite::getSiteCode));

        List<NonconformingProduct> plantFaultDefects= allDefects.stream()
                .filter(this::isPlantFault)
                .collect(Collectors.toList());
        BigDecimal plantFaultDefect= sumWeight(plantFaultDefects);
        BigDecimal plantFaultIrreparable = sumIrreparable(plantFaultDefects);

        BigDecimal externalDefect= allDefects.stream()
                .filter(d -> !isPlantFault(d))
                .filter(this::isExternalReturn)
                .map(d -> d.getIrreparableWeightTonnes() != null ? d.getIrreparableWeightTonnes() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ncpDefect= allDefects.stream()
                .filter(d -> !isPlantFault(d))
                .filter(d -> !isExternalReturn(d))
                .map(d -> d.getIrreparableWeightTonnes() != null ? d.getIrreparableWeightTonnes() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ReportDto.ReportByPlant.SiteRow> rows = new ArrayList<>();
        rows.add(plantRow("Итого по СтПЦ-2", totalProduced, totalDefect, calcPercent(totalDefect, totalProduced), PLANT_ALLOWABLE_TOTAL, "total"));
        rows.add(plantRow("Несоответствующая по вине цеха", totalProduced, plantFaultDefect, calcPercent(plantFaultDefect, totalProduced), PLANT_ALLOWABLE_TOTAL, "by_fault"));
        rows.add(plantRow("Несоответствующая исправимая, в т.ч.", totalProduced, totalReworkable, calcPercent(totalReworkable, totalProduced), PLANT_ALLOWABLE_TOTAL, "reworkable_total"));
        addPlantSiteRows(rows, "reworkable_site", allDefects, producedBySite, reworkableAllowableBySiteName, siteCodeByName, true);

        rows.add(plantRow("Несоответствующая неисправимая всего, в т.ч.", totalProduced, totalIrreparable, calcPercent(totalIrreparable, totalProduced), PLANT_ALLOWABLE_IRREPARABLE_TOTAL, "irreparable_total"));
        rows.add(plantRow("Несоответствующая неисправимая по вине цеха", totalProduced, plantFaultIrreparable, calcPercent(plantFaultIrreparable, totalProduced), PLANT_ALLOWABLE_IRREPARABLE_TOTAL, "irreparable_by_fault"));
        addPlantSiteRows(rows, "irreparable_site", allDefects, producedBySite, reworkableAllowableBySiteName, siteCodeByName, false);

        rows.add(plantRow("НЦП", totalProduced, ncpDefect, calcPercent(ncpDefect, totalProduced), BigDecimal.ZERO, "ncp"));
        rows.add(plantRow("Внешний брак", totalProduced, externalDefect, calcPercent(externalDefect, totalProduced), BigDecimal.ZERO, "external"));

        rows.add(ReportDto.ReportByPlant.SiteRow.builder()
                .siteName("Восстановлено")
                .produced(null)
                .nonconforming(totalReworked)
                .nonconformingPercent(null)
                .allowablePercent(null)
                .type("reworked")
                .exceedsAllowable(false)
                .build());

        return ReportDto.ReportByPlant.builder()
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
                .rows(rows)
                .totals(ReportDto.ReportByPlant.Totals.builder()
                        .totalProduced(totalProduced)
                        .totalNonconforming(totalDefect)
                        .totalNonconformingPercent(calcPercent(totalDefect, totalProduced))
                        .totalAllowable(PLANT_ALLOWABLE_TOTAL)
                        .build())
                .build();
    }
    private ReportDto.ReportByPlant.SiteRow plantRow(String name, BigDecimal produced, BigDecimal nonconf,
                                                          BigDecimal percent, BigDecimal allowable, String type) {
        boolean exceeds = percent != null && allowable != null && percent.compareTo(allowable) > 0;
        return ReportDto.ReportByPlant.SiteRow.builder()
                .siteName(name)
                .produced(produced)
                .nonconforming(nonconf)
                .nonconformingPercent(percent)
                .allowablePercent(allowable)
                .type(type)
                .exceedsAllowable(exceeds)
                .build();
    }


    private void addPlantSiteRows(List<ReportDto.ReportByPlant.SiteRow> rows, String type,
                                    List<NonconformingProduct> allDefects, Map<String, BigDecimal> producedBySite,
                                    Map<String, BigDecimal> reworkableAllowableBySiteName, Map<String, String> siteCodeByName,
                                    boolean reworkable) {
        allDefects.stream()
                .collect(Collectors.groupingBy(d -> d.getProductionSite().getSiteName()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String siteName = entry.getKey();
                    List<NonconformingProduct> defs = entry.getValue();
                    BigDecimal mass;
                    if (reworkable) {
                        mass = defs.stream().map(this::reworkableWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
                    } else {
                        mass = sumIrreparable(defs);
                    }
                    if (mass.compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    }
                    BigDecimal produced = producedBySite.getOrDefault(siteName, BigDecimal.ZERO);
                    BigDecimal allowable = reworkable
                            ? reworkableAllowableBySiteName.getOrDefault(siteName, BigDecimal.ZERO)
                            : SITE_ALLOWABLE_IRREPARABLE.getOrDefault(siteCodeByName.get(siteName), BigDecimal.ZERO);
                    BigDecimal percent = calcPercent(mass, produced);
                    rows.add(ReportDto.ReportByPlant.SiteRow.builder()
                            .siteName(siteName)
                            .produced(produced)
                            .nonconforming(mass)
                            .nonconformingPercent(percent)
                            .allowablePercent(allowable)
                            .type(type)
                            .exceedsAllowable(percent.compareTo(allowable) > 0)
                            .build());
                });
    }
    private BigDecimal reworkableWeight(NonconformingProduct d) {
        BigDecimal weight = d.getWeightTonnes() != null ? d.getWeightTonnes() : BigDecimal.ZERO;
        BigDecimal irreparable = d.getIrreparableWeightTonnes() != null ? d.getIrreparableWeightTonnes() : BigDecimal.ZERO;
        BigDecimal result = weight.subtract(irreparable);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    private boolean isPlantFault(NonconformingProduct d) {
        String workshop = d.getManufacturerWorkshop();
        return workshop == null || workshop.isBlank() || workshop.toLowerCase().contains("стпц");
    }

    private boolean isExternalReturn(NonconformingProduct d) {
        return d.getDetectionSource() != null
                && d.getDetectionSource().getSourceName() != null
                && d.getDetectionSource().getSourceName().toLowerCase().contains("внешн");
    }
    @Transactional(readOnly = true)
    public ReportDto.ReportByFault getReportByFault(DefectFilterDto filter) {
        List<NonconformingProduct> allDefects = findDefects(filter, null);
        List<ProductionReport> productions = productionRepository.findByReportDateBetween(filter.getDateFrom(), filter.getDateTo());

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
                .periodFrom(filter.getDateFrom().format(DATE_FORMATTER))
                .periodTo(filter.getDateTo().format(DATE_FORMATTER))
                .rows(rows)
                .totals(ReportDto.ReportByFault.Totals.builder()
                        .totalProduced(sumProduced(productions))
                        .totalNonconforming(sumWeight(allDefects))
                        .build())
                                .build();
    }


    @Transactional(readOnly = true)
    public ReportDto.ReportByAct getReportByActs(DefectFilterDto filter) {
        List<NonconformingProduct> defects = findDefects(filter, null);
        List<ProductionReport> productions =
                productionRepository.findByReportDateBetween(filter.getDateFrom(), filter.getDateTo());
        BigDecimal producedWeight = sumProduced(productions);

        Map<String, List<NonconformingProduct>> byAct = defects.stream()
                .collect(Collectors.groupingBy(this::actKey));

        List<ReportDto.ReportByAct.ActGroup> groups = byAct.entrySet().stream()
                .map(e -> buildActGroup(e.getKey(), e.getValue(), producedWeight))
                .sorted((a, b) -> b.getGroupTotals().getTotal().compareTo(a.getGroupTotals().getTotal()))
                .collect(Collectors.toList());

        BigDecimal totalAll = sumWeight(defects);
        BigDecimal reworkedAll = sumReworked(defects);
        BigDecimal defectAll = sumIrreparable(defects);
        BigDecimal defectPercent = calcPercent(totalAll, producedWeight);

        return ReportDto.ReportByAct.builder()
                .periodFrom(filter.getDateFrom().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .periodTo(filter.getDateTo().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .producedWeight(producedWeight)
                .groups(groups)
                .totals(ReportDto.ReportByAct.Totals.builder()
                        .total(totalAll)
                        .reworked(reworkedAll)
                        .defect(defectAll)
                        .defectPercent(defectPercent)
                        .count(defects.size())
                        .build())
                .build();
    }

    private String actKey(NonconformingProduct d) {
        String note = d.getNote();
        if (note == null || note.trim().isEmpty()) {
            return "Без документа";
        }
        String normalized = note.trim().replaceFirst("\\s*№\\s*\\d+.*$", "").trim();
        return normalized.isEmpty() ? "Без документа" : normalized;
    }

    private String resolveDocumentType(String actNumber) {
        String lower = actNumber.toLowerCase();
        if (lower.startsWith("акт")) {
            return "Акт";
        }
        if (lower.startsWith("справка")) {
            return "Справка";
        }
        return "Иной";
    }

    private ReportDto.ReportByAct.ActGroup buildActGroup(String actNumber,
            List<NonconformingProduct> items, BigDecimal producedWeight) {
        String documentType = resolveDocumentType(actNumber);
        String siteName = items.stream()
                .map(d -> d.getProductionSite() != null ? d.getProductionSite().getSiteName() : null)
                .filter(s -> s != null)
                .findFirst()
                .orElse("-");

        List<ReportDto.ReportByAct.DefectRow> rows = items.stream()
                .collect(Collectors.groupingBy(d -> d.getDefectType().getDefectName()))
                .entrySet().stream()
                .map(e -> {
                    List<NonconformingProduct> defectItems = e.getValue();
                    return ReportDto.ReportByAct.DefectRow.builder()
                            .defectType(e.getKey())
                            .cause(defectItems.stream()
                                    .map(d -> d.getDefectCause() != null ? d.getDefectCause().getCauseName() : null)
                                    .filter(c -> c != null)
                                    .findFirst()
                                    .orElse("-"))
                            .total(sumWeight(defectItems))
                            .reworked(sumReworked(defectItems))
                            .reworkType(defectItems.stream()
                                    .map(d -> d.getReworkType() != null ? d.getReworkType().getReworkName() : null)
                                    .filter(r -> r != null)
                                    .findFirst()
                                    .orElse("-"))
                            .defect(sumIrreparable(defectItems))
                            .build();
                })
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .collect(Collectors.toList());

        BigDecimal groupTotal = sumWeight(items);
        BigDecimal groupReworked = sumReworked(items);
        BigDecimal groupDefect = sumIrreparable(items);
        BigDecimal groupPercent = calcPercent(groupTotal, producedWeight);

        return ReportDto.ReportByAct.ActGroup.builder()
                .actNumber(actNumber)
                .documentType(documentType)
                .siteName(siteName)
                .count(items.size())
                .rows(rows)
                .groupTotals(ReportDto.ReportByAct.Totals.builder()
                        .total(groupTotal)
                        .reworked(groupReworked)
                        .defect(groupDefect)
                        .defectPercent(groupPercent)
                        .count(items.size())
                        .build())
                .build();
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
                    .exceedsAllowable(false)
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
        boolean exceedsAllowable = false;

        if (isByFault) {
            allowable = defects.get(0).getProductionSite().getAllowableDefectPercent();
            exceedsAllowable = percent.compareTo(allowable) > 0;
        }

        rows.add(ReportDto.ReportByFault.FaultRow.builder()
                .category(category)
                .siteName(defects.get(0).getProductionSite().getSiteName())
                .produced(produced)
                .nonconforming(defectWeight)
                .nonconformingPercent(percent)
                .allowablePercent(allowable)
                .exceedsAllowable(exceedsAllowable)
                .build());
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

        BigDecimal totalWeight = sumWeight(defects);
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
