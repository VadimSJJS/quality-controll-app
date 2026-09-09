package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.AuthResponse;
import com.vadimsjjs.qualitycontrollapp.dto.LoginRequest;
import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import com.vadimsjjs.qualitycontrollapp.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PersonalRepository personalRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public Optional<Personal> authenticate(LoginRequest request) {
        Optional<Personal> user = personalRepository.findByPersonalNo(request.getPersonalNo());

        if (user.isPresent()) {
            Personal personal = user.get();
            String key = String.valueOf(personal.getPersonalNo());

            if (loginAttemptService.isLocked(key)) {
                long remainingTime = loginAttemptService.getLockRemainingTime(key);
                throw new RuntimeException("Учетная запись заблокирована. Осталось ждать: " + remainingTime + " мин.");
            }

            if (passwordEncoder.matches(request.getPassword(), personal.resolvePassword())) {
                loginAttemptService.loginSucceeded(key);
                return user;
            } else {
                loginAttemptService.loginFailed(key);

                if (loginAttemptService.isLocked(key)) {
                    throw new RuntimeException("Учетная запись заблокирована на 15 минут из-за слишком большого количества неудачных попыток ввода пароля.");
                }

                throw new RuntimeException("Неверный табельный номер или пароль");
            }
        }

        throw new RuntimeException("Неверный табельный номер или пароль");
    }

    public AuthResponse logout() {
        return AuthResponse.logout();
    }

    public AuthResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return AuthResponse.unauthorized();
        }

        String personalNoStr = authentication.getName();
        try {
            Long personalNo = Long.parseLong(personalNoStr);
            Personal personal = personalRepository.findByPersonalNo(personalNo)
                    .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

            return AuthResponse.success(
                    personalNo,
                    personal.getFio(),
                    java.util.List.of()
            );
        } catch (NumberFormatException e) {
            return AuthResponse.unauthorized();
        }
    }
}
