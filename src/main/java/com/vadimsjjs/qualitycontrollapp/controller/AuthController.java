package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.dto.AuthResponse;
import com.vadimsjjs.qualitycontrollapp.dto.LoginRequest;
import com.vadimsjjs.qualitycontrollapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Неверный табельный номер или пароль"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Внутренняя ошибка сервера"));
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