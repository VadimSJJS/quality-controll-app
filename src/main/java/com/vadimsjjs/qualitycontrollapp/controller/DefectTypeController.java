package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.DefectType;
import com.vadimsjjs.qualitycontrollapp.repository.DefectTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/directories/defect-types")
@RequiredArgsConstructor
public class DefectTypeController {

    private final DefectTypeRepository repository;

    @GetMapping
    public List<DefectType> getAll() {
        return repository.findAll();
    }
}