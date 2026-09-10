package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.SteelGrade;
import com.vadimsjjs.qualitycontrollapp.repository.SteelGradeRepository;
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
public class SteelGradeController {

    private final SteelGradeRepository steelGradeRepository;

    @GetMapping("/steel-grades")
    @PreAuthorize("hasAnyRole('OTK_MASTER', 'OTK', 'OTK_CHIEF', 'ADMIN', 'PPB', 'VIEWER')")
    public ResponseEntity<List<SteelGrade>> getSteelGrades() {
        List<SteelGrade> grades = steelGradeRepository.findAllOrderBySteelGrade();
        return ResponseEntity.ok(grades);
    }

    @GetMapping("/test-steel")
    @PreAuthorize("hasAnyRole('OTK_MASTER', 'OTK', 'OTK_CHIEF', 'ADMIN', 'PPB', 'VIEWER')")
    public ResponseEntity<String> testSteel() {
        try {
            List<SteelGrade> grades = steelGradeRepository.findAllOrderBySteelGrade();
            return ResponseEntity.ok("Найдено: " + grades.size() + " записей");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }
}