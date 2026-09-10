package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.Diameter;
import com.vadimsjjs.qualitycontrollapp.repository.DiameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/directories")
@RequiredArgsConstructor
public class DiameterController {

    private final DiameterRepository diameterRepository;

    @GetMapping("/diameters")
    @PreAuthorize("hasAnyRole('OTK_MASTER', 'OTK', 'OTK_CHIEF', 'ADMIN', 'PPB', 'VIEWER')")
    public ResponseEntity<List<Diameter>> getAllDiameters() {
        List<Diameter> diameters = diameterRepository.findAllByOrderByDiameter();
        return ResponseEntity.ok(diameters);
    }
}
