package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.ReportDto;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportExportServiceByActsTest {

    private ReportDto.ReportByAct sampleReport() {
        ReportDto.ReportByAct.DefectRow r1 = ReportDto.ReportByAct.DefectRow.builder()
                .defectType("Деформация")
                .cause("Перегрузка")
                .total(BigDecimal.valueOf(5.000))
                .reworked(BigDecimal.valueOf(2.000))
                .reworkType("Токарная обработка")
                .defect(BigDecimal.valueOf(3.000))
                .build();

        ReportDto.ReportByAct.ActGroup g1 = ReportDto.ReportByAct.ActGroup.builder()
                .actNumber("Акт Т46-2026")
                .documentType("Акт")
                .siteName("Участок 1")
                .count(1)
                .rows(List.of(r1))
                .groupTotals(ReportDto.ReportByAct.Totals.builder()
                        .total(BigDecimal.valueOf(5.000))
                        .reworked(BigDecimal.valueOf(2.000))
                        .defect(BigDecimal.valueOf(3.000))
                        .defectPercent(BigDecimal.valueOf(60.00))
                        .count(1)
                        .build())
                .build();

        return ReportDto.ReportByAct.builder()
                .periodFrom("01.01.2026")
                .periodTo("31.12.2026")
                .producedWeight(BigDecimal.valueOf(100.000))
                .groups(List.of(g1))
                .totals(ReportDto.ReportByAct.Totals.builder()
                        .total(BigDecimal.valueOf(5.000))
                        .reworked(BigDecimal.valueOf(2.000))
                        .defect(BigDecimal.valueOf(3.000))
                        .defectPercent(BigDecimal.valueOf(3.00))
                        .count(1)
                        .build())
                .build();
    }

    @Test
    void exportByActsToExcelProducesValidXlsx() throws Exception {
        ReportExportService svc = new ReportExportService();
        byte[] out = svc.exportByActsToExcel(sampleReport());

        assertNotNull(out);
        assertTrue(out.length > 0);
        assertEquals('P', out[0]);
        assertEquals('K', out[1]);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(out))) {
            Sheet sheet = wb.getSheetAt(0);
            String period = sheet.getRow(0).getCell(0).getStringCellValue();
            assertTrue(period.startsWith("Период:"), "Ожидалась строка периода, а получено: " + period);
            String produced = sheet.getRow(1).getCell(0).getStringCellValue();
            String actNumber = sheet.getRow(3).getCell(0).getStringCellValue();
            assertEquals("Акт Т46-2026", actNumber);

            org.apache.poi.ss.usermodel.Font headerFont =
                    wb.getFontAt(sheet.getRow(2).getCell(0).getCellStyle().getFontIndex());
            assertEquals("Times New Roman", headerFont.getFontName(), "Header font must be Times New Roman");
            assertEquals(11, headerFont.getFontHeightInPoints(), "Header font size must be 11");

            org.apache.poi.ss.usermodel.Font numFont =
                    wb.getFontAt(sheet.getRow(3).getCell(5).getCellStyle().getFontIndex());
            assertEquals("Times New Roman", numFont.getFontName(), "Number cell font must be Times New Roman");
            assertEquals(11, numFont.getFontHeightInPoints(), "Number cell font size must be 11");
        }
    }

    @Test
    void exportByActsToWordProducesValidDocx() throws Exception {
        ReportExportService svc = new ReportExportService();
        byte[] out = svc.exportByActsToWord(sampleReport());

        assertNotNull(out);
        assertTrue(out.length > 0);
        assertEquals('P', out[0]);
        assertEquals('K', out[1]);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(out))) {
            String titleFont = null;
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : doc.getParagraphs()) {
                for (org.apache.poi.xwpf.usermodel.XWPFRun run : p.getRuns()) {
                    String t = run.getText(0);
                    if (t != null && t.contains("Свод актов")) {
                        titleFont = run.getFontFamily();
                    }
                }
            }
            assertNotNull(titleFont, "Title run not found");
            assertEquals("Times New Roman", titleFont, "Title must use Times New Roman");

            java.util.List<String> found = new java.util.ArrayList<>();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText();
                if (t != null && !t.isEmpty()) {
                    found.add(t);
                }
                for (org.apache.poi.xwpf.usermodel.XWPFRun run : p.getRuns()) {
                    String rt = run.getText(0);
                    if (rt != null && !rt.isEmpty()) {
                        found.add(rt);
                    }
                }
            }
            for (org.apache.poi.xwpf.usermodel.XWPFTable table : doc.getTables()) {
                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                        String ct = cell.getText();
                        if (ct != null && !ct.isEmpty()) {
                            found.add(ct);
                        }
                    }
                }
            }
            String allText = String.join("\n----\n", found);
            assertTrue(found.stream().anyMatch(s -> s.contains("Свод актов")),
                    "Title not found. Full text:\n" + allText);
            assertTrue(found.stream().anyMatch(s -> s.contains("Акт Т46-2026")),
                    "Act number not found. Full text:\n" + allText);
            assertTrue(found.stream().anyMatch(s -> s.contains("Процент брака")),
                    "Percent not found. Full text:\n" + allText);
        }
    }
}
