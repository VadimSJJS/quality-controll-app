package com.vadimsjjs.qualitycontrollapp.config;

import com.vadimsjjs.qualitycontrollapp.service.AuditService;
import com.vadimsjjs.qualitycontrollapp.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

import java.util.List;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;
    private final DataSource dataSource;

    private static final String[] PUBLIC_PATHS = {
            "/login", "/css/**", "/js/**", "/webjars/**", "/error"
    };

    private static final List<String> OTK_ROLES = List.of("OTK_MASTER", "OTK", "OTK_CHIEF");
    private static final List<String> EDIT_ROLES = List.of("ADMIN", "PPB");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
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
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeServices(rememberMeServices())
                        .key("uniqueAndSecretKeyForRememberMe")
                        .tokenValiditySeconds(1209600) // 14 дней
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/login?expired=true")
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // Запомнить на 14 дней
    @Bean
    public TokenBasedRememberMeServices rememberMeServices() {
        TokenBasedRememberMeServices rememberMeServices =
                new TokenBasedRememberMeServices("uniqueAndSecretKeyForRememberMe", userDetailsService);
        rememberMeServices.setTokenValiditySeconds(1209600); // 14 дней
        rememberMeServices.setCookieName("remember-me");
        rememberMeServices.setAlwaysRemember(true);
        return rememberMeServices;
    }

    /*
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }

    @Bean
    public PersistentTokenBasedRememberMeServices persistentRememberMeServices() {
        PersistentTokenBasedRememberMeServices rememberMeServices =
            new PersistentTokenBasedRememberMeServices("uniqueAndSecretKeyForRememberMe",
                                                         userDetailsService,
                                                         persistentTokenRepository());
        rememberMeServices.setTokenValiditySeconds(1209600);
        rememberMeServices.setCookieName("remember-me");
        rememberMeServices.setAlwaysRemember(true);
        return rememberMeServices;
    }
    */

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            try {
                Long personalNo = Long.parseLong(authentication.getName());
                auditService.logLogin(personalNo, getClientIp(request), request.getHeader("User-Agent"));
            } catch (Exception e) {
                // #
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
                    // #
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
}