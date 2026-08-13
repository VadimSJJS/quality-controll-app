package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.service.EncodingFixService;
import com.vadimsjjs.qualitycontrollapp.service.SeedDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedDataController {

    private final SeedDataService seedService;
    private final EncodingFixService encodingFixService;

    @PostMapping("/test-data")
    public ResponseEntity<String> seedTestData() {
        try {
            seedService.seedTestData();
            return ResponseEntity.ok("Тестовые данные добавлены успешно");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        }
    }

    @PostMapping("/fix-encoding")
    public ResponseEntity<String> fixEncoding() {
        try {
            int fixed = encodingFixService.fixAll();
            return ResponseEntity.ok("Исправлено записей: " + fixed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        }
    }
}
