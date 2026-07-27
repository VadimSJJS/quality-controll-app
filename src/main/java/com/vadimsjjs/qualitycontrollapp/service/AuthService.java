package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.AuthResponse;
import com.vadimsjjs.qualitycontrollapp.dto.LoginRequest;
import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import com.vadimsjjs.qualitycontrollapp.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PersonalRepository personalRepository;

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getPersonalNo().toString(),
                        request.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Personal personal = personalRepository.findByPersonalNo(request.getPersonalNo())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        return AuthResponse.success(
                request.getPersonalNo(),
                personal.getFio(),
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }

    public AuthResponse logout() {
        SecurityContextHolder.clearContext();
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
                    authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
            );
        } catch (NumberFormatException e) {
            log.error("Неверный формат табельного номера: {}", personalNoStr);
            return AuthResponse.unauthorized();
        }
    }
}