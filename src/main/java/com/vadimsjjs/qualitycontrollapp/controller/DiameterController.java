package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.Diameter;
import com.vadimsjjs.qualitycontrollapp.repository.DiameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/directories")
@RequiredArgsConstructor
public class DiameterController {

    private final DiameterRepository diameterRepository;

    @GetMapping("/diameters")
    public ResponseEntity<List<Diameter>> getAllDiameters() {
        List<Diameter> diameters = diameterRepository.findAllByOrderByDiameter();
        log.info("Загружено диаметров: {}", diameters.size());
        diameters.forEach(d -> log.info("  ID={}, DIAMETER={}", d.getId(), d.getDiameter()));
        return ResponseEntity.ok(diameters);
    }
}
