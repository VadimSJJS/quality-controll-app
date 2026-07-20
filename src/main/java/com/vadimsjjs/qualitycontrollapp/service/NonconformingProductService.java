package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.NonconformingProductRequest;
import com.vadimsjjs.qualitycontrollapp.dto.NonconformingProductResponse;
import com.vadimsjjs.qualitycontrollapp.entity.*;
import com.vadimsjjs.qualitycontrollapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NonconformingProductService {

    private final NonconformingProductRepository repository;
    private final ProductionSiteRepository productionSiteRepository;
    private final DetectionSourceRepository detectionSourceRepository;
    private final DefectTypeRepository defectTypeRepository;
    private final DefectCauseRepository defectCauseRepository;
    private final ReworkTypeRepository reworkTypeRepository;

    @Transactional
    public NonconformingProductResponse create(NonconformingProductRequest request) {
        NonconformingProduct entity = toEntity(request);
        NonconformingProduct saved = repository.save(entity);
        log.info("Создана запись с ID: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public NonconformingProductResponse update(Long id, NonconformingProductRequest request) {
        NonconformingProduct entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Запись не найдена с ID: " + id));
        updateEntity(entity, request);
        NonconformingProduct updated = repository.save(entity);
        log.info("Обновлена запись с ID: {}", updated.getId());
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Запись не найдена с ID: " + id);
        }
        repository.deleteById(id);
        log.info("Удалена запись с ID: {}", id);
    }

    public NonconformingProductResponse findById(Long id) {
        NonconformingProduct entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Запись не найдена с ID: " + id));
        return toResponse(entity);
    }

    public Page<NonconformingProductResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toResponse);
    }

    public Page<NonconformingProductResponse> findByFilters(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long productionSiteId,
            Long defectTypeId,
            Pageable pageable) {

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int startRow = pageNumber * pageSize;
        int endRow = startRow + pageSize;

        List<NonconformingProduct> content = repository.findWithFiltersNative(
                dateFrom, dateTo, productionSiteId, defectTypeId, startRow, endRow);

        long total = repository.countWithFilters(dateFrom, dateTo, productionSiteId, defectTypeId);

        List<NonconformingProductResponse> responses = content.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
    }


    private NonconformingProduct toEntity(NonconformingProductRequest request) {
        NonconformingProduct entity = new NonconformingProduct();

        entity.setDetectionDate(request.getDetectionDate());

        ProductionSite site = productionSiteRepository.findById(request.getProductionSiteId())
                .orElseThrow(() -> new RuntimeException("Участок не найден"));
        entity.setProductionSite(site);

        DetectionSource source = detectionSourceRepository.findById(request.getDetectionSourceId())
                .orElseThrow(() -> new RuntimeException("Источник выявления не найден"));
        entity.setDetectionSource(source);

        entity.setWeightTonnes(request.getWeightTonnes());
        entity.setIrreparableWeightTonnes(request.getIrreparableWeightTonnes());

        DefectType defectType = defectTypeRepository.findById(request.getDefectTypeId())
                .orElseThrow(() -> new RuntimeException("Вид несоответствия не найден"));
        entity.setDefectType(defectType);

        if (request.getDefectCauseId() != null) {
            DefectCause cause = defectCauseRepository.findById(request.getDefectCauseId())
                    .orElseThrow(() -> new RuntimeException("Причина не найдена"));
            entity.setDefectCause(cause);
        }

        if (request.getDefectSubcauseId() != null) {
            DefectCause subcause = defectCauseRepository.findById(request.getDefectSubcauseId())
                    .orElseThrow(() -> new RuntimeException("Подпричина не найдена"));
            entity.setDefectSubcause(subcause);
        }

        if (request.getReworkTypeId() != null) {
            ReworkType reworkType = reworkTypeRepository.findById(request.getReworkTypeId())
                    .orElseThrow(() -> new RuntimeException("Вид доработки не найден"));
            entity.setReworkType(reworkType);
        }

        entity.setNote(request.getNote());
        entity.setProductCode(request.getProductCode());
        entity.setReelNumber(request.getReelNumber());
        entity.setHeatNumber(request.getHeatNumber());
        entity.setManufacturerBrigade(request.getManufacturerBrigade());
        entity.setBundleNumber(request.getBundleNumber());
        entity.setManufacturerWorkshop(request.getManufacturerWorkshop());
        entity.setEquipmentKey(request.getEquipmentKey());
        entity.setReworkDate(request.getReworkDate());
        entity.setReworkWeightTonnes(request.getReworkWeightTonnes());

        return entity;
    }

    private void updateEntity(NonconformingProduct entity, NonconformingProductRequest request) {
        entity.setDetectionDate(request.getDetectionDate());

        if (request.getProductionSiteId() != null) {
            ProductionSite site = productionSiteRepository.findById(request.getProductionSiteId())
                    .orElseThrow(() -> new RuntimeException("Участок не найден"));
            entity.setProductionSite(site);
        }

        if (request.getDetectionSourceId() != null) {
            DetectionSource source = detectionSourceRepository.findById(request.getDetectionSourceId())
                    .orElseThrow(() -> new RuntimeException("Источник выявления не найден"));
            entity.setDetectionSource(source);
        }

        entity.setWeightTonnes(request.getWeightTonnes());
        entity.setIrreparableWeightTonnes(request.getIrreparableWeightTonnes());

        if (request.getDefectTypeId() != null) {
            DefectType defectType = defectTypeRepository.findById(request.getDefectTypeId())
                    .orElseThrow(() -> new RuntimeException("Вид несоответствия не найден"));
            entity.setDefectType(defectType);
        }

        if (request.getDefectCauseId() != null) {
            DefectCause cause = defectCauseRepository.findById(request.getDefectCauseId())
                    .orElseThrow(() -> new RuntimeException("Причина не найдена"));
            entity.setDefectCause(cause);
        } else {
            entity.setDefectCause(null);
        }

        if (request.getDefectSubcauseId() != null) {
            DefectCause subcause = defectCauseRepository.findById(request.getDefectSubcauseId())
                    .orElseThrow(() -> new RuntimeException("Подпричина не найдена"));
            entity.setDefectSubcause(subcause);
        } else {
            entity.setDefectSubcause(null);
        }

        if (request.getReworkTypeId() != null) {
            ReworkType reworkType = reworkTypeRepository.findById(request.getReworkTypeId())
                    .orElseThrow(() -> new RuntimeException("Вид доработки не найден"));
            entity.setReworkType(reworkType);
        } else {
            entity.setReworkType(null);
        }

        entity.setNote(request.getNote());
        entity.setProductCode(request.getProductCode());
        entity.setReelNumber(request.getReelNumber());
        entity.setHeatNumber(request.getHeatNumber());
        entity.setManufacturerBrigade(request.getManufacturerBrigade());
        entity.setBundleNumber(request.getBundleNumber());
        entity.setManufacturerWorkshop(request.getManufacturerWorkshop());
        entity.setEquipmentKey(request.getEquipmentKey());
        entity.setReworkDate(request.getReworkDate());
        entity.setReworkWeightTonnes(request.getReworkWeightTonnes());
    }

    private NonconformingProductResponse toResponse(NonconformingProduct entity) {
        return NonconformingProductResponse.builder()
                .id(entity.getId())
                .detectionDate(entity.getDetectionDate())
                .productionSiteName(entity.getProductionSite() != null ? entity.getProductionSite().getSiteName() : null)
                .detectionSourceName(entity.getDetectionSource() != null ? entity.getDetectionSource().getSourceName() : null)
                .weightTonnes(entity.getWeightTonnes())
                .irreparableWeightTonnes(entity.getIrreparableWeightTonnes())
                .defectTypeName(entity.getDefectType() != null ? entity.getDefectType().getDefectName() : null)
                .defectCauseName(entity.getDefectCause() != null ? entity.getDefectCause().getCauseName() : null)
                .defectSubcauseName(entity.getDefectSubcause() != null ? entity.getDefectSubcause().getCauseName() : null)
                .note(entity.getNote())
                .productCode(entity.getProductCode())
                .reelNumber(entity.getReelNumber())
                .heatNumber(entity.getHeatNumber())
                .manufacturerBrigade(entity.getManufacturerBrigade())
                .bundleNumber(entity.getBundleNumber())
                .manufacturerWorkshop(entity.getManufacturerWorkshop())
                .equipmentKey(entity.getEquipmentKey())
                .reworkDate(entity.getReworkDate())
                .reworkTypeName(entity.getReworkType() != null ? entity.getReworkType().getReworkName() : null)
                .reworkWeightTonnes(entity.getReworkWeightTonnes())
                .status(determineStatus(entity))
                .build();
    }

    private String determineStatus(NonconformingProduct entity) {
        if (entity.getIrreparableWeightTonnes() != null
                && entity.getIrreparableWeightTonnes().compareTo(BigDecimal.ZERO) > 0) {
            return "DEFECT";
        }
        if (entity.getReworkDate() != null) {
            return "REWORKED";
        }
        return "NOT_REWORKED";
    }
}