package com.vadimsjjs.qualitycontrollapp.controller.advice;

import com.vadimsjjs.qualitycontrollapp.entity.Personal;
import com.vadimsjjs.qualitycontrollapp.repository.PersonalRepository;
import com.vadimsjjs.qualitycontrollapp.security.RoleChecker;
import com.vadimsjjs.qualitycontrollapp.security.RoleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collection;

@ControllerAdvice
@RequiredArgsConstructor
public class CurrentUserAdvice {

    private final PersonalRepository personalRepository;
    private final RoleChecker roleChecker;

    @ModelAttribute
    public void addCurrentUserToModel(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated() || isAnonymous(authentication)) {
            return;
        }

        String personalNoStr = authentication.getName();

        try {
            Long personalNo = Long.parseLong(personalNoStr);

            personalRepository.findByPersonalNo(personalNo)
                    .ifPresent(user -> {
                        model.addAttribute("fio", user.getFio());
                        model.addAttribute("personalNo", user.getPersonalNo());
                        model.addAttribute("password", user.getPassword());
                    });

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            model.addAttribute("roles", RoleResolver.join(authorities));
            model.addAttribute("roleGroup", RoleResolver.resolveGroup(authorities));
            model.addAttribute("personalNo", personalNo);

            // Флаги прав для скрытия недоступных элементов интерфейса
            model.addAttribute("canEdit", roleChecker.canEdit());
            model.addAttribute("canDelete", roleChecker.canDelete());
            model.addAttribute("isAdmin", roleChecker.isAdmin());

        } catch (NumberFormatException e) {
            // # EMPTY BLOCK
        }
    }

    private boolean isAnonymous(Authentication authentication) {
        return "anonymousUser".equals(authentication.getPrincipal());
    }
}