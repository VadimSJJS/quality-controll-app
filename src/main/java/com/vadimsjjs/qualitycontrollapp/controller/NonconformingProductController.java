package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.dto.NonconformingProductRequest;
import com.vadimsjjs.qualitycontrollapp.dto.NonconformingProductResponse;
import com.vadimsjjs.qualitycontrollapp.service.NonconformingProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/defects")
@RequiredArgsConstructor
public class NonconformingProductController {

    private final NonconformingProductService service;

    @GetMapping
    public ResponseEntity<Page<NonconformingProductResponse>> getAll(
            @PageableDefault(size = 20, sort = "detectionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<NonconformingProductResponse>> filter(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long productionSiteId,
            @RequestParam(required = false) Long defectTypeId,
            @PageableDefault(size = 20, sort = "detectionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NonconformingProductResponse> result = service.findByFilters(
                dateFrom, dateTo, productionSiteId, defectTypeId, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NonconformingProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OTK', 'ADMIN', 'PPB')")
    public ResponseEntity<NonconformingProductResponse> create(@Valid @RequestBody NonconformingProductRequest request) {
        NonconformingProductResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OTK', 'ADMIN', 'PPB')")
    public ResponseEntity<NonconformingProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NonconformingProductRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OTK', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}