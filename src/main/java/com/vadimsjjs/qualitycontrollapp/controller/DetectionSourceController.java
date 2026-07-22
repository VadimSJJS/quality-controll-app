package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.DetectionSource;
import com.vadimsjjs.qualitycontrollapp.repository.DetectionSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/directories/detection-sources")
@RequiredArgsConstructor
public class DetectionSourceController {

    private final DetectionSourceRepository repository;

    @GetMapping
    public List<DetectionSource> getAll() {
        return repository.findAll();
    }
}