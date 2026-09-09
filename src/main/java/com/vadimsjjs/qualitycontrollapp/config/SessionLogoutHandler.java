package com.vadimsjjs.qualitycontrollapp.config;

import com.vadimsjjs.qualitycontrollapp.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionLogoutHandler implements LogoutHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            loginAttemptService.loginSucceeded(authentication.getName());
        }
    }
}
