package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.Diameter;
import com.vadimsjjs.qualitycontrollapp.repository.DiameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directories")
@RequiredArgsConstructor
public class DiameterController {

    private final DiameterRepository diameterRepository;

    @GetMapping("/diameters")
    public ResponseEntity<List<Diameter>> getAllDiameters() {
        return ResponseEntity.ok(diameterRepository.findAllByOrderByDiameter());
    }
}
