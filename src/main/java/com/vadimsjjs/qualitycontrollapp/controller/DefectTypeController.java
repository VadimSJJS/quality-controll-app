package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.dto.DefectTypeRequest;
import com.vadimsjjs.qualitycontrollapp.entity.DefectType;
import com.vadimsjjs.qualitycontrollapp.repository.DefectTypeRepository;
import com.vadimsjjs.qualitycontrollapp.repository.NonconformingProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Справочник видов несоответствия (HLP_DEFECT_TYPE).
 *
 * <p>Пользователи сами ведут справочник: добавляют, изменяют и удаляют виды дефектов,
 * а при регистрации продукции выбирают значение из этого справочника.
 * Значения, добавленные прямо в форме ввода продукции, попадают сюда же.
 */
@RestController
@RequestMapping("/directories/defect-types")
@RequiredArgsConstructor
public class DefectTypeController {

    private final DefectTypeRepository repository;
    private final NonconformingProductRepository nonconformingProductRepository;

    @GetMapping
    @PreAuthorize("@roleChecker.canView()")
    public List<DefectType> getAll() {
        return repository.findAll(Sort.by("defectName"));
    }

    /**
     * Подсказки при вводе названия дефекта.
     * GET /directories/defect-types/search?q=намо
     */
    @GetMapping("/search")
    @PreAuthorize("@roleChecker.canView()")
    public List<DefectType> search(@RequestParam(name = "q") String query) {
        String fragment = query == null ? "" : query.trim();
        if (fragment.isEmpty()) {
            return List.of();
        }
        return repository.findTop50ByDefectNameContainingIgnoreCaseOrderByDefectNameAsc(fragment);
    }

    @PostMapping
    @PreAuthorize("@roleChecker.canEdit()")
    public ResponseEntity<DefectType> create(@Valid @RequestBody DefectTypeRequest request) {
        String name = request.getDefectName().trim();
        repository.findByDefectNameIgnoreCase(name).ifPresent(existing -> {
            throw new IllegalStateException("Вид дефекта «" + name + "» уже есть в справочнике");
        });

        DefectType entity = new DefectType();
        entity.setDefectName(name);
        entity.setDefectCode(resolveCode(request.getDefectCode()));
        entity.setReworkable(request.getReworkable() != null ? request.getReworkable() : Boolean.TRUE);

        DefectType saved = repository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@roleChecker.canEdit()")
    public DefectType update(@PathVariable Long id, @Valid @RequestBody DefectTypeRequest request) {
        DefectType entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Вид дефекта не найден"));

        String name = request.getDefectName().trim();
        repository.findByDefectNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException("Вид дефекта «" + name + "» уже есть в справочнике");
                });

        entity.setDefectName(name);
        if (request.getDefectCode() != null && !request.getDefectCode().isBlank()) {
            entity.setDefectCode(request.getDefectCode().trim());
        }
        if (request.getReworkable() != null) {
            entity.setReworkable(request.getReworkable());
        }
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@roleChecker.canDelete()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        DefectType entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Вид дефекта не найден"));

        if (nonconformingProductRepository.existsByDefectType_Id(id)) {
            throw new IllegalStateException(
                    "Нельзя удалить: вид дефекта «" + entity.getDefectName()
                            + "» уже используется в записях о несоответствующей продукции");
        }

        repository.delete(entity);
        return ResponseEntity.noContent().build();
    }

    /**
     * Код необязателен: если не задан, берём следующий свободный числовой.
     */
    private String resolveCode(String requestedCode) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            return requestedCode.trim();
        }
        long max = repository.findAll().stream()
                .map(DefectType::getDefectCode)
                .filter(code -> code != null && code.matches("\\d+"))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0L);
        return String.format("%03d", max + 1);
    }
}
