package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.SteelGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SteelGradeRepository extends JpaRepository<SteelGrade, Long> {

    @Query(value = "SELECT ID_STEEL_GRADE, STEEL_GRADE, ID_GROUP_STEEL FROM HLP_STEEL_GRADE ORDER BY STEEL_GRADE", nativeQuery = true)
    List<SteelGrade> findAllOrderBySteelGrade();
}