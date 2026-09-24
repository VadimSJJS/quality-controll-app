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
    @PreAuthorize("@roleChecker.canView()")
    public ResponseEntity<List<SteelGrade>> getSteelGrades() {
        List<SteelGrade> grades = steelGradeRepository.findAllOrderBySteelGrade();
        return ResponseEntity.ok(grades);
    }

    @GetMapping("/test-steel")
    @PreAuthorize("@roleChecker.canView()")
    public ResponseEntity<String> testSteel() {
        try {
            List<SteelGrade> grades = steelGradeRepository.findAllOrderBySteelGrade();
            return ResponseEntity.ok("РќР°Р№РґРµРЅРѕ: " + grades.size() + " Р·Р°РїРёСЃРµР№");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("РћС€РёР±РєР°: " + e.getMessage());
        }
    }
}