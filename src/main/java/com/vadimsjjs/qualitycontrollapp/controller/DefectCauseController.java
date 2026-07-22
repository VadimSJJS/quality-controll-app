package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.DefectCause;
import com.vadimsjjs.qualitycontrollapp.repository.DefectCauseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/directories/defect-causes")
@RequiredArgsConstructor
public class DefectCauseController {

    private final DefectCauseRepository repository;

    @GetMapping
    public List<DefectCause> getAll() {
        return repository.findByParentCauseIsNull();
    }

    @GetMapping("/{id}/subcauses")
    public List<DefectCause> getSubcauses(@PathVariable Long id) {
        return repository.findByParentCauseId(id);
    }
}