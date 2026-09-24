package com.vadimsjjs.qualitycontrollapp.config;

import com.vadimsjjs.qualitycontrollapp.service.AuditService;
import com.vadimsjjs.qualitycontrollapp.service.CustomUserDetailsService;
import com.vadimsjjs.qualitycontrollapp.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;
    private final LoginFailureHandler loginFailureHandler;
    private final SessionLogoutHandler sessionLogoutHandler;

    // Публичные пути: страница входа, API входа и статика.
    // /api/seed/** сюда НЕ входит — тестовые данные доступны только администратору.
    private static final String[] PUBLIC_PATHS = {
            "/login", "/api/auth/login",
            "/css/**", "/js/**", "/fonts/**", "/lib/**", "/webjars/**", "/error", "/favicon.ico"
    };

    // Сессия 14 дней
    private static final int REMEMBER_ME_VALIDITY_SECONDS = 14 * 24 * 60 * 60;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .requiresChannel(channel -> channel
                        .requestMatchers(r -> r.getScheme().equals("http"))
                        .requiresSecure()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/defects/**").hasAnyRole("OTK_MASTER", "OTK", "OTK_CHIEF", "ADMIN", "PPB")
                        .requestMatchers("/reports/**").authenticated()
                        .requestMatchers("/directories/**").authenticated()
                        .requestMatchers("/charts/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler())
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired=true")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler())
                        .addLogoutHandler(sessionLogoutHandler)
                        .invalidateHttpSession(true)          // уничтожить HTTP-сессию на сервере
                        .deleteCookies("JSESSIONID", "remember-me")  // удалить куки
                        .clearAuthentication(true)             // очистить SecurityContext
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeServices(rememberMeServices())
                )
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new RestAwareAuthenticationEntryPoint())
                    .accessDeniedPage("/access-denied")
                )
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public TokenBasedRememberMeServices rememberMeServices() {
        String rememberMeKey = System.getenv("REMEMBER_ME_KEY");
        if (rememberMeKey == null || rememberMeKey.isBlank()) {
            rememberMeKey = "change-me-in-production";
        }
        TokenBasedRememberMeServices rememberMeServices =
                new TokenBasedRememberMeServices(rememberMeKey, userDetailsService);
        rememberMeServices.setTokenValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS);
        rememberMeServices.setCookieName("remember-me");
        rememberMeServices.setAlwaysRemember(false);
        return rememberMeServices;
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            try {
                Long personalNo = Long.parseLong(authentication.getName());
                auditService.logLogin(personalNo, getClientIp(request), request.getHeader("User-Agent"));
            } catch (Exception e) {
            }
            response.sendRedirect("/");
        };
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                try {
                    Long personalNo = Long.parseLong(authentication.getName());
                    auditService.logLogout(personalNo);
                } catch (Exception e) {
                }
            }
            response.sendRedirect("/login?logout=true");
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    public static class RestAwareAuthenticationEntryPoint
            implements org.springframework.security.web.AuthenticationEntryPoint {

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             org.springframework.security.core.AuthenticationException authException)
                throws IOException {
            String path = request.getRequestURI();
            if (path != null && path.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":401,\"message\":\"Сессия истекла. Войдите в систему повторно.\"}");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
        }
    }
}