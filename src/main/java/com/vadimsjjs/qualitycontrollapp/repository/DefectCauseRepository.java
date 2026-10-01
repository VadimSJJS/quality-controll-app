package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.DefectCause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefectCauseRepository extends JpaRepository<DefectCause, Long> {

    @Query(value = "SELECT * FROM HLP_DEFECT_CAUSE WHERE ID_PARENT_CAUSE IS NULL ORDER BY CAUSE_NAME", nativeQuery = true)
    List<DefectCause> findByParentCauseIsNull();

    @Query(value = "SELECT * FROM HLP_DEFECT_CAUSE WHERE ID_PARENT_CAUSE = :parentId ORDER BY CAUSE_NAME", nativeQuery = true)
    List<DefectCause> findByParentCauseId(@Param("parentId") Long parentId);

    Optional<DefectCause> findByCauseName(String causeName);

    /** Поиск причины по названию без учёта регистра — для свободного ввода в форме. */
    @Query(value = "SELECT * FROM HLP_DEFECT_CAUSE WHERE UPPER(CAUSE_NAME) = UPPER(:name)", nativeQuery = true)
    List<DefectCause> findByCauseNameIgnoreCase(@Param("name") String name);

    /** Поиск подпричины по названию среди подпричин указанной причины. */
    @Query(value = "SELECT * FROM HLP_DEFECT_CAUSE WHERE ID_PARENT_CAUSE = :parentId AND UPPER(CAUSE_NAME) = UPPER(:name)", nativeQuery = true)
    List<DefectCause> findByParentAndNameIgnoreCase(@Param("parentId") Long parentId, @Param("name") String name);

    /** Все причины и подпричины — для подсказок при вводе. */
    @Query(value = "SELECT * FROM HLP_DEFECT_CAUSE ORDER BY CAUSE_NAME", nativeQuery = true)
    List<DefectCause> findAllOrdered();
}