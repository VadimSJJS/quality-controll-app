package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.dto.ReportDto;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionReport;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/** Отчёты по вине цеха и актам. */
@Service
@RequiredArgsConstructor
public class FaultReportService {

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

        BigDecimal totalReworked = defectAggregator.sumReworked(allDefects);
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
                        .totalProduced(defectAggregator.sumProduced(productions))
                        .totalNonconforming(defectAggregator.sumWeight(allDefects))
                        .build())
                                .build();
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByAct getReportByActs(DefectFilterDto filter) {
        List<NonconformingProduct> defects = findDefects(filter, null);
        List<ProductionReport> productions =
                productionRepository.findByReportDateBetween(filter.getDateFrom(), filter.getDateTo());
        BigDecimal producedWeight = defectAggregator.sumProduced(productions);

        Map<String, List<NonconformingProduct>> byAct = defects.stream()
                .collect(Collectors.groupingBy(this::actKey));

        List<ReportDto.ReportByAct.ActGroup> groups = byAct.entrySet().stream()
                .map(e -> buildActGroup(e.getKey(), e.getValue(), producedWeight))
                .sorted((a, b) -> b.getGroupTotals().getTotal().compareTo(a.getGroupTotals().getTotal()))
                .collect(Collectors.toList());

        BigDecimal totalAll = defectAggregator.sumWeight(defects);
        BigDecimal reworkedAll = defectAggregator.sumReworked(defects);
        BigDecimal defectAll = defectAggregator.sumIrreparable(defects);
        BigDecimal defectPercent = defectAggregator.calcPercent(totalAll, producedWeight);

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
                            .total(defectAggregator.sumWeight(defectItems))
                            .reworked(defectAggregator.sumReworked(defectItems))
                            .reworkType(defectItems.stream()
                                    .map(d -> d.getReworkType() != null ? d.getReworkType().getReworkName() : null)
                                    .filter(r -> r != null)
                                    .findFirst()
                                    .orElse("-"))
                            .defect(defectAggregator.sumIrreparable(defectItems))
                            .build();
                })
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .collect(Collectors.toList());

        BigDecimal groupTotal = defectAggregator.sumWeight(items);
        BigDecimal groupReworked = defectAggregator.sumReworked(items);
        BigDecimal groupDefect = defectAggregator.sumIrreparable(items);
        BigDecimal groupPercent = defectAggregator.calcPercent(groupTotal, producedWeight);

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

        BigDecimal defectWeight = defectAggregator.sumWeight(defects);
        BigDecimal produced = BigDecimal.ZERO;

        if (isByFault) {
            String siteName = defects.get(0).getProductionSite().getSiteName();
            produced = productions.stream()
                    .filter(p -> p.getProductionSite().getSiteName().equals(siteName))
                    .map(ProductionReport::getProducedWeightTonnes)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal percent = defectAggregator.calcPercent(defectWeight, produced);
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

}
