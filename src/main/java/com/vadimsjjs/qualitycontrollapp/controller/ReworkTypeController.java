package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.ReworkType;
import com.vadimsjjs.qualitycontrollapp.repository.ReworkTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/directories/rework-types")
@RequiredArgsConstructor
public class ReworkTypeController {

    private final ReworkTypeRepository repository;

    @GetMapping
    @PreAuthorize("hasAnyRole('OTK_MASTER', 'OTK', 'OTK_CHIEF', 'ADMIN', 'PPB', 'VIEWER')")
    public List<ReworkType> getAll() {
        return repository.findAll();
    }
}