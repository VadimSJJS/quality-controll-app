package com.vadimsjjs.qualitycontrollapp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Проверка прав пользователя на операции с несоответствующей продукцией.
 *
 * <p>Работает в двух режимах (переключается свойством app.security.full-access):
 * <ul>
 *   <li><b>true</b> — любой авторизованный пользователь может просматривать,
 *       добавлять, редактировать и удалять записи (режим «полный доступ»);</li>
 *   <li><b>false</b> — действуют роли из справочника персонала, как предусмотрено ТЗ:
 *       ОТК и ППБ редактируют, удалять может только старший ОТК/администратор,
 *       роль {@code FULL_ACCESS} даёт полный доступ отдельному пользователю.</li>
 * </ul>
 *
 * <p>Роль берётся из {@code V_PERSONAL_STPC2.ROLE_NAME} при входе по табельному номеру.
 */
@Component("roleChecker")
public class RoleChecker {

    /** Роль, которая всегда даёт полный доступ (указывается в ROLE_NAME персонала). */
    public static final String FULL_ACCESS_ROLE = "FULL_ACCESS";

    private static final List<String> EDIT_ROLES = List.of(
            FULL_ACCESS_ROLE,
            "OTK_MASTER", "OTK", "OTK_CHIEF",
            "10_OTK", "11_OTK", "12_OTK",
            "PPB", "6_PPB",
            "ADMIN"
    );

    private static final List<String> DELETE_ROLES = List.of(
            FULL_ACCESS_ROLE,
            "OTK_MASTER", "OTK_CHIEF",
            "10_OTK", "11_OTK", "12_OTK",
            "ADMIN"
    );

    private static final List<String> VIEW_ROLES = List.of(
            FULL_ACCESS_ROLE,
            "OTK_MASTER", "OTK", "OTK_CHIEF",
            "10_OTK", "11_OTK", "12_OTK",
            "PPB", "6_PPB",
            "ADMIN",
            "VIEWER"
    );

    private final boolean fullAccessForAllUsers;

    public RoleChecker(@Value("${app.security.full-access:true}") boolean fullAccessForAllUsers) {
        this.fullAccessForAllUsers = fullAccessForAllUsers;
    }

    public boolean canEdit() {
        return hasAccess(EDIT_ROLES);
    }

    public boolean canDelete() {
        return hasAccess(DELETE_ROLES);
    }

    public boolean canView() {
        return hasAccess(VIEW_ROLES);
    }

    /** Права администратора (служебные функции) — полный доступ их не даёт. */
    public boolean isAdmin() {
        return hasAnyRole(List.of("ADMIN"));
    }

    public boolean isFullAccessForAllUsers() {
        return fullAccessForAllUsers;
    }

    private boolean hasAccess(List<String> roles) {
        if (!isAuthenticated()) {
            return false;
        }
        if (fullAccessForAllUsers) {
            return true;
        }
        return hasAnyRole(roles);
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()));
    }

    private boolean hasAnyRole(List<String> roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
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
