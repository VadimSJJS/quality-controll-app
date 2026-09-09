package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.dto.AuthResponse;
import com.vadimsjjs.qualitycontrollapp.dto.LoginRequest;
import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import com.vadimsjjs.qualitycontrollapp.service.AuthService;
import com.vadimsjjs.qualitycontrollapp.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            Personal user = authService.authenticate(request)
                    .orElseThrow(() -> new RuntimeException("Неверный табельный номер или пароль"));

            AuthResponse response = AuthResponse.success(
                    user.getPersonalNo(),
                    user.getFio(),
                    java.util.List.of()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String message = e.getMessage();


            if (message.contains("Заблокирована")) {
                Long personalNo = request.getPersonalNo();
                if (personalNo != null && loginAttemptService.isLocked(String.valueOf(personalNo))) {
                    long remainingMinutes = loginAttemptService.getLockRemainingTime(String.valueOf(personalNo));
                    return ResponseEntity.status(429).body(AuthResponse.locked(message, remainingMinutes));
                }
            }

            return ResponseEntity.status(401).body(AuthResponse.error(message));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout() {
        return ResponseEntity.ok(authService.logout());
    }

    @GetMapping("/current")
    public ResponseEntity<AuthResponse> currentUser(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication));
    }
}
