package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.DefectReportResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    public byte[] exportToExcel(DefectReportResponse report) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Отчёт по качеству");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);

            int rowNum = 0;

            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ОТЧЁТ ПО НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            Row periodRow = sheet.createRow(rowNum++);
            periodRow.createCell(0).setCellValue("Период:");
            periodRow.createCell(1).setCellValue(report.getPeriod().getDateFrom() + " — " + report.getPeriod().getDateTo());

            rowNum++;

            Row summaryHeader = sheet.createRow(rowNum++);
            summaryHeader.createCell(0).setCellValue("СВОДКА");
            summaryHeader.getCell(0).setCellStyle(boldStyle);

            DefectReportResponse.Summary summary = report.getSummary();
            addSummaryRow(sheet, rowNum++, "Всего брака, т:", summary.getTotalDefectWeight());
            addSummaryRow(sheet, rowNum++, "Восстановлено, т:", summary.getTotalReworkedWeight());
            addSummaryRow(sheet, rowNum++, "Неисправимый брак, т:", summary.getTotalIrreparableWeight());
            addSummaryRow(sheet, rowNum++, "Произведено, т:", summary.getTotalProducedWeight());
            addSummaryRow(sheet, rowNum++, "Уровень брака, %:", summary.getTotalDefectPercent());
            addSummaryRow(sheet, rowNum++, "Количество записей:", new BigDecimal(summary.getTotalRecords()));

            rowNum++;

            Row siteHeader = sheet.createRow(rowNum++);
            String[] siteHeaders = {"Участок", "Произведено, т", "Брак, т", "Восстановлено, т", "Неисправимый брак, т", "Уровень, %", "Допустимый, %"};
            for (int i = 0; i < siteHeaders.length; i++) {
                Cell cell = siteHeader.createCell(i);
                cell.setCellValue(siteHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            for (DefectReportResponse.SiteReport site : report.getSiteReports()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(site.getSiteName());
                row.createCell(1).setCellValue(site.getProducedWeight() != null ? site.getProducedWeight().doubleValue() : 0);
                row.createCell(2).setCellValue(site.getDefectWeight() != null ? site.getDefectWeight().doubleValue() : 0);
                row.createCell(3).setCellValue(site.getReworkWeight() != null ? site.getReworkWeight().doubleValue() : 0);
                row.createCell(4).setCellValue(site.getIrreparableWeight() != null ? site.getIrreparableWeight().doubleValue() : 0);
                row.createCell(5).setCellValue(site.getDefectPercent() != null ? site.getDefectPercent().doubleValue() : 0);
                row.createCell(6).setCellValue(site.getAllowablePercent() != null ? site.getAllowablePercent().doubleValue() : 0);
            }

            rowNum++;

            Row defectHeader = sheet.createRow(rowNum++);
            String[] defectHeaders = {"Вид несоответствия", "Вес, т", "% от общего брака", "Восстанавливаемый"};
            for (int i = 0; i < defectHeaders.length; i++) {
                Cell cell = defectHeader.createCell(i);
                cell.setCellValue(defectHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            for (DefectReportResponse.DefectTypeReport defect : report.getDefectTypeReports()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(defect.getDefectType());
                row.createCell(1).setCellValue(defect.getWeight() != null ? defect.getWeight().doubleValue() : 0);
                row.createCell(2).setCellValue(defect.getPercent() != null ? defect.getPercent().doubleValue() : 0);
                row.createCell(3).setCellValue(defect.isReworkable() ? "Да" : "Нет");
            }

            rowNum++;
            Row causeHeader = sheet.createRow(rowNum++);
            String[] causeHeaders = {"Причина", "Вес, т", "% от общего брака"};
            for (int i = 0; i < causeHeaders.length; i++) {
                Cell cell = causeHeader.createCell(i);
                cell.setCellValue(causeHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            for (DefectReportResponse.CauseReport cause : report.getCauseReports()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(cause.getCause());
                row.createCell(1).setCellValue(cause.getWeight() != null ? cause.getWeight().doubleValue() : 0);
                row.createCell(2).setCellValue(cause.getPercent() != null ? cause.getPercent().doubleValue() : 0);
            }

            // Автоширина колонок
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка экспорта в Excel", e);
        }
    }
    private void addSummaryRow(Sheet sheet, int rowNum, String label, BigDecimal value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value != null ? value.doubleValue() : 0);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.000"));
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        return style;
    }
}