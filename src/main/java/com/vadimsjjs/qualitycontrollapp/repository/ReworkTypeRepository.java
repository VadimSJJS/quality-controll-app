package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.ReworkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReworkTypeRepository extends JpaRepository<ReworkType, Long> {
    Optional<ReworkType> findByReworkName(String reworkName);
}