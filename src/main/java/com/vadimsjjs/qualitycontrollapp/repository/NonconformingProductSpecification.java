package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Единая спецификация фильтров для {@link NonconformingProduct}.
 * Заменяет дублирующую логику в {@code findWithFilter} / {@code countWithFilters}.
 * Каждый параметр фильтра nullable — если null, условие не применяется.
 */
public final class NonconformingProductSpecification {

    private NonconformingProductSpecification() {
    }

    /**
     * Собирает Specification по {@link DefectFilterDto}.
     * Поддерживает все 16 параметров фильтра из ТЗ (п. 3.2).
     */
    public static Specification<NonconformingProduct> byFilter(DefectFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("detectionDate"), filter.getDateFrom()));
            }
            if (filter.getDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("detectionDate"), filter.getDateTo()));
            }
            if (filter.getProductionSiteId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("productionSite").get("id"), filter.getProductionSiteId()));
            }
            if (filter.getDefectTypeId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("defectType").get("id"), filter.getDefectTypeId()));
            }
            if (filter.getDefectCauseId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("defectCause").get("id"), filter.getDefectCauseId()));
            }
            if (filter.getDefectSubcauseId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("defectSubcause").get("id"), filter.getDefectSubcauseId()));
            }
            if (filter.getDiameterId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("diameter").get("id"), filter.getDiameterId()));
            }
            if (StringUtils.hasText(filter.getSteelCordConstruction())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("steelCordConstruction"), filter.getSteelCordConstruction()));
            }
            if (filter.getProductCode() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("productCode"), filter.getProductCode()));
            }
            if (StringUtils.hasText(filter.getHeatNumber())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("heatNumber"), filter.getHeatNumber()));
            }
            if (StringUtils.hasText(filter.getSteelGrade())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("steelGrade"), filter.getSteelGrade()));
            }
            if (StringUtils.hasText(filter.getEquipmentKey())) {
                String key = filter.getEquipmentKey();
                Predicate eqKey = criteriaBuilder.equal(root.get("equipmentKey"), key);
                Predicate eqUnit = criteriaBuilder.equal(root.get("unitNumber"), key);
                Predicate eqWorkpiece = criteriaBuilder.equal(root.get("workpieceKey"), key);
                Predicate likeWorkpiece = criteriaBuilder.like(
                        root.get("workpieceKey"), key + "-%");
                predicates.add(criteriaBuilder.or(eqKey, eqUnit, eqWorkpiece, likeWorkpiece));
            }
            if (filter.getOperatorPersonalNumber() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("operatorPersonalNumber"), filter.getOperatorPersonalNumber()));
            }
            if (filter.getManufacturerBrigade() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("manufacturerBrigade"), filter.getManufacturerBrigade()));
            }
            if (filter.getDetectionSourceId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("detectionSource").get("id"), filter.getDetectionSourceId()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
