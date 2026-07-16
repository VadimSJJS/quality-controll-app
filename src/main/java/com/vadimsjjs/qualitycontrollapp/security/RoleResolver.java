package com.vadimsjjs.qualitycontrollapp.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public final class RoleResolver {

    private RoleResolver() {
    }

    public static String resolveGroup(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return "ROLE_VIEWER";
        }

        String roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        if (roles.contains("ADMIN")) return "ROLE_ADMIN";
        if (roles.contains("OTK")) return "ROLE_OTK";
        if (roles.contains("6_PPB")) return "ROLE_PPB";
        return "ROLE_VIEWER";
    }

    public static String join(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return "ROLE_VIEWER";
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .reduce((a, b) -> a + ", " + b)
                .orElse("ROLE_VIEWER");
    }
}