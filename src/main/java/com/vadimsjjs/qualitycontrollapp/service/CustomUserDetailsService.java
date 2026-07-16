package com.vadimsjjs.qualitycontrollapp.security;

import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import com.vadimsjjs.qualitycontrollapp.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final PersonalRepository personalRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Personal personal = personalRepository.findByPersonalNo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        return User.builder()
                .username(String.valueOf(personal.getPersonalNo()))
                .password(personal.getPassword() != null ? personal.getPassword() : "")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + personal.resolveRoleName())))
                .disabled(!personal.isActive())
                .build();
    }
}