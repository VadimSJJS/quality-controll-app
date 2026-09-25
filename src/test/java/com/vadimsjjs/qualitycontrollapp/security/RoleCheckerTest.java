package com.vadimsjjs.qualitycontrollapp.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка прав доступа к базе несоответствующей продукции.
 */
class RoleCheckerTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                "12345", "pwd", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void fullAccessEnabled_anyUserCanCreateEditAndDelete() {
        RoleChecker checker = new RoleChecker(true);

        // роль, которой раньше не было ни в одном списке
        authenticateAs("SOME_NEW_ROLE");

        assertTrue(checker.canView(), "просмотр должен быть разрешён");
        assertTrue(checker.canEdit(), "добавление/редактирование должно быть разрешено");
        assertTrue(checker.canDelete(), "удаление должно быть разрешено");
    }

    @Test
    void fullAccessEnabled_viewerRoleAlsoGetsEditAndDelete() {
        RoleChecker checker = new RoleChecker(true);

        authenticateAs("VIEWER");

        assertTrue(checker.canEdit());
        assertTrue(checker.canDelete());
    }

    @Test
    void fullAccessDisabled_rolesWorkAsBefore() {
        RoleChecker checker = new RoleChecker(false);

        authenticateAs("OTK");
        assertTrue(checker.canView());
        assertTrue(checker.canEdit());
        assertFalse(checker.canDelete(), "обычный ОТК удалять не должен");

        authenticateAs("OTK_MASTER");
        assertTrue(checker.canDelete(), "старший ОТК удаляет");

        authenticateAs("PPB");
        assertTrue(checker.canEdit(), "ППБ редактирует");
        assertFalse(checker.canDelete(), "ППБ удалять не должен");
    }

    @Test
    void fullAccessDisabled_unknownRoleGetsViewOnly() {
        RoleChecker checker = new RoleChecker(false);

        authenticateAs("SOME_NEW_ROLE");

        assertFalse(checker.canEdit());
        assertFalse(checker.canDelete());
    }

    @Test
    void explicitFullAccessRoleAlwaysCanDelete() {
        RoleChecker checker = new RoleChecker(false);

        authenticateAs(RoleChecker.FULL_ACCESS_ROLE);

        assertTrue(checker.canView());
        assertTrue(checker.canEdit());
        assertTrue(checker.canDelete());
    }

    @Test
    void anonymousUserHasNoAccess() {
        RoleChecker checker = new RoleChecker(true);

        assertFalse(checker.canView());
        assertFalse(checker.canEdit());
        assertFalse(checker.canDelete());
    }

    @Test
    void fullAccessDoesNotGrantAdminRights() {
        RoleChecker checker = new RoleChecker(true);

        authenticateAs("OTK");

        assertFalse(checker.isAdmin(), "полный доступ не должен давать права администратора");
    }
}
