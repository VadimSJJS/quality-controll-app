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

    @Query(value = "SELECT * FROM ( " +
            "SELECT a.*, ROWNUM rn FROM ( " +
            "SELECT " +
            "    ID_NONCONFORMING_PRODUCT, " +
            "    DETECTION_DATE, " +
            "    ID_PRODUCTION_SITE, " +
            "    ID_DETECTION_SOURCE, " +
            "    WEIGHT_TONNES, " +
            "    IRREPARABLE_WEIGHT_TONNES, " +
            "    ID_DEFECT_TYPE, " +
            "    ID_DEFECT_CAUSE, " +
            "    ID_DEFECT_SUBCAUSE, " +
            "    NOTE, " +
            "    PRODUCT_CODE, " +
            "    REEL_NUMBER, " +
            "    HEAT_NUMBER, " +
            "    MANUFACTURER_BRIGADE, " +
            "    BUNDLE_NUMBER, " +
            "    MANUFACTURER_WORKSHOP, " +
            "    EQUIPMENT_KEY, " +
            "    ID_DIAMETER, " +
            "    STEEL_CORD_CONSTRUCTION, " +
            "    OPERATOR_PERSONAL_NUMBER, " +
            "    BRIGADE, " +
            "    STEEL_GRADE, " +
            "    UNIT_NUMBER, " +
            "    WORKPIECE_KEY, " +
            "    QUANTITY, " +
            "    REWORK_QUANTITY, " +
            "    REWORK_DATE, " +
            "    ID_REWORK_TYPE, " +
            "    REWORK_WEIGHT_TONNES " +
            "FROM NONCONFORMING_PRODUCT n " +
            "WHERE (:dateFrom IS NULL OR n.DETECTION_DATE >= :dateFrom) " +
            "AND (:dateTo IS NULL OR n.DETECTION_DATE <= :dateTo) " +
            "AND (:siteId IS NULL OR n.ID_PRODUCTION_SITE = :siteId) " +
            "AND (:defectTypeId IS NULL OR n.ID_DEFECT_TYPE = :defectTypeId) " +
            "AND (:defectCauseId IS NULL OR n.ID_DEFECT_CAUSE = :defectCauseId) " +
            "AND (:defectSubcauseId IS NULL OR n.ID_DEFECT_SUBCAUSE = :defectSubcauseId) " +
            "AND (:diameterId IS NULL OR n.ID_DIAMETER = :diameterId) " +
            "AND (:steelCordConstruction IS NULL OR n.STEEL_CORD_CONSTRUCTION = :steelCordConstruction) " +
            "AND (:productCode IS NULL OR n.PRODUCT_CODE = :productCode) " +
            "AND (:heatNumber IS NULL OR n.HEAT_NUMBER = :heatNumber) " +
            "AND (:steelGrade IS NULL OR n.STEEL_GRADE = :steelGrade) " +
            "AND (:equipmentKey IS NULL OR n.EQUIPMENT_KEY = :equipmentKey) " +
            "AND (:operatorPersonalNumber IS NULL OR n.OPERATOR_PERSONAL_NUMBER = :operatorPersonalNumber) " +
            "AND (:manufacturerBrigade IS NULL OR n.MANUFACTURER_BRIGADE = :manufacturerBrigade) " +
            "AND (:detectionSourceId IS NULL OR n.ID_DETECTION_SOURCE = :detectionSourceId) " +
            "ORDER BY n.DETECTION_DATE DESC " +
            ") a WHERE ROWNUM <= :endRow " +
            ") WHERE rn > :startRow",
            nativeQuery = true)
    List<NonconformingProduct> findWithFiltersNative(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("siteId") Long siteId,
            @Param("defectTypeId") Long defectTypeId,
            @Param("defectCauseId") Long defectCauseId,
            @Param("defectSubcauseId") Long defectSubcauseId,
            @Param("diameterId") Long diameterId,
            @Param("steelCordConstruction") String steelCordConstruction,
            @Param("productCode") Long productCode,
            @Param("heatNumber") String heatNumber,
            @Param("steelGrade") String steelGrade,
            @Param("equipmentKey") String equipmentKey,
            @Param("operatorPersonalNumber") Long operatorPersonalNumber,
            @Param("manufacturerBrigade") Long manufacturerBrigade,
            @Param("detectionSourceId") Long detectionSourceId,
            @Param("startRow") int startRow,
            @Param("endRow") int endRow);

    @Query("SELECT COUNT(n) FROM NonconformingProduct n " +
            "WHERE (:dateFrom IS NULL OR n.detectionDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR n.detectionDate <= :dateTo) " +
            "AND (:siteId IS NULL OR n.productionSite.id = :siteId) " +
            "AND (:defectTypeId IS NULL OR n.defectType.id = :defectTypeId) " +
            "AND (:defectCauseId IS NULL OR n.defectCause.id = :defectCauseId) " +
            "AND (:defectSubcauseId IS NULL OR n.defectSubcause.id = :defectSubcauseId) " +
            "AND (:diameterId IS NULL OR n.diameter.id = :diameterId) " +
            "AND (:steelCordConstruction IS NULL OR n.steelCordConstruction = :steelCordConstruction) " +
            "AND (:productCode IS NULL OR n.productCode = :productCode) " +
            "AND (:heatNumber IS NULL OR n.heatNumber = :heatNumber) " +
            "AND (:steelGrade IS NULL OR n.steelGrade = :steelGrade) " +
            "AND (:equipmentKey IS NULL OR n.equipmentKey = :equipmentKey) " +
            "AND (:operatorPersonalNumber IS NULL OR n.operatorPersonalNumber = :operatorPersonalNumber) " +
            "AND (:manufacturerBrigade IS NULL OR n.manufacturerBrigade = :manufacturerBrigade) " +
            "AND (:detectionSourceId IS NULL OR n.detectionSource.id = :detectionSourceId)")
    long countWithFilters(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("siteId") Long siteId,
            @Param("defectTypeId") Long defectTypeId,
            @Param("defectCauseId") Long defectCauseId,
            @Param("defectSubcauseId") Long defectSubcauseId,
            @Param("diameterId") Long diameterId,
            @Param("steelCordConstruction") String steelCordConstruction,
            @Param("productCode") Long productCode,
            @Param("heatNumber") String heatNumber,
            @Param("steelGrade") String steelGrade,
            @Param("equipmentKey") String equipmentKey,
            @Param("operatorPersonalNumber") Long operatorPersonalNumber,
            @Param("manufacturerBrigade") Long manufacturerBrigade,
            @Param("detectionSourceId") Long detectionSourceId);

    /**
     * Используется ли вид дефекта в записях о несоответствующей продукции.
     * Нужен, чтобы не дать удалить из справочника значение, на которое ссылаются записи.
     */
    boolean existsByDefectType_Id(Long defectTypeId);

    /**
     * Выборка записей по полному набору фильтров из ТЗ (п. 3.2):
     * даты выявления, участок, вид несоответствия, причина, подпричина, диаметр,
     * конструкция металлокорда, код, номер плавки, марка стали, оборудование,
     * персональный номер оператора, бригада изготовителя.
     */
    @Query("SELECT n FROM NonconformingProduct n " +
            "WHERE (:dateFrom IS NULL OR n.detectionDate >= :dateFrom) " +
            "AND (:dateTo IS NULL OR n.detectionDate <= :dateTo) " +
            "AND (:siteId IS NULL OR n.productionSite.id = :siteId) " +
            "AND (:defectTypeId IS NULL OR n.defectType.id = :defectTypeId) " +
            "AND (:defectCauseId IS NULL OR n.defectCause.id = :defectCauseId) " +
            "AND (:defectSubcauseId IS NULL OR n.defectSubcause.id = :defectSubcauseId) " +
            "AND (:diameterId IS NULL OR n.diameter.id = :diameterId) " +
            "AND (:steelCordConstruction IS NULL OR n.steelCordConstruction = :steelCordConstruction) " +
            "AND (:productCode IS NULL OR n.productCode = :productCode) " +
            "AND (:heatNumber IS NULL OR n.heatNumber = :heatNumber) " +
            "AND (:steelGrade IS NULL OR n.steelGrade = :steelGrade) " +
            "AND (:equipmentKey IS NULL OR n.equipmentKey = :equipmentKey) " +
            "AND (:operatorPersonalNumber IS NULL OR n.operatorPersonalNumber = :operatorPersonalNumber) " +
            "AND (:manufacturerBrigade IS NULL OR n.manufacturerBrigade = :manufacturerBrigade)")
    List<NonconformingProduct> findWithFilter(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("siteId") Long siteId,
            @Param("defectTypeId") Long defectTypeId,
            @Param("defectCauseId") Long defectCauseId,
            @Param("defectSubcauseId") Long defectSubcauseId,
            @Param("diameterId") Long diameterId,
            @Param("steelCordConstruction") String steelCordConstruction,
            @Param("productCode") Long productCode,
            @Param("heatNumber") String heatNumber,
            @Param("steelGrade") String steelGrade,
            @Param("equipmentKey") String equipmentKey,
            @Param("operatorPersonalNumber") Long operatorPersonalNumber,
            @Param("manufacturerBrigade") Long manufacturerBrigade);
}