package com.vadimsjjs.qualitycontrollapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверка страницы справочников и API видов дефектов.
 * Только чтение: тесты ничего не пишут в базу.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DirectoryAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "12345", authorities = "ROLE_SOME_NEW_ROLE")
    void directoryPageIsAvailable() throws Exception {
        mockMvc.perform(get("/directories").secure(true))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Виды несоответствий")));
    }

    @Test
    @WithMockUser(username = "12345", authorities = "ROLE_SOME_NEW_ROLE")
    void defectTypesApiIsAvailable() throws Exception {
        mockMvc.perform(get("/directories/defect-types").secure(true))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @WithMockUser(username = "12345", authorities = "ROLE_SOME_NEW_ROLE")
    void defectTypesSearchRejectsBlankQuery() throws Exception {
        mockMvc.perform(get("/directories/defect-types/search").param("q", "  ").secure(true))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotReadDefectTypes() throws Exception {
        mockMvc.perform(get("/directories/defect-types").secure(true))
                .andExpect(status().is3xxRedirection());
    }
}
