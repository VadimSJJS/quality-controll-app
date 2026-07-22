package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionSiteRepository extends JpaRepository<ProductionSite, Long> {
    List<ProductionSite> findAllByOrderBySiteCode();
}