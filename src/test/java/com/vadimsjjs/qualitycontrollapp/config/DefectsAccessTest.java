package com.vadimsjjs.qualitycontrollapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверка доступа к разделу «Журнал несоответствий» (/defects) через реальную
 * цепочку безопасности Spring Security.
 *
 * <p>Проверяем, что пользователь с ролью, которой раньше не было ни в одном списке
 * RoleChecker, больше не получает отказ на уровне URL-правила и может работать
 * с записями (добавление/редактирование/удаление разрешены рольchecker-ом).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DefectsAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "12345", authorities = "ROLE_SOME_NEW_ROLE")
    void unknownRoleIsNotBlockedOnDefectsPage() throws Exception {
        // .secure(true) - приложение работает только по HTTPS (requiresChannel), MockMvc по умолчанию http
        mockMvc.perform(get("/defects").secure(true))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "12345", authorities = "ROLE_VIEWER")
    void viewerRoleReachesDefectsPage() throws Exception {
        mockMvc.perform(get("/defects").secure(true))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "12345", authorities = "ROLE_OTK")
    void otkRoleReachesDefectsPage() throws Exception {
        mockMvc.perform(get("/defects").secure(true))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousStillGetsRedirectToLogin() throws Exception {
        mockMvc.perform(get("/defects").secure(true))
                .andExpect(status().is3xxRedirection());
    }
}
