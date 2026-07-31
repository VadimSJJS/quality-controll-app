package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.SteelGrade;
import com.vadimsjjs.qualitycontrollapp.repository.SteelGradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/directories")
@RequiredArgsConstructor
public class SteelGradeController {

    private final SteelGradeRepository steelGradeRepository;

    @GetMapping("/steel-grades")
    public ResponseEntity<List<SteelGrade>> getSteelGrades() {
        return ResponseEntity.ok(steelGradeRepository.findAllOrderBySteelGrade());
    }
}