package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.*;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionReport;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionReportRepository;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionSiteRepository;
import com.vadimsjjs.qualitycontrollapp.service.DefectAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/** Отчёты по участку, продукции, бригаде, оборудованию, персоналу и цеху. */
@Service
@RequiredArgsConstructor
public class SiteReportService {

    private final ReportQuerySupport support;
    private final ProductionSiteRepository productionSiteRepository;
    private final ProductionReportRepository productionRepository;
    private final DefectAggregator defectAggregator;

    private static final DateTimeFormatter DATE_FORMATTER = ReportQuerySupport.DATE_FORMATTER;
    private static final BigDecimal PLANT_ALLOWABLE_TOTAL = ReportQuerySupport.PLANT_ALLOWABLE_TOTAL;
    private static final BigDecimal PLANT_ALLOWABLE_IRREPARABLE_TOTAL = ReportQuerySupport.PLANT_ALLOWABLE_IRREPARABLE_TOTAL;
    private static final Map<String, BigDecimal> SITE_ALLOWABLE_IRREPARABLE = ReportQuerySupport.SITE_ALLOWABLE_IRREPARABLE;

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
    public ReportDto.ReportBySite getReportBySite(String siteCode, DefectFilterDto filter) {
        ProductionSite site = getProductionSiteByCode(siteCode);
        List<NonconformingProduct> defects = findDefects(filter, site.getId());

        List<ReportDto.ReportBySite.DefectRow> rows = defects.stream()
                .collect(Collectors.groupingBy(d -> d.getDefectType().getDefectName()))
                .entrySet().stream()
                .map(entry -> {
                    String defectType = entry.getKey();
                    List<NonconformingProduct> items = entry.getValue();

                    BigDecimal total = defectAggregator.sumWeight(items);
                    BigDecimal reworked = defectAggregator.sumReworked(items);
                    BigDecimal defect = defectAggregator.sumIrreparable(items);
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

        BigDecimal totalAll = defectAggregator.sumWeight(defects);
        BigDecimal reworkedAll = defectAggregator.sumReworked(defects);
        BigDecimal defectAll = defectAggregator.sumIrreparable(defects);

        BigDecimal producedWeight = BigDecimal.ZERO;
        BigDecimal allowablePercent = BigDecimal.ZERO;
        boolean exceedsAllowable = false;

        if (!defects.isEmpty()) {
            producedWeight = productionRepository.sumProducedBySiteAndDateRange(filter.getDateFrom(), filter.getDateTo(), site.getId());
            allowablePercent = site.getAllowableDefectPercent() != null
                    ? site.getAllowableDefectPercent()
                    : BigDecimal.ZERO;
            exceedsAllowable = defectAggregator.calcPercent(totalAll, producedWeight).compareTo(allowablePercent) > 0;
        }

        BigDecimal defectPercent = defectAggregator.calcPercent(totalAll, producedWeight);

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
                                        .total(defectAggregator.sumWeight(defectItems))
                                        .reworked(defectAggregator.sumReworked(defectItems))
                                        .reworkType(defectItems.stream()
                                                .filter(d -> d.getReworkType() != null)
                                                .findFirst()
                                                .map(d -> d.getReworkType().getReworkName())
                                                .orElse(""))
                                        .defect(defectAggregator.sumIrreparable(defectItems))
                                        .build();
                            })
                            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                            .collect(Collectors.toList());

                    BigDecimal groupTotal = defectAggregator.sumWeight(items);
                    BigDecimal groupReworked = defectAggregator.sumReworked(items);
                    BigDecimal groupDefect = defectAggregator.sumIrreparable(items);

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

        BigDecimal totalAll = defectAggregator.sumWeight(defects);
        BigDecimal reworkedAll = defectAggregator.sumReworked(defects);
        BigDecimal defectAll = defectAggregator.sumIrreparable(defects);

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

                    BigDecimal groupTotal = defectAggregator.sumWeight(items);

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
                        .total(defectAggregator.sumWeight(defects))
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
                                        .total(defectAggregator.sumWeight(defectItems))
                                        .reworked(defectAggregator.sumReworked(defectItems))
                                        .reworkType(defectItems.stream()
                                                .filter(d -> d.getReworkType() != null)
                                                .findFirst()
                                                .map(d -> d.getReworkType().getReworkName())
                                                .orElse(""))
                                        .defect(defectAggregator.sumIrreparable(defectItems))
                                        .build();
                            })
                            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                            .collect(Collectors.toList());

                    BigDecimal groupTotal = defectAggregator.sumWeight(items);
                    BigDecimal groupReworked = defectAggregator.sumReworked(items);
                    BigDecimal groupDefect = defectAggregator.sumIrreparable(items);

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

        BigDecimal totalAll = defectAggregator.sumWeight(defects);
        BigDecimal reworkedAll = defectAggregator.sumReworked(defects);
        BigDecimal defectAll = defectAggregator.sumIrreparable(defects);

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
                .filter(d -> d.resolveEquipment() != null)
                .collect(Collectors.groupingBy(NonconformingProduct::resolveEquipment));

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
                                        .total(defectAggregator.sumWeight(defectItems))
                                        .reworked(defectAggregator.sumReworked(defectItems))
                                        .reworkType(defectItems.stream()
                                                .filter(d -> d.getReworkType() != null)
                                                .findFirst()
                                                .map(d -> d.getReworkType().getReworkName())
                                                .orElse(""))
                                        .defect(defectAggregator.sumIrreparable(defectItems))
                                        .build();
                            })
                            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                            .collect(Collectors.toList());

                    BigDecimal groupTotal = defectAggregator.sumWeight(items);
                    BigDecimal groupReworked = defectAggregator.sumReworked(items);
                    BigDecimal groupDefect = defectAggregator.sumIrreparable(items);

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

        BigDecimal totalAll = defectAggregator.sumWeight(defects);
        BigDecimal reworkedAll = defectAggregator.sumReworked(defects);
        BigDecimal defectAll = defectAggregator.sumIrreparable(defects);

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
                                        .total(defectAggregator.sumWeight(defectItems))
                                        .defect(defectAggregator.sumIrreparable(defectItems))
                                        .note(defectItems.stream()
                                                .findFirst()
                                                .map(NonconformingProduct::getNote)
                                                .orElse(""))
                                        .build();
                            })
                            .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                            .collect(Collectors.toList());

                    BigDecimal groupTotal = defectAggregator.sumWeight(items);
                    BigDecimal groupDefect = defectAggregator.sumIrreparable(items);

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

        BigDecimal totalAll = defectAggregator.sumWeight(defects);
        BigDecimal defectAll = defectAggregator.sumIrreparable(defects);

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
        BigDecimal totalProduced = defectAggregator.sumProduced(productions);
        BigDecimal totalDefect = defectAggregator.sumWeight(allDefects);
        BigDecimal totalIrreparable = defectAggregator.sumIrreparable(allDefects);
        BigDecimal totalReworkable = totalDefect.subtract(totalIrreparable);
        BigDecimal totalReworked = defectAggregator.sumReworked(allDefects);

        Map<String, BigDecimal> reworkableAllowableBySiteName = productionSiteRepository.findAll().stream()
                .collect(Collectors.toMap(ProductionSite::getSiteName,
                        s -> s.getAllowableDefectPercent() != null ? s.getAllowableDefectPercent() : BigDecimal.ZERO));
        Map<String, String> siteCodeByName = productionSiteRepository.findAll().stream()
                .collect(Collectors.toMap(ProductionSite::getSiteName, ProductionSite::getSiteCode));

        List<NonconformingProduct> plantFaultDefects= allDefects.stream()
                .filter(this::isPlantFault)
                .collect(Collectors.toList());
        BigDecimal plantFaultDefect= defectAggregator.sumWeight(plantFaultDefects);
        BigDecimal plantFaultIrreparable = defectAggregator.sumIrreparable(plantFaultDefects);

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
        rows.add(plantRow("Итого по СтПЦ-2", totalProduced, totalDefect, defectAggregator.calcPercent(totalDefect, totalProduced), PLANT_ALLOWABLE_TOTAL, "total"));
        rows.add(plantRow("Несоответствующая по вине цеха", totalProduced, plantFaultDefect, defectAggregator.calcPercent(plantFaultDefect, totalProduced), PLANT_ALLOWABLE_TOTAL, "by_fault"));
        rows.add(plantRow("Несоответствующая исправимая, в т.ч.", totalProduced, totalReworkable, defectAggregator.calcPercent(totalReworkable, totalProduced), PLANT_ALLOWABLE_TOTAL, "reworkable_total"));
        addPlantSiteRows(rows, "reworkable_site", allDefects, producedBySite, reworkableAllowableBySiteName, siteCodeByName, true);

        rows.add(plantRow("Несоответствующая неисправимая всего, в т.ч.", totalProduced, totalIrreparable, defectAggregator.calcPercent(totalIrreparable, totalProduced), PLANT_ALLOWABLE_IRREPARABLE_TOTAL, "irreparable_total"));
        rows.add(plantRow("Несоответствующая неисправимая по вине цеха", totalProduced, plantFaultIrreparable, defectAggregator.calcPercent(plantFaultIrreparable, totalProduced), PLANT_ALLOWABLE_IRREPARABLE_TOTAL, "irreparable_by_fault"));
        addPlantSiteRows(rows, "irreparable_site", allDefects, producedBySite, reworkableAllowableBySiteName, siteCodeByName, false);

        rows.add(plantRow("НЦП", totalProduced, ncpDefect, defectAggregator.calcPercent(ncpDefect, totalProduced), BigDecimal.ZERO, "ncp"));
        rows.add(plantRow("Внешний брак", totalProduced, externalDefect, defectAggregator.calcPercent(externalDefect, totalProduced), BigDecimal.ZERO, "external"));

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
                        .totalNonconformingPercent(defectAggregator.calcPercent(totalDefect, totalProduced))
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
                        mass = defs.stream().map(d -> defectAggregator.reworkableWeight(d)).reduce(BigDecimal.ZERO, BigDecimal::add);
                    } else {
                        mass = defectAggregator.sumIrreparable(defs);
                    }
                    if (mass.compareTo(BigDecimal.ZERO) == 0) {
                        return;
                    }
                    BigDecimal produced = producedBySite.getOrDefault(siteName, BigDecimal.ZERO);
                    BigDecimal allowable = reworkable
                            ? reworkableAllowableBySiteName.getOrDefault(siteName, BigDecimal.ZERO)
                            : SITE_ALLOWABLE_IRREPARABLE.getOrDefault(normalizeSiteCode(siteCodeByName.get(siteName)), BigDecimal.ZERO);
                    BigDecimal percent = defectAggregator.calcPercent(mass, produced);
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


    private boolean isPlantFault(NonconformingProduct d) {
        String workshop = d.getManufacturerWorkshop();
        return workshop == null || workshop.isBlank() || workshop.toLowerCase().contains("стпц");
    }

    private boolean isExternalReturn(NonconformingProduct d) {
        return d.getDetectionSource() != null
                && d.getDetectionSource().getSourceName() != null
                && d.getDetectionSource().getSourceName().toLowerCase().contains("внешн");
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

}
