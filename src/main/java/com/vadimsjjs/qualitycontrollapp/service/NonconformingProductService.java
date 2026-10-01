package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.NonconformingProductRequest;
import com.vadimsjjs.qualitycontrollapp.dto.NonconformingProductResponse;
import com.vadimsjjs.qualitycontrollapp.dto.ReworkActionRequest;
import com.vadimsjjs.qualitycontrollapp.dto.ReworkActionResponse;
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

/**
 * Основная бизнес-логика учёта несоответствующей продукции: создание, изменение,
 * удаление и выборка записей с фильтрами из ТЗ (даты, участок, дефект, причина, подпричина,
 * диаметр, конструкция металлокорда, код, плавка, марка стали, оборудование, оператор, бригада).
 *
 * <p>Все изменения записываются в журнал аудита через {@link com.vadimsjjs.qualitycontrollapp.aspect.AuditAspect}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NonconformingProductService {

    /** Коды видов доработки в действиях: 1 — восстановлено, 2 — переназначено, 3 — брак. */
    public static final int REWORK_RESTORED = 1;
    public static final int REWORK_REASSIGNED = 2;
    public static final int REWORK_SCRAPPED = 3;

    private final NonconformingProductRepository repository;
    private final ProductionSiteRepository productionSiteRepository;
    private final DetectionSourceRepository detectionSourceRepository;
    private final DefectTypeRepository defectTypeRepository;
    private final DefectCauseRepository defectCauseRepository;
    private final ReworkTypeRepository reworkTypeRepository;
    private final DiameterRepository diameterRepository;
    private final SteelGradeRepository steelGradeRepository;

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

    /**
     * Определяет вид несоответствия для записи.
     *
     * <p>Порядок: если пришёл идентификатор — берём его. Иначе работаем с названием,
     * которое пользователь напечатал вручную: ищем в справочнике без учёта регистра,
     * а если не нашли — добавляем новое значение в справочник. Так пользователь может
     * вводить новые дефекты прямо при регистрации продукции, не заходя в справочник.
     */
    private DefectType resolveDefectType(NonconformingProductRequest request) {
        if (request.getDefectTypeId() != null) {
            return defectTypeRepository.findById(request.getDefectTypeId())
                    .orElseThrow(() -> new RuntimeException("Вид несоответствия не найден"));
        }

        String name = request.getDefectTypeName();
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Укажите вид несоответствия");
        }

        String trimmed = name.trim();
        return defectTypeRepository.findByDefectNameIgnoreCase(trimmed)
                .orElseGet(() -> createDefectTypeOnTheFly(trimmed));
    }

    private DefectType createDefectTypeOnTheFly(String name) {
        DefectType created = new DefectType();
        created.setDefectName(name);
        created.setReworkable(Boolean.TRUE);
        created.setDefectCode(nextDefectCode());
        DefectType saved = defectTypeRepository.save(created);
        log.info("Добавлен новый вид несоответствия в справочник: '{}' (код {})", name, saved.getDefectCode());
        return saved;
    }

    /** Следующий свободный числовой код вида дефекта (001, 002, ...). */
    private String nextDefectCode() {
        long max = defectTypeRepository.findAll().stream()
                .map(DefectType::getDefectCode)
                .filter(code -> code != null && code.matches("\\d+"))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0L);
        return String.format("%03d", max + 1);
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
            Long defectCauseId,
            Long defectSubcauseId,
            Long diameterId,
            String steelCordConstruction,
            Long productCode,
            String heatNumber,
            String steelGrade,
            String equipmentKey,
            Long operatorPersonalNumber,
            Long manufacturerBrigade,
            Long detectionSourceId,
            Pageable pageable) {

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int startRow = pageNumber * pageSize;
        int endRow = startRow + pageSize;

        List<NonconformingProduct> content = repository.findWithFiltersNative(
                dateFrom, dateTo, productionSiteId, defectTypeId, defectCauseId, defectSubcauseId,
                diameterId, steelCordConstruction, productCode, heatNumber, steelGrade,
                equipmentKey, operatorPersonalNumber, manufacturerBrigade, detectionSourceId, startRow, endRow);

        long total = repository.countWithFilters(
                dateFrom, dateTo, productionSiteId, defectTypeId, defectCauseId, defectSubcauseId,
                diameterId, steelCordConstruction, productCode, heatNumber, steelGrade,
                equipmentKey, operatorPersonalNumber, manufacturerBrigade, detectionSourceId);

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

        DefectType defectType = resolveDefectType(request);
        entity.setDefectType(defectType);

        if (request.getDefectCauseId() != null) {
            DefectCause cause = defectCauseRepository.findById(request.getDefectCauseId())
                    .orElseThrow(() -> new RuntimeException("Причина не найдена"));
            entity.setDefectCause(cause);
        } else if (hasText(request.getDefectCauseName())) {
            entity.setDefectCause(resolveOrCreateCause(request.getDefectCauseName(), null));
        }

        if (request.getDefectSubcauseId() != null) {
            DefectCause subcause = defectCauseRepository.findById(request.getDefectSubcauseId())
                    .orElseThrow(() -> new RuntimeException("Подпричина не найдена"));
            entity.setDefectSubcause(subcause);
        } else if (hasText(request.getDefectSubcauseName())) {
            if (entity.getDefectCause() == null) {
                throw new RuntimeException("Сначала укажите причину, потом подпричину");
            }
            entity.setDefectSubcause(resolveOrCreateCause(request.getDefectSubcauseName(), entity.getDefectCause()));
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
        if (request.getOperatorPersonalNumber() != null) {
            entity.setOperatorPersonalNumber(request.getOperatorPersonalNumber());
        }

        if (request.getDiameterId() != null) {
            Diameter diameter = diameterRepository.findById(request.getDiameterId())
                    .orElseThrow(() -> new RuntimeException("Диаметр не найден"));
            entity.setDiameter(diameter);
        }

        if (request.getSteelGradeId() != null) {
            SteelGrade steelGrade = steelGradeRepository.findById(request.getSteelGradeId())
                    .orElseThrow(() -> new RuntimeException("Марка стали не найдена"));
            entity.setSteelGrade(steelGrade.getSteelGrade());
        }

        entity.setUnitNumber(request.getUnitNumber());
        entity.setQuantity(request.getQuantity());
        entity.setReworkQuantity(request.getReworkQuantity());
        entity.setWorkpieceKey(request.getWorkpieceKey());
        entity.setSteelCordConstruction(request.getSteelCordConstruction());
        entity.setBrigade(request.getBrigade());

        entity.setReworkDate(request.getReworkDate());
        applyReworkResults(entity, request);
        applyReworkActions(entity, request);
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
        } else if (hasText(request.getDefectCauseName())) {
            entity.setDefectCause(resolveOrCreateCause(request.getDefectCauseName(), null));
        } else {
            entity.setDefectCause(null);
        }

        if (request.getDefectSubcauseId() != null) {
            DefectCause subcause = defectCauseRepository.findById(request.getDefectSubcauseId())
                    .orElseThrow(() -> new RuntimeException("Подпричина не найдена"));
            entity.setDefectSubcause(subcause);
        } else if (hasText(request.getDefectSubcauseName())) {
            if (entity.getDefectCause() == null) {
                throw new RuntimeException("Сначала укажите причину, потом подпричину");
            }
            entity.setDefectSubcause(resolveOrCreateCause(request.getDefectSubcauseName(), entity.getDefectCause()));
        } else {
            entity.setDefectSubcause(null);
        }

        if (request.getEquipmentKey() != null) {
            entity.setEquipmentKey(request.getEquipmentKey());
        }
        if (request.getOperatorPersonalNumber() != null) {
            entity.setOperatorPersonalNumber(request.getOperatorPersonalNumber());
        }

        if (request.getReworkTypeId() != null) {
            ReworkType reworkType = reworkTypeRepository.findById(request.getReworkTypeId())
                    .orElseThrow(() -> new RuntimeException("Вид доработки не найден"));
            entity.setReworkType(reworkType);
        } else {
            entity.setReworkType(null);
        }

        if (request.getDiameterId() != null) {
            Diameter diameter = diameterRepository.findById(request.getDiameterId())
                    .orElseThrow(() -> new RuntimeException("Диаметр не найден"));
            entity.setDiameter(diameter);
        } else {
            entity.setDiameter(null);
        }

        if (request.getSteelGradeId() != null) {
            SteelGrade steelGrade = steelGradeRepository.findById(request.getSteelGradeId())
                    .orElseThrow(() -> new RuntimeException("Марка стали не найдена"));
            entity.setSteelGrade(steelGrade.getSteelGrade());
        } else {
            entity.setSteelGrade(null);
        }

        entity.setUnitNumber(request.getUnitNumber());
        entity.setQuantity(request.getQuantity());
        entity.setReworkQuantity(request.getReworkQuantity());
        entity.setWorkpieceKey(request.getWorkpieceKey());
        entity.setSteelCordConstruction(request.getSteelCordConstruction());
        entity.setBrigade(request.getBrigade());

        entity.setNote(request.getNote());
        entity.setProductCode(request.getProductCode());
        entity.setReelNumber(request.getReelNumber());
        entity.setHeatNumber(request.getHeatNumber());
        entity.setManufacturerBrigade(request.getManufacturerBrigade());
        entity.setBundleNumber(request.getBundleNumber());
        entity.setManufacturerWorkshop(request.getManufacturerWorkshop());
        entity.setEquipmentKey(request.getEquipmentKey());
        entity.setReworkDate(request.getReworkDate());
        applyReworkResults(entity, request);
        applyReworkActions(entity, request);
    }

    /**
     * Распределяет задержанную массу между тремя результатами доработки.
     *
     * <p>REWORK_WEIGHT_TONNES остаётся итогом «восстановлено + переназначено» —
     * на него опираются существующие отчёты, поэтому обратная совместимость сохраняется.
     * Проверка «не больше задержанного» выполняется в DTO, здесь — страховка для
     * случаев вызова сервиса напрямую.
     */
    private void applyReworkResults(NonconformingProduct entity, NonconformingProductRequest request) {
        BigDecimal restored = nz(request.getRestoredWeightTonnes());
        BigDecimal reassigned = nz(request.getReassignedWeightTonnes());
        BigDecimal scrapped = nz(request.getScrappedWeightTonnes());

        BigDecimal totalResult = restored.add(reassigned).add(scrapped);
        if (totalResult.signum() > 0 && request.getWeightTonnes() != null
                && totalResult.compareTo(request.getWeightTonnes()) > 0) {
            throw new RuntimeException("Сумма восстановлено + переназначено + брак ("
                    + totalResult.stripTrailingZeros().toPlainString()
                    + " т) превышает задержанную массу ("
                    + request.getWeightTonnes().stripTrailingZeros().toPlainString() + " т)");
        }

        entity.setRestoredWeightTonnes(totalResult.signum() == 0 ? null : restored);
        entity.setReassignedWeightTonnes(totalResult.signum() == 0 ? null : reassigned);
        entity.setReworkWeightTonnes(restored.add(reassigned));

        if (totalResult.signum() > 0 && request.getScrappedWeightTonnes() != null) {
            entity.setIrreparableWeightTonnes(scrapped);
        }
    }

    /**
     * Сохраняет отдельные действия по доработке записи и пересчитывает итоги записи
     * по этим действиям.
     *
     * <p>Действия — источник истины по «как именно доработана продукция»: восстановлено,
     * переназначено, неисправимый брак. Итоговые поля записи приводятся в соответствие
     * с действиями, чтобы отчёты и сводные таблицы считали по факту.
     *
     * <p>Если действий нет, ничего не меняется — работают три поля записи.
     */
    private void applyReworkActions(NonconformingProduct entity, NonconformingProductRequest request) {
        List<ReworkActionRequest> actions = request.getReworkActions();
        if (actions == null || actions.isEmpty()) {
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal restored = BigDecimal.ZERO;
        BigDecimal reassigned = BigDecimal.ZERO;
        BigDecimal scrapped = BigDecimal.ZERO;
        LocalDate lastDate = null;

        for (ReworkActionRequest actionRequest : actions) {
            if (actionRequest.getActionDate() == null || actionRequest.getWeightTonnes() == null) {
                continue;
            }
            BigDecimal weight = actionRequest.getWeightTonnes();
            if (weight.signum() <= 0) {
                continue;
            }
            total = total.add(weight);
            int type = actionRequest.getActionType() == null ? 0 : actionRequest.getActionType();
            if (type == REWORK_RESTORED) {
                restored = restored.add(weight);
            } else if (type == REWORK_REASSIGNED) {
                reassigned = reassigned.add(weight);
            } else if (type == REWORK_SCRAPPED) {
                scrapped = scrapped.add(weight);
            }
            if (lastDate == null || actionRequest.getActionDate().isAfter(lastDate)) {
                lastDate = actionRequest.getActionDate();
            }
        }

        if (total.signum() == 0) {
            return;
        }
        if (request.getWeightTonnes() != null && total.compareTo(request.getWeightTonnes()) > 0) {
            throw new RuntimeException("Сумма доработок ("
                    + total.stripTrailingZeros().toPlainString()
                    + " т) превышает задержанную массу ("
                    + request.getWeightTonnes().stripTrailingZeros().toPlainString() + " т)");
        }

        entity.getReworkActions().clear();
        for (ReworkActionRequest actionRequest : actions) {
            if (actionRequest.getActionDate() == null || actionRequest.getWeightTonnes() == null
                    || actionRequest.getWeightTonnes().signum() <= 0) {
                continue;
            }
            ReworkAction action = new ReworkAction();
            action.setNonconformingProduct(entity);
            action.setActionDate(actionRequest.getActionDate());
            action.setActionType(actionRequest.getActionType() == null ? REWORK_RESTORED : actionRequest.getActionType());
            action.setWeightTonnes(actionRequest.getWeightTonnes());
            action.setNote(actionRequest.getNote());
            entity.getReworkActions().add(action);
        }

        entity.setRestoredWeightTonnes(restored.signum() == 0 ? null : restored);
        entity.setReassignedWeightTonnes(reassigned.signum() == 0 ? null : reassigned);
        entity.setReworkWeightTonnes(restored.add(reassigned));
        entity.setIrreparableWeightTonnes(scrapped.signum() == 0 ? null : scrapped);
        if (lastDate != null) {
            entity.setReworkDate(lastDate);
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Ищет причину по названию без учёта регистра, а если не нашла — добавляет в справочник.
     * Работает так же, как свободный ввод вида дефекта.
     *
     * @param parentCause родительская причина; null — создаётся причина верхнего уровня
     */
    private DefectCause resolveOrCreateCause(String name, DefectCause parentCause) {
        String trimmed = name.trim();
        List<DefectCause> found = parentCause == null
                ? defectCauseRepository.findByCauseNameIgnoreCase(trimmed)
                : defectCauseRepository.findByParentAndNameIgnoreCase(parentCause.getId(), trimmed);

        DefectCause existing = found.stream()
                .filter(c -> parentCause == null ? c.getParentCause() == null : parentCause.equals(c.getParentCause()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        DefectCause created = new DefectCause();
        created.setCauseName(trimmed);
        created.setCauseCode(nextCauseCode(parentCause));
        created.setParentCause(parentCause);
        DefectCause saved = defectCauseRepository.save(created);
        log.info("Добавлена новая {} в справочник: '{}' (код {})",
                parentCause == null ? "причина" : "подпричина", trimmed, saved.getCauseCode());
        return saved;
    }

    /** Следующий свободный код причины: для причины C09, для подпричины C09-01. */
    private String nextCauseCode(DefectCause parentCause) {
        if (parentCause != null) {
            long max = defectCauseRepository.findByParentCauseId(parentCause.getId()).stream()
                    .map(DefectCause::getCauseCode)
                    .filter(code -> code != null && code.matches(parentCause.getCauseCode() + "-\\d+"))
                    .mapToLong(code -> Long.parseLong(code.substring(code.lastIndexOf('-') + 1)))
                    .max()
                    .orElse(0L);
            return parentCause.getCauseCode() + "-" + String.format("%02d", max + 1);
        }
        long max = defectCauseRepository.findByParentCauseIsNull().stream()
                .map(DefectCause::getCauseCode)
                .filter(code -> code != null && code.matches("C\\d+"))
                .mapToLong(code -> Long.parseLong(code.substring(1)))
                .max()
                .orElse(0L);
        return String.format("C%02d", max + 1);
    }

    /** Название вида доработки для кода действия из справочника. */
    private String actionTypeName(Integer actionType) {
        if (actionType == null) {
            return null;
        }
        return reworkTypeRepository.findById(actionType.longValue())
                .map(ReworkType::getReworkName)
                .orElse(null);
    }

    /** Отдельные действия по доработке в виде ответа API. */
    private List<ReworkActionResponse> toActionResponses(NonconformingProduct entity) {
        if (entity.getReworkActions() == null || entity.getReworkActions().isEmpty()) {
            return List.of();
        }
        return entity.getReworkActions().stream()
                .map(a -> ReworkActionResponse.builder()
                        .id(a.getId())
                        .actionDate(a.getActionDate())
                        .actionType(a.getActionType())
                        .actionTypeName(actionTypeName(a.getActionType()))
                        .weightTonnes(a.getWeightTonnes())
                        .note(a.getNote())
                        .build())
                .collect(Collectors.toList());
    }

    private NonconformingProductResponse toResponse(NonconformingProduct entity) {
        return NonconformingProductResponse.builder()
                .id(entity.getId())
                .detectionDate(entity.getDetectionDate())
                .productionSiteId(entity.getProductionSite() != null ? entity.getProductionSite().getId() : null)
                .productionSiteName(entity.getProductionSite() != null ? entity.getProductionSite().getSiteName() : null)
                .detectionSourceId(entity.getDetectionSource() != null ? entity.getDetectionSource().getId() : null)
                .detectionSourceName(entity.getDetectionSource() != null ? entity.getDetectionSource().getSourceName() : null)
                .defectTypeId(entity.getDefectType() != null ? entity.getDefectType().getId() : null)
                .defectTypeName(entity.getDefectType() != null ? entity.getDefectType().getDefectName() : null)
                .defectCauseId(entity.getDefectCause() != null ? entity.getDefectCause().getId() : null)
                .defectCauseName(entity.getDefectCause() != null ? entity.getDefectCause().getCauseName() : null)
                .defectSubcauseId(entity.getDefectSubcause() != null ? entity.getDefectSubcause().getId() : null)
                .defectSubcauseName(entity.getDefectSubcause() != null ? entity.getDefectSubcause().getCauseName() : null)
                .reworkTypeId(entity.getReworkType() != null ? entity.getReworkType().getId() : null)
                .reworkTypeName(entity.getReworkType() != null ? entity.getReworkType().getReworkName() : null)
                .weightTonnes(entity.getWeightTonnes())
                .irreparableWeightTonnes(entity.getIrreparableWeightTonnes())
                .note(entity.getNote())
                .productCode(entity.getProductCode())
                .reelNumber(entity.getReelNumber())
                .heatNumber(entity.getHeatNumber())
                .manufacturerBrigade(entity.getManufacturerBrigade())
                .bundleNumber(entity.getBundleNumber())
                .manufacturerWorkshop(entity.getManufacturerWorkshop())
                .equipmentKey(entity.getEquipmentKey())
                .diameterId(entity.getDiameter() != null ? entity.getDiameter().getId() : null)
                .diameterValue(entity.getDiameter() != null ? entity.getDiameter().getDiameter() : null)
                .operatorPersonalNumber(entity.getOperatorPersonalNumber())
                .reworkDate(entity.getReworkDate())
                .reworkWeightTonnes(entity.getReworkWeightTonnes())
                .restoredWeightTonnes(entity.getRestoredWeightTonnes())
                .reassignedWeightTonnes(entity.getReassignedWeightTonnes())
                .scrappedWeightTonnes(entity.getIrreparableWeightTonnes())
                .status(determineStatus(entity))
                .steelGradeId(entity.getSteelGrade() != null ? resolveSteelGradeId(entity.getSteelGrade()) : null)
                .steelGrade(entity.getSteelGrade())
                .unitNumber(entity.getUnitNumber())
                .quantity(entity.getQuantity())
                .reworkQuantity(entity.getReworkQuantity())
                .workpieceKey(entity.getWorkpieceKey())
                .steelCordConstruction(entity.getSteelCordConstruction())
                .brigade(entity.getBrigade())
                .reworkActions(toActionResponses(entity))
                .build();
    }

    private Long resolveSteelGradeId(String steelGrade) {
        if (steelGrade == null) return null;
        return steelGradeRepository.findAll().stream()
                .filter(s -> steelGrade.equals(s.getSteelGrade()))
                .map(SteelGrade::getId)
                .findFirst()
                .orElse(null);
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