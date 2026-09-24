package com.vadimsjjs.qualitycontrollapp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("roleChecker")
public class RoleChecker {

    private static final List<String> EDIT_ROLES = List.of(
            "OTK_MASTER", "OTK", "OTK_CHIEF",
            "10_OTK", "11_OTK", "12_OTK",
            "PPB", "6_PPB",
            "ADMIN"
    );

    private static final List<String> DELETE_ROLES = List.of(
            "OTK_MASTER", "OTK_CHIEF",
            "10_OTK", "11_OTK", "12_OTK",
            "ADMIN"
    );

    private static final List<String> VIEW_ROLES = List.of(
            "OTK_MASTER", "OTK", "OTK_CHIEF",
            "10_OTK", "11_OTK", "12_OTK",
            "PPB", "6_PPB",
            "ADMIN",
            "VIEWER"
    );

    public boolean canEdit() {
        return hasAnyRole(EDIT_ROLES);
    }

    public boolean canDelete() {
        return hasAnyRole(DELETE_ROLES);
    }

    public boolean canView() {
        return hasAnyRole(VIEW_ROLES);
    }

    public boolean isAdmin() {
        return hasAnyRole(List.of("ADMIN"));
    }

    private boolean hasAnyRole(List<String> roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> {
                    String role = authority.startsWith("ROLE_") ? authority.substring(5) : authority;
                    return roles.contains(role);
                });
    }
}
