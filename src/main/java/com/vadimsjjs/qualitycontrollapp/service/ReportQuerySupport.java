package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.NonconformingProductRepository;
import com.vadimsjjs.qualitycontrollapp.repository.NonconformingProductSpecification;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;

/**
 * Общие запросы и константы для отчётов.
 *
 * <p>Вынесен из {@link ReportService}, чтобы конкретные отчёты можно было разнести
 * по небольшим сервисам без дублирования фильтрации и нормативов.
 */
@Service
@RequiredArgsConstructor
public class ReportQuerySupport {

    private final NonconformingProductRepository nonconformingRepository;
    private final ProductionSiteRepository productionSiteRepository;

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Допустимый уровень несоответствующей продукции по цеху, % (Приложение А, Рис. 8). */
    public static final BigDecimal PLANT_ALLOWABLE_TOTAL = new BigDecimal("0.55");

    /** Допустимый уровень неисправимой (брака) продукции по цеху, %. */
    public static final BigDecimal PLANT_ALLOWABLE_IRREPARABLE_TOTAL = new BigDecimal("0.04");

    /** Допустимый уровень неисправимой продукции по участкам, %. */
    public static final Map<String, BigDecimal> SITE_ALLOWABLE_IRREPARABLE = Map.of(
            "ГСВ", new BigDecimal("0.00"),
            "ТТГУ", new BigDecimal("0.01"),
            "УТВ-МК", new BigDecimal("0.01"),
            "УТВ-РМЛ", new BigDecimal("0.01"),
            "КУ-1", new BigDecimal("0.07"),
            "КУ-2", new BigDecimal("0.07")
    );

    /**
     * Единая точка выборки записей для всех отчётов.
     *
     * @param forcedSiteId участок, заданный параметром отчёта (иначе берётся из фильтра)
     */
    public List<NonconformingProduct> findDefects(DefectFilterDto filter, Long forcedSiteId) {
        DefectFilterDto effectiveFilter = filter;
        if (forcedSiteId != null && forcedSiteId != filter.getProductionSiteId()) {
            effectiveFilter = new DefectFilterDto();
            effectiveFilter.setDateFrom(filter.getDateFrom());
            effectiveFilter.setDateTo(filter.getDateTo());
            effectiveFilter.setProductionSiteId(forcedSiteId);
            effectiveFilter.setDefectTypeId(filter.getDefectTypeId());
            effectiveFilter.setDefectCauseId(filter.getDefectCauseId());
            effectiveFilter.setDefectSubcauseId(filter.getDefectSubcauseId());
            effectiveFilter.setDiameterId(filter.getDiameterId());
            effectiveFilter.setSteelCordConstruction(filter.getSteelCordConstruction());
            effectiveFilter.setProductCode(filter.getProductCode());
            effectiveFilter.setHeatNumber(filter.getHeatNumber());
            effectiveFilter.setSteelGrade(filter.getSteelGrade());
            effectiveFilter.setEquipmentKey(filter.getEquipmentKey());
            effectiveFilter.setOperatorPersonalNumber(filter.getOperatorPersonalNumber());
            effectiveFilter.setManufacturerBrigade(filter.getManufacturerBrigade());
            effectiveFilter.setDetectionSourceId(filter.getDetectionSourceId());
        }
        return nonconformingRepository.findAll(NonconformingProductSpecification.byFilter(effectiveFilter));
    }

    public ProductionSite getProductionSiteByCode(String siteCode) {
        return productionSiteRepository.findBySiteCode(siteCode)
                .orElseThrow(() -> new IllegalArgumentException("Участок с кодом '" + siteCode + "' не найден"));
    }

    /**
     * Код участка в виде ключа норматива: в базе коды УТВ-участков записаны
     * с подчёркиванием (УТВ_МК), а в нормативах — с дефисом (УТВ-МК).
     */
    public static String normalizeSiteCode(String siteCode) {
        if (siteCode == null) {
            return "";
        }
        return siteCode.trim().replace('_', '-');
    }
}
