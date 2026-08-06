package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.ProductionReportRequest;
import com.vadimsjjs.qualitycontrollapp.dto.ProductionReportResponse;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionReport;
import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionReportRepository;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionSiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionReportService {

    private final ProductionReportRepository repository;
    private final ProductionSiteRepository productionSiteRepository;

    @Transactional
    public ProductionReportResponse create(ProductionReportRequest request) {
        ProductionReport entity = toEntity(request);
        ProductionReport saved = repository.save(entity);
        log.info("Создан отчёт производства с ID: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ProductionReportResponse update(Long id, ProductionReportRequest request) {
        ProductionReport entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Отчёт не найден с ID: " + id));
        updateEntity(entity, request);
        ProductionReport updated = repository.save(entity);
        log.info("Обновлён отчёт производства с ID: {}", updated.getId());
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Отчёт не найден с ID: " + id);
        }
        repository.deleteById(id);
        log.info("Удалён отчёт производства с ID: {}", id);
    }

    public ProductionReportResponse findById(Long id) {
        ProductionReport entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Отчёт не найден с ID: " + id));
        return toResponse(entity);
    }

    public List<ProductionReportResponse> findByDateRange(LocalDate dateFrom, LocalDate dateTo) {
        List<ProductionReport> reports = repository.findByReportDateBetween(dateFrom, dateTo);
        return reports.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductionReportResponse findByDateAndSite(LocalDate date, Long siteId) {
        ProductionReport entity = repository.findByReportDateAndProductionSiteId(date, siteId);
        if (entity == null) {
            return null;
        }
        return toResponse(entity);
    }

    public Page<ProductionReportResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toResponse);
    }

    private ProductionReport toEntity(ProductionReportRequest request) {
        ProductionReport entity = new ProductionReport();
        entity.setReportDate(request.getReportDate());

        ProductionSite site = productionSiteRepository.findById(request.getProductionSiteId())
                .orElseThrow(() -> new RuntimeException("Участок не найден"));
        entity.setProductionSite(site);

        entity.setProducedWeightTonnes(request.getProducedWeightTonnes());
        entity.setSourceSystem(request.getSourceSystem());

        return entity;
    }

    private void updateEntity(ProductionReport entity, ProductionReportRequest request) {
        entity.setReportDate(request.getReportDate());

        if (request.getProductionSiteId() != null) {
            ProductionSite site = productionSiteRepository.findById(request.getProductionSiteId())
                    .orElseThrow(() -> new RuntimeException("Участок не найден"));
            entity.setProductionSite(site);
        }

        entity.setProducedWeightTonnes(request.getProducedWeightTonnes());
        entity.setSourceSystem(request.getSourceSystem());
    }

    private ProductionReportResponse toResponse(ProductionReport entity) {
        return ProductionReportResponse.builder()
                .id(entity.getId())
                .reportDate(entity.getReportDate())
                .productionSiteId(entity.getProductionSite() != null ? entity.getProductionSite().getId() : null)
                .productionSiteName(entity.getProductionSite() != null ? entity.getProductionSite().getSiteName() : null)
                .productionSiteCode(entity.getProductionSite() != null ? entity.getProductionSite().getSiteCode() : null)
                .producedWeightTonnes(entity.getProducedWeightTonnes())
                .sourceSystem(entity.getSourceSystem())
                .build();
    }
}
