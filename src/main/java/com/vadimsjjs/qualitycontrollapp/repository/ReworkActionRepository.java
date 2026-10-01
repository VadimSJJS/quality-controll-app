package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.ReworkAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReworkActionRepository extends JpaRepository<ReworkAction, Long> {

    /** Все действия по доработке записи, в порядке дат — для прослеживаемости. */
    List<ReworkAction> findByNonconformingProductIdOrderByActionDateAscIdAsc(Long defectId);
}