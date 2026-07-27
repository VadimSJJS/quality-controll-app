package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import com.vadimsjjs.qualitycontrollapp.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final PersonalRepository personalRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("=== loadUserByUsername вызван с username: '{}' ===", username);

        try {
            Long personalNo = Long.parseLong(username);

            Personal personal = personalRepository.findByPersonalNo(personalNo)
                    .orElseThrow(() -> {
                        log.warn("Пользователь с табельным {} НЕ НАЙДЕН", username);
                        return new UsernameNotFoundException("Пользователь с табельным номером " + username + " не найден");
                    });

            log.info("✅ Найден пользователь: PERSONAL_NO={}, FIO='{}', ROLE_NAME='{}', END_DATE={}",
                    personal.getPersonalNo(),
                    personal.getFio(),
                    personal.getRoleName(),
                    personal.getEndDate());

            if (!personal.isActive()) {
                log.warn("Срок действия пользователя {} истёк", username);
                throw new UsernameNotFoundException("Срок действия пользователя истёк");
            }

            return User.builder()
                    .username(username)
                    .password(personal.resolvePassword())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + personal.resolveRoleName())))
                    .disabled(!personal.isActive())
                    .build();

        } catch (NumberFormatException e) {
            log.warn("Неверный формат табельного номера: '{}'", username);
            throw new UsernameNotFoundException("Табельный номер должен содержать только цифры");
        }
    }
}