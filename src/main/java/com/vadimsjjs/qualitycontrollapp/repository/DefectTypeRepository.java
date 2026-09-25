package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.DefectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefectTypeRepository extends JpaRepository<DefectType, Long> {

    Optional<DefectType> findByDefectName(String defectName);

    /** Поиск без учёта регистра — чтобы «Намот» и «НАМОТ» не дублировались. */
    Optional<DefectType> findByDefectNameIgnoreCase(String defectName);

    boolean existsByDefectNameIgnoreCase(String defectName);

    /** Подсказки при вводе названия дефекта в форме регистрации продукции. */
    List<DefectType> findTop50ByDefectNameContainingIgnoreCaseOrderByDefectNameAsc(String fragment);
}
