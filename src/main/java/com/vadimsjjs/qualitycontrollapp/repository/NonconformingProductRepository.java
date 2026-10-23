package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NonconformingProductRepository
        extends JpaRepository<NonconformingProduct, Long>, JpaSpecificationExecutor<NonconformingProduct> {





    /**
     * Используется ли вид дефекта в записях о несоответствующей продукции.
     * Нужен, чтобы не дать удалить из справочника значение, на которое ссылаются записи.
     */
    boolean existsByDefectType_Id(Long defectTypeId);



    /**
     * Загружает все записи с инициализированными связями
     * Устраняет N+1 ленивой загрузки при формировании списка записей.
     */
    @EntityGraph(attributePaths = {
            "productionSite", "detectionSource", "defectType", "defectCause",
            "defectSubcause", "diameter", "reworkType", "reworkActions"
    })
    @Query("SELECT n FROM NonconformingProduct n")
    List<NonconformingProduct> findAllWithEagerLoading();

    /**
     * Загружает запись по ID с инициализированными связями (без N+1).
     */
    @EntityGraph(attributePaths = {
            "productionSite", "detectionSource", "defectType", "defectCause",
            "defectSubcause", "diameter", "reworkType", "reworkActions"
    })
    @Query("SELECT n FROM NonconformingProduct n WHERE n.id = :id")
    Optional<NonconformingProduct> findByIdWithEagerLoading(@Param("id") Long id);

    /**
     * Суммарная масса несоответствующей продукции по видам дефектов (GROUP BY + SUM).
     */
    @Query(value = """
            SELECT n.defectType.id AS defectTypeId,
                   SUM(n.weightTonnes) AS totalWeight
            FROM NonconformingProduct n
            WHERE (:dateFrom IS NULL OR n.detectionDate >= :dateFrom)
              AND (:dateTo IS NULL OR n.detectionDate <= :dateTo)
              AND (:siteId IS NULL OR n.productionSite.id = :siteId)
              AND (:defectTypeId IS NULL OR n.defectType.id = :defectTypeId)
              AND (:defectCauseId IS NULL OR n.defectCause.id = :defectCauseId)
              AND (:defectSubcauseId IS NULL OR n.defectSubcause.id = :defectSubcauseId)
              AND (:diameterId IS NULL OR n.diameter.id = :diameterId)
              AND (:steelCordConstruction IS NULL OR n.steelCordConstruction = :steelCordConstruction)
              AND (:productCode IS NULL OR n.productCode = :productCode)
              AND (:heatNumber IS NULL OR n.heatNumber = :heatNumber)
              AND (:steelGrade IS NULL OR n.steelGrade = :steelGrade)
              AND (:equipmentKey IS NULL OR n.equipmentKey = :equipmentKey OR n.unitNumber = :equipmentKey
                   OR n.workpieceKey = :equipmentKey OR n.workpieceKey LIKE CONCAT(:equipmentKey, '-%'))
              AND (:operatorPersonalNumber IS NULL OR n.operatorPersonalNumber = :operatorPersonalNumber)
              AND (:manufacturerBrigade IS NULL OR n.manufacturerBrigade = :manufacturerBrigade)
              AND n.defectType IS NOT NULL
            GROUP BY n.defectType.id
            """)
    List<Object[]> sumWeightByDefectType(
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

    /**
     * Суммарная масса неисправимого брака по видам дефектов (GROUP BY + SUM).
     */
    @Query(value = """
            SELECT n.defectType.id AS defectTypeId,
                   SUM(n.irreparableWeightTonnes) AS irreparableWeight
            FROM NonconformingProduct n
            WHERE (:dateFrom IS NULL OR n.detectionDate >= :dateFrom)
              AND (:dateTo IS NULL OR n.detectionDate <= :dateTo)
              AND (:siteId IS NULL OR n.productionSite.id = :siteId)
              AND (:defectTypeId IS NULL OR n.defectType.id = :defectTypeId)
              AND (:defectCauseId IS NULL OR n.defectCause.id = :defectCauseId)
              AND (:defectSubcauseId IS NULL OR n.defectSubcause.id = :defectSubcauseId)
              AND (:diameterId IS NULL OR n.diameter.id = :diameterId)
              AND (:steelCordConstruction IS NULL OR n.steelCordConstruction = :steelCordConstruction)
              AND (:productCode IS NULL OR n.productCode = :productCode)
              AND (:heatNumber IS NULL OR n.heatNumber = :heatNumber)
              AND (:steelGrade IS NULL OR n.steelGrade = :steelGrade)
              AND (:equipmentKey IS NULL OR n.equipmentKey = :equipmentKey OR n.unitNumber = :equipmentKey
                   OR n.workpieceKey = :equipmentKey OR n.workpieceKey LIKE CONCAT(:equipmentKey, '-%'))
              AND (:operatorPersonalNumber IS NULL OR n.operatorPersonalNumber = :operatorPersonalNumber)
              AND (:manufacturerBrigade IS NULL OR n.manufacturerBrigade = :manufacturerBrigade)
              AND n.defectType IS NOT NULL
              AND n.irreparableWeightTonnes IS NOT NULL
              AND n.irreparableWeightTonnes > 0
            GROUP BY n.defectType.id
            """)
    List<Object[]> sumIrreparableByDefectType(
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

    /**
     * Суммарная масса по участкам (GROUP BY + SUM) для отчёта по цеху.
     */
    @Query(value = """
            SELECT n.productionSite.id AS siteId,
                   SUM(n.weightTonnes) AS totalWeight,
                   SUM(n.irreparableWeightTonnes) AS irreparableWeight
            FROM NonconformingProduct n
            WHERE (:dateFrom IS NULL OR n.detectionDate >= :dateFrom)
              AND (:dateTo IS NULL OR n.detectionDate <= :dateTo)
              AND (:siteId IS NULL OR n.productionSite.id = :siteId)
              AND (:defectTypeId IS NULL OR n.defectType.id = :defectTypeId)
              AND (:defectCauseId IS NULL OR n.defectCause.id = :defectCauseId)
              AND (:defectSubcauseId IS NULL OR n.defectSubcause.id = :defectSubcauseId)
              AND (:diameterId IS NULL OR n.diameter.id = :diameterId)
              AND (:steelCordConstruction IS NULL OR n.steelCordConstruction = :steelCordConstruction)
              AND (:productCode IS NULL OR n.productCode = :productCode)
              AND (:heatNumber IS NULL OR n.heatNumber = :heatNumber)
              AND (:steelGrade IS NULL OR n.steelGrade = :steelGrade)
              AND (:equipmentKey IS NULL OR n.equipmentKey = :equipmentKey OR n.unitNumber = :equipmentKey
                   OR n.workpieceKey = :equipmentKey OR n.workpieceKey LIKE CONCAT(:equipmentKey, '-%'))
              AND (:operatorPersonalNumber IS NULL OR n.operatorPersonalNumber = :operatorPersonalNumber)
              AND (:manufacturerBrigade IS NULL OR n.manufacturerBrigade = :manufacturerBrigade)
              AND n.productionSite IS NOT NULL
            GROUP BY n.productionSite.id
            """)
    List<Object[]> sumWeightBySite(
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