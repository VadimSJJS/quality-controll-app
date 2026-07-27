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
}