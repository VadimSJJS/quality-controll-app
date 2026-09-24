package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.service.EncodingFixService;
import com.vadimsjjs.qualitycontrollapp.service.SeedDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@roleChecker.isAdmin()")
    public ResponseEntity<String> seedTestData() {
        try {
            seedService.seedTestData();
            return ResponseEntity.ok("РўРµСЃС‚РѕРІС‹Рµ РґР°РЅРЅС‹Рµ РґРѕР±Р°РІР»РµРЅС‹ СѓСЃРїРµС€РЅРѕ");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("РћС€РёР±РєР°: " + e.getMessage());
        }
    }

    @PostMapping("/fix-encoding")
    @PreAuthorize("@roleChecker.isAdmin()")
    public ResponseEntity<String> fixEncoding() {
        try {
            int fixed = encodingFixService.fixAll();
            return ResponseEntity.ok("РСЃРїСЂР°РІР»РµРЅРѕ Р·Р°РїРёСЃРµР№: " + fixed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("РћС€РёР±РєР°: " + e.getMessage());
        }
    }
}
