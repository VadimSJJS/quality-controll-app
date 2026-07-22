package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.DetectionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionSourceRepository extends JpaRepository<DetectionSource, Long> {
}