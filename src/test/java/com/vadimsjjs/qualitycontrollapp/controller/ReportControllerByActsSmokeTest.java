package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.dto.ReportDto;
import com.vadimsjjs.qualitycontrollapp.service.ExcelExportService;
import com.vadimsjjs.qualitycontrollapp.service.ReportExportService;
import com.vadimsjjs.qualitycontrollapp.service.ReportService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * HTTP‑smoke (без браузера и без Oracle): проверяем, что эндпоинты «Свод по актам»
 * отдают 200, корректный Content‑Disposition и исполняемые файлы XLSX/DOCX.
 * Сервисы замоканы — никакой БД, никаких фильтров контроллера не задействовано.
 */
//@WebMvcTest(controllers = ReportController.class)
//@AutoConfigureMockMvc(addFilters = false)
//class ReportControllerByActsSmokeTest {
//    @Autowired
//    private MockMvc mvc;
//    @MockBean
//    private ReportService reportService;
//    @MockBean
//    private ReportExportService reportExportService;
//    @MockBean
//    private ExcelExportService excelExportService;
//    private final ReportExportService realExport = new ReportExportService();
//
//    private ReportDto.ReportByAct sampleReport() {
//        ReportDto.ReportByAct.DefectRow r1 = ReportDto.ReportByAct.DefectRow.builder()
//                .defectType("Деформация")
//                .cause("Перегрузка")
//                .total(BigDecimal.valueOf(5.000))
//                .reworked(BigDecimal.valueOf(2.000))
//                .reworkType("Токарная обработка")
//                .defect(BigDecimal.valueOf(3.000))
//                .build();
//
//        ReportDto.ReportByAct.ActGroup g1 = ReportDto.ReportByAct.ActGroup.builder()
//                .actNumber("Акт Т46-2026")
//                .documentType("Акт")
//                .siteName("Участок 1")
//                .count(1)
//                .rows(List.of(r1))
//                .groupTotals(ReportDto.ReportByAct.Totals.builder()
//                        .total(BigDecimal.valueOf(5.000))
//                        .reworked(BigDecimal.valueOf(2.000))
//                        .defect(BigDecimal.valueOf(3.000))
//                        .defectPercent(BigDecimal.valueOf(60.00))
//                        .count(1)
//                        .build())
//                .build();
//
//        return ReportDto.ReportByAct.builder()
//                .periodFrom("01.01.2026")
//                .periodTo("31.12.2026")
//                .producedWeight(BigDecimal.valueOf(100.000))
//                .groups(List.of(g1))
//                .totals(ReportDto.ReportByAct.Totals.builder()
//                        .total(BigDecimal.valueOf(5.000))
//                        .reworked(BigDecimal.valueOf(2.000))
//                        .defect(BigDecimal.valueOf(3.000))
//                        .defectPercent(BigDecimal.valueOf(3.00))
//                        .count(1)
//                        .build())
//                .build();
//    }
//
//    private void stubByActs() throws Exception {
//        when(reportService.getReportByActs(any(DefectFilterDto.class))).thenReturn(sampleReport());
//        when(reportExportService.exportByActsToExcel(any(ReportDto.ReportByAct.class)))
//                .thenReturn(realExport.exportByActsToExcel(sampleReport()));
//        when(reportExportService.exportByActsToWord(any(ReportDto.ReportByAct.class)))
//                .thenReturn(realExport.exportByActsToWord(sampleReport()));
//    }
//
//    @Test
//    void byActsJsonEndpointReturnsJson() throws Exception {
//        stubByActs();
//        MockHttpServletResponse resp = mvc.perform(get("/api/reports/by-acts")
//                        .param("dateFrom", "2026-01-01")
//                        .param("dateTo", "2026-12-31"))
//                .andExpect(status().isOk())
//                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                .andReturn()
//                .getResponse();
//
//        String body = resp.getContentAsString();
//        assertTrue(body.contains("Акт Т46-2026"), "JSON must contain the act number, body: " + body);
//    }
//
//    @Test
//    void byActsExcelDownloadHasContentDispositionAndValidXlsx() throws Exception {
//        stubByActs();
//        MockHttpServletResponse resp = mvc.perform(get("/api/reports/export/excel/by-acts")
//                        .param("dateFrom", "2026-01-01")
//                        .param("dateTo", "2026-12-31"))
//                .andExpect(status().isOk())
//                .andReturn()
//                .getResponse();
//
//        byte[] data = resp.getContentAsByteArray();
//        String cd = resp.getHeader(HttpHeaders.CONTENT_DISPOSITION);
//
//        assertNotNull(cd, "Content-Disposition header is required");
//        assertTrue(cd.startsWith("attachment; filename*=UTF-8''"), cd);
//        assertTrue(cd.endsWith(".xlsx"), cd);
//        // XLSX — ZIP, signature "PK"
//        assertTrue(data != null && data.length > 4 && data[0] == 'P' && data[1] == 'K',
//                "Response body must be a real XLSX/ZIP archive");
//
//        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(data))) {
//            assertEquals("Свод по актам", wb.getSheetAt(0).getSheetName());
//            assertTrue(wb.getSheetAt(0).getRow(3).getCell(0).getStringCellValue()
//                            .contains("Акт Т46-2026"),
//                    "First act group row must contain the act number");
//        }
//    }
//
//    @Test
//    void byActsWordDownloadHasContentDispositionAndValidDocx() throws Exception {
//        stubByActs();
//        MockHttpServletResponse resp = mvc.perform(get("/api/reports/export/word/by-acts")
//                        .param("dateFrom", "2026-01-01")
//                        .param("dateTo", "2026-12-31"))
//                .andExpect(status().isOk())
//                .andReturn()
//                .getResponse();
//
//        byte[] data = resp.getContentAsByteArray();
//        String cd = resp.getHeader(HttpHeaders.CONTENT_DISPOSITION);
//
//        assertNotNull(cd, "Content-Disposition header is required");
//        assertTrue(cd.startsWith("attachment; filename*=UTF-8''"), cd);
//        assertTrue(cd.endsWith(".docx"), cd);
//        // DOCX — тоже ZIP
//        assertTrue(data != null && data.length > 4 && data[0] == 'P' && data[1] == 'K',
//                "Response body must be a real DOCX/ZIP archive");
//
//        boolean foundTitle = false;
//        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data))) {
//            for (XWPFParagraph p : doc.getParagraphs()) {
//                for (XWPFRun run : p.getRuns()) {
//                    String t = run.getText(0);
//                    if (t != null && t.contains("Свод актов")) {
//                        foundTitle = true;
//                    }
//                }
//            }
//        }
//        assertTrue(foundTitle, "Title 'Свод актов и справок о браке по цеху' not found in docx");
//    }
//
//    /**
//     * Статический чек шаблона UI: кнопка «Свод по актам» присутствует, dispatch
//     * и renderByActs подключены, а кнопка НЕ помечена `active` по умолчанию
//     * (активна `by-site`).
//     */
//    @Test
//    void reportPageContainsByActsButtonNotActive() throws Exception {
//        String html;
//        try (InputStream is = getClass().getResourceAsStream("/templates/reports/index.html")) {
//            assertNotNull(is, "index.html must be on test classpath");
//            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
//        }
//
//        assertTrue(html.contains("data-type=\"by-acts\""), "by-acts button must exist");
//        assertTrue(html.contains("function renderByActs"), "renderByActs() must exist");
//        // минимум 3 обращения к 'by-acts' в JS‑switch (fetch/render/excel/word)
//        int count = html.split("['\"]by-acts['\"]", -1).length - 1;
//        assertTrue(count >= 4, "Expected >=4 'by-acts' references in JS, found " + count);
//
//        int idx = html.indexOf("data-type=\"by-acts\"");
//        assertTrue(idx > 0);
//        String window = html.substring(Math.max(0, idx - 120), idx);
//        assertFalse(window.contains("active"),
//                "by-acts button must NOT be 'active' by default (by-site is active): " + window);
//    }
//}
