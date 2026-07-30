package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.ProductionSite;
import com.vadimsjjs.qualitycontrollapp.repository.ProductionSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/directories/production-sites")
@RequiredArgsConstructor
public class ProductionSiteController {

    private final ProductionSiteRepository repository;

    private static final Map<String, Integer> SITE_ORDER = Map.of(
            "СК", 1,
            "ТУ", 2,
            "ГСВ", 3,
            "ТТГУ", 4,
            "УТВ_МК", 5,
            "УТВ_РМЛ", 6,
            "УТВ_СВ", 7,
            "КУ-1", 8,
            "КУ-2", 9
    );

    @GetMapping
    public List<ProductionSite> getAll() {
        List<ProductionSite> sites = repository.findAll();
        sites.sort(Comparator.comparingInt(s -> SITE_ORDER.getOrDefault(s.getSiteCode(), 999)));
        return sites;
    }
}