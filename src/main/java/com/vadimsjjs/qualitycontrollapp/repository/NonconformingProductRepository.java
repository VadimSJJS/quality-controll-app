package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NonconformingProductRepository extends JpaRepository<NonconformingProduct, Long> {

    @Query("SELECT n FROM NonconformingProduct n " +
            "WHERE (:dateFrom IS NULL OR n.detectionDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR n.detectionDate <= :dateTo) " +
            "AND (:siteId IS NULL OR n.productionSite.id = :siteId) " +
            "AND (:defectTypeId IS NULL OR n.defectType.id = :defectTypeId)")
    List<NonconformingProduct> findWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("siteId") Long siteId,
            @Param("defectTypeId") Long defectTypeId);

    @Query(value = "SELECT * FROM ( " +
            "SELECT a.*, ROWNUM rn FROM ( " +
            "SELECT * FROM NONCONFORMING_PRODUCT n " +
            "WHERE (:dateFrom IS NULL OR n.DETECTION_DATE >= :dateFrom) " +
            "AND (:dateTo IS NULL OR n.DETECTION_DATE <= :dateTo) " +
            "AND (:siteId IS NULL OR n.ID_PRODUCTION_SITE = :siteId) " +
            "AND (:defectTypeId IS NULL OR n.ID_DEFECT_TYPE = :defectTypeId) " +
            "AND (:equipmentKey IS NULL OR n.EQUIPMENT_KEY = :equipmentKey) " +
            "AND (:operatorPersonalNumber IS NULL OR n.OPERATOR_PERSONAL_NUMBER = :operatorPersonalNumber) " +
            "ORDER BY n.DETECTION_DATE DESC " +
            ") a WHERE ROWNUM <= :endRow " +
            ") WHERE rn > :startRow",
            nativeQuery = true)
    List<NonconformingProduct> findWithFiltersNative(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("siteId") Long siteId,
            @Param("defectTypeId") Long defectTypeId,
            @Param("equipmentKey") String equipmentKey,
            @Param("operatorPersonalNumber") Long operatorPersonalNumber,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow);

    @Query("SELECT COUNT(n) FROM NonconformingProduct n " +
            "WHERE (:dateFrom IS NULL OR n.detectionDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR n.detectionDate <= :dateTo) " +
            "AND (:siteId IS NULL OR n.productionSite.id = :siteId) " +
            "AND (:defectTypeId IS NULL OR n.defectType.id = :defectTypeId) " +
            "AND (:equipmentKey IS NULL OR n.equipmentKey = :equipmentKey) " +
            "AND (:operatorPersonalNumber IS NULL OR n.operatorPersonalNumber = :operatorPersonalNumber)")
    long countWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("siteId") Long siteId,
            @Param("defectTypeId") Long defectTypeId,
            @Param("equipmentKey") String equipmentKey,
            @Param("operatorPersonalNumber") Long operatorPersonalNumber);
}