package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.DefectCause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefectCauseRepository extends JpaRepository<DefectCause, Long> {
    List<DefectCause> findByParentCauseIsNull();
    List<DefectCause> findByParentCauseId(Long parentId);
}