package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/directories/production-sites")
@RequiredArgsConstructor
public class ProductionSiteController {

    private final ProductionSiteRepository repository;

    @GetMapping
    @PreAuthorize("@roleChecker.canView()")
    public List<ProductionSite> getAll() {
        return repository.findAllByOrderBySiteCode();
    }
}
