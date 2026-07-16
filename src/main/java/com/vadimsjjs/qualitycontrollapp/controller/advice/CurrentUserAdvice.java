package com.vadimsjjs.qualitycontrollapp.controller.advice;

import com.vadimsjjs.qualitycontrollapp.repository.PersonalRepository;
import com.vadimsjjs.qualitycontrollapp.security.RoleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collection;

// контроллер который автоматически добавляет данные о текущем пользователя в каждый шаблон th
@ControllerAdvice
@RequiredArgsConstructor
public class CurrentUserAdvice {

    private final PersonalRepository personalRepository;

    @ModelAttribute
    public void addCurrentUserToModel(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated() || isAnonymous(authentication)) {
            return;
        }

        String personalNo = authentication.getName();

        personalRepository.findByPersonalNo(personalNo)
                .ifPresent(user -> {
                    model.addAttribute("fio", user.getFio());
                    model.addAttribute("personalNo", user.getPersonalNo());
                });

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        model.addAttribute("roles", RoleResolver.join(authorities));
        model.addAttribute("roleGroup", RoleResolver.resolveGroup(authorities));
        model.addAttribute("personalNo", personalNo);
    }

    private boolean isAnonymous(Authentication authentication) {
        return "anonymousUser".equals(authentication.getPrincipal());
    }
}