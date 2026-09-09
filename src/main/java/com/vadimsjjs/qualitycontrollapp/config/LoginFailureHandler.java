package com.vadimsjjs.qualitycontrollapp.config;

import com.vadimsjjs.qualitycontrollapp.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String usernameParam = request.getParameter("username");

        if (usernameParam != null && !usernameParam.isBlank()) {
            String key;
            try {
                Long personalNo = Long.parseLong(usernameParam.trim());
                key = String.valueOf(personalNo);
            } catch (NumberFormatException e) {
                key = usernameParam;
            }

            loginAttemptService.loginFailed(key);

            if (loginAttemptService.isLocked(key)) {
                long remainingMinutes = loginAttemptService.getLockRemainingTime(key);
                String redirectUrl = "/login?blocked=true&minutes=" + remainingMinutes;
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                return;
            }
        }

        getRedirectStrategy().sendRedirect(request, response, "/login?error=true");
    }
}
