package com.vadimsjjs.qualitycontrollapp.repository;

import com.vadimsjjs.qualitycontrollapp.entity.ProductionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductionReportRepository extends JpaRepository<ProductionReport, Long> {

    @Query("SELECT p FROM ProductionReport p WHERE p.reportDate BETWEEN :dateFrom AND :dateTo")
    List<ProductionReport> findByReportDateBetween(@Param("dateFrom") LocalDate dateFrom,
                                                   @Param("dateTo") LocalDate dateTo);

    @Query("SELECT p FROM ProductionReport p WHERE p.reportDate = :date AND p.productionSite.id = :siteId")
    ProductionReport findByReportDateAndProductionSiteId(@Param("date") LocalDate date,
                                                         @Param("siteId") Long siteId);
}