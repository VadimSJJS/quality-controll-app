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
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;

import java.io.IOException;

import java.util.List;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;
    private final LoginFailureHandler loginFailureHandler;
    private final SessionLogoutHandler sessionLogoutHandler;
    private final DataSource dataSource;

    private static final String[] PUBLIC_PATHS = {
            "/login", "/css/**", "/js/**", "/fonts/**", "/webjars/**", "/error", "/api/seed/**", "/lib/**"
    };

    private static final List<String> OTK_ROLES = List.of("OTK_MASTER", "OTK", "OTK_CHIEF");
    private static final List<String> EDIT_ROLES = List.of("ADMIN", "PPB");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                        .logoutSuccessUrl("/login?logout")
                        .addLogoutHandler(sessionLogoutHandler)
                        .invalidateHttpSession(true)          // уничтожить HTTP-сессию на сервере
                        .deleteCookies("JSESSIONID", "remember-me")  // удалить куки
                        .clearAuthentication(true)             // очистить SecurityContext
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeServices(rememberMeServices())
                        .key("uniqueAndSecretKeyForRememberMe")
                        .tokenValiditySeconds(1209600) // 14 дней
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
        rememberMeServices.setTokenValiditySeconds(1209600);
        rememberMeServices.setCookieName("remember-me");
        rememberMeServices.setAlwaysRemember(true);
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