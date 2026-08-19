package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private static final String FONT_NAME = "Times New Roman";
    private static final short FONT_SIZE = 11;

    // ===== EXCEL EXPORT =====

    public byte[] exportBySiteToExcel(ReportDto.ReportBySite report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Отчёт по участку");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numStyle = createNumStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            int row = 0;
            Row r = sheet.createRow(row++);
            Cell c = r.createCell(0);
            c.setCellValue("ОТЧЕТ О НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО УЧАСТКУ " + report.getSiteName());
            c.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            row++;
            String[] headers = {"Вид несоответствия", "Всего, т", "Доработано, т", "Вид доработки", "Брак, т"};
            r = sheet.createRow(row++);
            for (int i = 0; i < headers.length; i++) {
                r.createCell(i).setCellValue(headers[i]);
                r.getCell(i).setCellStyle(headerStyle);
            }

            for (ReportDto.ReportBySite.DefectRow dr : report.getRows()) {
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue(dr.getDefectType());
                r.createCell(1).setCellValue(dr.getTotal() != null ? dr.getTotal().doubleValue() : 0);
                r.getCell(1).setCellStyle(numStyle);
                r.createCell(2).setCellValue(dr.getReworked() != null ? dr.getReworked().doubleValue() : 0);
                r.getCell(2).setCellStyle(numStyle);
                r.createCell(3).setCellValue(dr.getReworkType() != null ? dr.getReworkType() : "");
                r.createCell(4).setCellValue(dr.getDefect() != null ? dr.getDefect().doubleValue() : 0);
                r.getCell(4).setCellStyle(numStyle);
            }

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Итого по участку");
            r.getCell(0).setCellStyle(boldStyle);
            r.createCell(1).setCellValue(report.getTotals().getTotal() != null ? report.getTotals().getTotal().doubleValue() : 0);
            r.getCell(1).setCellStyle(numStyle);
            r.getCell(1).setCellStyle(boldStyle);
            r.createCell(2).setCellValue(report.getTotals().getReworked() != null ? report.getTotals().getReworked().doubleValue() : 0);
            r.getCell(2).setCellStyle(numStyle);
            r.getCell(2).setCellStyle(boldStyle);
            r.createCell(4).setCellValue(report.getTotals().getDefect() != null ? report.getTotals().getDefect().doubleValue() : 0);
            r.getCell(4).setCellStyle(numStyle);
            r.getCell(4).setCellStyle(boldStyle);

            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByProductTypeToExcel(ReportDto.ReportByProductType report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Отчёт по виду продукции");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numStyle = createNumStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            int row = 0;
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue("ОТЧЕТ О НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО УЧАСТКУ " + report.getSiteName() + " ПО ВИДУ ПРОДУКЦИИ");
            r.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            for (ReportDto.ReportByProductType.ProductTypeGroup grp : report.getGroups()) {
                row++;
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue(grp.getProductType());
                r.getCell(0).setCellStyle(boldStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(row - 1, row - 1, 0, 5));

                String[] headers = {"Вид несоответствия", "Всего, т", "Доработано, т", "Вид доработки", "Брак, т"};
                r = sheet.createRow(row++);
                for (int i = 0; i < headers.length; i++) {
                    r.createCell(i).setCellValue(headers[i]);
                    r.getCell(i).setCellStyle(headerStyle);
                }

                for (ReportDto.ReportByProductType.DefectRow dr : grp.getRows()) {
                    r = sheet.createRow(row++);
                    r.createCell(0).setCellValue(dr.getDefectType());
                    r.createCell(1).setCellValue(dr.getTotal() != null ? dr.getTotal().doubleValue() : 0);
                    r.getCell(1).setCellStyle(numStyle);
                    r.createCell(2).setCellValue(dr.getReworked() != null ? dr.getReworked().doubleValue() : 0);
                    r.getCell(2).setCellStyle(numStyle);
                    r.createCell(3).setCellValue(dr.getReworkType() != null ? dr.getReworkType() : "");
                    r.createCell(4).setCellValue(dr.getDefect() != null ? dr.getDefect().doubleValue() : 0);
                    r.getCell(4).setCellStyle(numStyle);
                }

                r = sheet.createRow(row++);
                r.createCell(0).setCellValue("Итого по " + grp.getProductType());
                r.getCell(0).setCellStyle(boldStyle);
                r.createCell(1).setCellValue(grp.getGroupTotals().getTotal() != null ? grp.getGroupTotals().getTotal().doubleValue() : 0);
                r.getCell(1).setCellStyle(numStyle);
                r.getCell(1).setCellStyle(boldStyle);
                r.createCell(2).setCellValue(grp.getGroupTotals().getReworked() != null ? grp.getGroupTotals().getReworked().doubleValue() : 0);
                r.getCell(2).setCellStyle(numStyle);
                r.getCell(2).setCellStyle(boldStyle);
                r.createCell(4).setCellValue(grp.getGroupTotals().getDefect() != null ? grp.getGroupTotals().getDefect().doubleValue() : 0);
                r.getCell(4).setCellStyle(numStyle);
                r.getCell(4).setCellStyle(boldStyle);
            }

            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByProductAndCauseToExcel(ReportDto.ReportByProductAndCause report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Отчёт по видам и причинам");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numStyle = createNumStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            int row = 0;
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue("ОТЧЕТ О НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО УЧАСТКУ " + report.getSiteName() + " ПО ВИДУ ПРОДУКЦИИ И ПРИЧИНАМ");
            r.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            for (ReportDto.ReportByProductAndCause.CauseGroup grp : report.getGroups()) {
                row++;
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue(grp.getProductType());
                r.getCell(0).setCellStyle(boldStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(row - 1, row - 1, 0, 4));

                String[] headers = {"Вид несоответствия", "Всего, т", "Причина", "Подпричина", "Примечание"};
                r = sheet.createRow(row++);
                for (int i = 0; i < headers.length; i++) {
                    r.createCell(i).setCellValue(headers[i]);
                    r.getCell(i).setCellStyle(headerStyle);
                }

                for (ReportDto.ReportByProductAndCause.DefectCauseRow dcr : grp.getRows()) {
                    r = sheet.createRow(row++);
                    r.createCell(0).setCellValue(dcr.getDefectType());
                    r.createCell(1).setCellValue(dcr.getTotal() != null ? dcr.getTotal().doubleValue() : 0);
                    r.getCell(1).setCellStyle(numStyle);
                    r.createCell(2).setCellValue(dcr.getCause() != null ? dcr.getCause() : "");
                    r.createCell(3).setCellValue(dcr.getSubcause() != null ? dcr.getSubcause() : "");
                    r.createCell(4).setCellValue(dcr.getNote() != null ? dcr.getNote() : "");
                }

                r = sheet.createRow(row++);
                r.createCell(0).setCellValue("Итого по " + grp.getProductType());
                r.getCell(0).setCellStyle(boldStyle);
                r.createCell(1).setCellValue(grp.getGroupTotals().getTotal() != null ? grp.getGroupTotals().getTotal().doubleValue() : 0);
                r.getCell(1).setCellStyle(numStyle);
                r.getCell(1).setCellStyle(boldStyle);
            }

            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByBrigadeToExcel(ReportDto.ReportByBrigade report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Отчёт по бригаде");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numStyle = createNumStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            int row = 0;
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue("ОТЧЕТ О НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО УЧАСТКУ " + report.getSiteName() + " ПО БРИГАДЕ");
            r.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            for (ReportDto.ReportByBrigade.ProductTypeGroup grp : report.getGroups()) {
                row++;
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue(grp.getProductType());
                r.getCell(0).setCellStyle(boldStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(row - 1, row - 1, 0, 5));

                String[] headers = {"Вид несоответствия", "Всего, т", "Доработано, т", "Вид доработки", "Брак, т"};
                r = sheet.createRow(row++);
                for (int i = 0; i < headers.length; i++) {
                    r.createCell(i).setCellValue(headers[i]);
                    r.getCell(i).setCellStyle(headerStyle);
                }

                for (ReportDto.ReportByBrigade.DefectRow dr : grp.getRows()) {
                    r = sheet.createRow(row++);
                    r.createCell(0).setCellValue(dr.getDefectType());
                    r.createCell(1).setCellValue(dr.getTotal() != null ? dr.getTotal().doubleValue() : 0);
                    r.getCell(1).setCellStyle(numStyle);
                    r.createCell(2).setCellValue(dr.getReworked() != null ? dr.getReworked().doubleValue() : 0);
                    r.getCell(2).setCellStyle(numStyle);
                    r.createCell(3).setCellValue(dr.getReworkType() != null ? dr.getReworkType() : "");
                    r.createCell(4).setCellValue(dr.getDefect() != null ? dr.getDefect().doubleValue() : 0);
                    r.getCell(4).setCellStyle(numStyle);
                }

                r = sheet.createRow(row++);
                r.createCell(0).setCellValue("Итого по " + grp.getProductType());
                r.getCell(0).setCellStyle(boldStyle);
                r.createCell(1).setCellValue(grp.getGroupTotals().getTotal() != null ? grp.getGroupTotals().getTotal().doubleValue() : 0);
                r.getCell(1).setCellStyle(numStyle);
                r.getCell(1).setCellStyle(boldStyle);
                r.createCell(2).setCellValue(grp.getGroupTotals().getReworked() != null ? grp.getGroupTotals().getReworked().doubleValue() : 0);
                r.getCell(2).setCellStyle(numStyle);
                r.getCell(2).setCellStyle(boldStyle);
                r.createCell(4).setCellValue(grp.getGroupTotals().getDefect() != null ? grp.getGroupTotals().getDefect().doubleValue() : 0);
                r.getCell(4).setCellStyle(numStyle);
                r.getCell(4).setCellStyle(boldStyle);
            }

            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByEquipmentToExcel(ReportDto.ReportByEquipment report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Отчёт по оборудованию");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numStyle = createNumStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            int row = 0;
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue("ОТЧЕТ О НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО УЧАСТКУ " + report.getSiteName() + " ПО ОБОРУДОВАНИЮ");
            r.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            for (ReportDto.ReportByEquipment.EquipmentGroup grp : report.getGroups()) {
                row++;
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue(grp.getEquipment());
                r.getCell(0).setCellStyle(boldStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(row - 1, row - 1, 0, 5));

                String[] headers = {"Вид несоответствия", "Всего, т", "Доработано, т", "Вид доработки", "Брак, т"};
                r = sheet.createRow(row++);
                for (int i = 0; i < headers.length; i++) {
                    r.createCell(i).setCellValue(headers[i]);
                    r.getCell(i).setCellStyle(headerStyle);
                }

                for (ReportDto.ReportByEquipment.DefectRow dr : grp.getRows()) {
                    r = sheet.createRow(row++);
                    r.createCell(0).setCellValue(dr.getDefectType());
                    r.createCell(1).setCellValue(dr.getTotal() != null ? dr.getTotal().doubleValue() : 0);
                    r.getCell(1).setCellStyle(numStyle);
                    r.createCell(2).setCellValue(dr.getReworked() != null ? dr.getReworked().doubleValue() : 0);
                    r.getCell(2).setCellStyle(numStyle);
                    r.createCell(3).setCellValue(dr.getReworkType() != null ? dr.getReworkType() : "");
                    r.createCell(4).setCellValue(dr.getDefect() != null ? dr.getDefect().doubleValue() : 0);
                    r.getCell(4).setCellStyle(numStyle);
                }

                r = sheet.createRow(row++);
                r.createCell(0).setCellValue("Итого по " + grp.getEquipment());
                r.getCell(0).setCellStyle(boldStyle);
                r.createCell(1).setCellValue(grp.getGroupTotals().getTotal() != null ? grp.getGroupTotals().getTotal().doubleValue() : 0);
                r.getCell(1).setCellStyle(numStyle);
                r.getCell(1).setCellStyle(boldStyle);
                r.createCell(2).setCellValue(grp.getGroupTotals().getReworked() != null ? grp.getGroupTotals().getReworked().doubleValue() : 0);
                r.getCell(2).setCellStyle(numStyle);
                r.getCell(2).setCellStyle(boldStyle);
                r.createCell(4).setCellValue(grp.getGroupTotals().getDefect() != null ? grp.getGroupTotals().getDefect().doubleValue() : 0);
                r.getCell(4).setCellStyle(numStyle);
                r.getCell(4).setCellStyle(boldStyle);
            }

            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByPersonnelToExcel(ReportDto.ReportByPersonnel report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Отчёт по персоналу");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numStyle = createNumStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            int row = 0;
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue("ОТЧЕТ О НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО УЧАСТКУ " + report.getSiteName() + " ПО ПЕРСОНАЛУ");
            r.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            for (ReportDto.ReportByPersonnel.PersonnelGroup grp : report.getGroups()) {
                row++;
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue("Перс№/таб№ " + grp.getPersonnelNumber());
                r.getCell(0).setCellStyle(boldStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(row - 1, row - 1, 0, 3));

                String[] headers = {"Вид несоответствия", "Всего, т", "Брак, т", "Примечание"};
                r = sheet.createRow(row++);
                for (int i = 0; i < headers.length; i++) {
                    r.createCell(i).setCellValue(headers[i]);
                    r.getCell(i).setCellStyle(headerStyle);
                }

                for (ReportDto.ReportByPersonnel.DefectRow dr : grp.getRows()) {
                    r = sheet.createRow(row++);
                    r.createCell(0).setCellValue(dr.getDefectType());
                    r.createCell(1).setCellValue(dr.getTotal() != null ? dr.getTotal().doubleValue() : 0);
                    r.getCell(1).setCellStyle(numStyle);
                    r.createCell(2).setCellValue(dr.getDefect() != null ? dr.getDefect().doubleValue() : 0);
                    r.getCell(2).setCellStyle(numStyle);
                    r.createCell(3).setCellValue(dr.getNote() != null ? dr.getNote() : "");
                }

                r = sheet.createRow(row++);
                r.createCell(0).setCellValue("Итого перс.№/таб№ " + grp.getPersonnelNumber());
                r.getCell(0).setCellStyle(boldStyle);
                r.createCell(1).setCellValue(grp.getGroupTotals().getTotal() != null ? grp.getGroupTotals().getTotal().doubleValue() : 0);
                r.getCell(1).setCellStyle(numStyle);
                r.getCell(1).setCellStyle(boldStyle);
                r.createCell(2).setCellValue(grp.getGroupTotals().getDefect() != null ? grp.getGroupTotals().getDefect().doubleValue() : 0);
                r.getCell(2).setCellStyle(numStyle);
                r.getCell(2).setCellStyle(boldStyle);
            }

            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByPlantToExcel(ReportDto.ReportByPlant report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Отчёт по цеху");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numStyle = createNumStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            int row = 0;
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue("ДАННЫЕ ПО НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО ЦЕХУ");
            r.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            String[] headers = {"Участки", "Производство, т", "Несоответствующая, т", "Несоответствующая, %", "Допустимый уровень, %"};
            r = sheet.createRow(row++);
            for (int i = 0; i < headers.length; i++) {
                r.createCell(i).setCellValue(headers[i]);
                r.getCell(i).setCellStyle(headerStyle);
            }

            for (ReportDto.ReportByPlant.SiteRow sr : report.getRows()) {
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue(sr.getSiteName());
                if ("total".equals(sr.getType())) r.getCell(0).setCellStyle(boldStyle);
                r.createCell(1).setCellValue(sr.getProduced() != null ? sr.getProduced().doubleValue() : 0);
                r.getCell(1).setCellStyle(numStyle);
                r.createCell(2).setCellValue(sr.getNonconforming() != null ? sr.getNonconforming().doubleValue() : 0);
                r.getCell(2).setCellStyle(numStyle);
                r.createCell(3).setCellValue(sr.getNonconformingPercent() != null ? sr.getNonconformingPercent().doubleValue() : 0);
                r.getCell(3).setCellStyle(numStyle);
                r.createCell(4).setCellValue(sr.getAllowablePercent() != null ? sr.getAllowablePercent().doubleValue() : 0);
                r.getCell(4).setCellStyle(numStyle);
                if (sr.isExceedsAllowable()) {
                    for (int i = 0; i < 5; i++) r.getCell(i).setCellStyle(boldStyle);
                }
            }

            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByFaultToExcel(ReportDto.ReportByFault report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Отчёт по вине");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numStyle = createNumStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            int row = 0;
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue("ДАННЫЕ ПО НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО ЦЕХУ (ПО ВИНЕ)");
            r.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            String[] headers = {"Категория", "Участок", "Производство, т", "Несоответствующая, т", "Несоответствующая, %", "Допустимый уровень, %"};
            r = sheet.createRow(row++);
            for (int i = 0; i < headers.length; i++) {
                r.createCell(i).setCellValue(headers[i]);
                r.getCell(i).setCellStyle(headerStyle);
            }

            for (ReportDto.ReportByFault.FaultRow fr : report.getRows()) {
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue(fr.getCategory());
                r.createCell(1).setCellValue(fr.getSiteName() != null ? fr.getSiteName() : "");
                r.createCell(2).setCellValue(fr.getProduced() != null ? fr.getProduced().doubleValue() : 0);
                r.getCell(2).setCellStyle(numStyle);
                r.createCell(3).setCellValue(fr.getNonconforming() != null ? fr.getNonconforming().doubleValue() : 0);
                r.getCell(3).setCellStyle(numStyle);
                r.createCell(4).setCellValue(fr.getNonconformingPercent() != null ? fr.getNonconformingPercent().doubleValue() : 0);
                r.getCell(4).setCellStyle(numStyle);
                r.createCell(5).setCellValue(fr.getAllowablePercent() != null ? fr.getAllowablePercent().doubleValue() : 0);
                r.getCell(5).setCellStyle(numStyle);
                if (fr.isExceedsAllowable()) {
                    for (int i = 0; i < 6; i++) r.getCell(i).setCellStyle(boldStyle);
                }
            }

            for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportParetoToExcel(ParetoReport report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Диаграмма Парето");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numStyle = createNumStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            int row = 0;
            Row r = sheet.createRow(row++);
            r.createCell(0).setCellValue("ДИАГРАММА ПАРЕТО — " + report.getSiteName());
            r.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            r = sheet.createRow(row++);
            r.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());
            r.createCell(1).setCellValue("Группировка: " + (report.getGroupingType().equals("defect") ? "по виду дефекта" : "по причине"));

            String[] headers = {"№", "Категория", "Вес, т", "Накопительный %"};
            r = sheet.createRow(row++);
            for (int i = 0; i < headers.length; i++) {
                r.createCell(i).setCellValue(headers[i]);
                r.getCell(i).setCellStyle(headerStyle);
            }

            int idx = 1;
            for (ParetoReport.ParetoItem item : report.getItems()) {
                r = sheet.createRow(row++);
                r.createCell(0).setCellValue(idx++);
                r.createCell(1).setCellValue(item.getCategory());
                r.createCell(2).setCellValue(item.getWeight() != null ? item.getWeight().doubleValue() : 0);
                r.getCell(2).setCellStyle(numStyle);
                r.createCell(3).setCellValue(item.getCumulativePercent() != null ? item.getCumulativePercent().doubleValue() : 0);
                r.getCell(3).setCellStyle(numStyle);
            }

            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
                        return baos.toByteArray();
        }
    }

    // ===== EXCEL EXPORT — Свод по актам =====
    public byte[] exportByActsToExcel(ReportDto.ReportByAct report) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Свод по актам");
            int rowIdx = 0;

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor((short) 22);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row periodRow = sheet.createRow(rowIdx++);
            periodRow.createCell(0).setCellValue("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());
            Row producedRow = sheet.createRow(rowIdx++);
            producedRow.createCell(0).setCellValue("Производство за период: " + report.getProducedWeight() + " т");

            String[] headers = {"Документ", "Вид", "Цех", "Вид несоответствия", "Причина брака",
                    "Всего, т", "Доработано, т", "Вид доработки", "Брак, т"};
            Row header = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (ReportDto.ReportByAct.ActGroup group : report.getGroups()) {
                Row gh = sheet.createRow(rowIdx++);
                gh.createCell(0).setCellValue(group.getActNumber());
                gh.createCell(1).setCellValue(group.getDocumentType());
                gh.createCell(2).setCellValue(group.getSiteName());
                gh.createCell(3).setCellValue("Всего по документу:");
                gh.createCell(5).setCellValue(group.getGroupTotals().getTotal().doubleValue());
                gh.createCell(6).setCellValue(group.getGroupTotals().getReworked().doubleValue());
                gh.createCell(8).setCellValue(group.getGroupTotals().getDefect().doubleValue());

                for (ReportDto.ReportByAct.DefectRow r : group.getRows()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(3).setCellValue(r.getDefectType());
                    row.createCell(4).setCellValue(r.getCause());
                    row.createCell(5).setCellValue(r.getTotal().doubleValue());
                    row.createCell(6).setCellValue(r.getReworked().doubleValue());
                    row.createCell(7).setCellValue(r.getReworkType());
                    row.createCell(8).setCellValue(r.getDefect().doubleValue());
                }
            }

            ReportDto.ReportByAct.Totals totals = report.getTotals();
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.createCell(0).setCellValue("Итого:");
            totalRow.createCell(5).setCellValue(totals.getTotal().doubleValue());
            totalRow.createCell(6).setCellValue(totals.getReworked().doubleValue());
            totalRow.createCell(8).setCellValue(totals.getDefect().doubleValue());

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }


    // ===== WORD EXPORT (DOCX via XWPF) =====
    public byte[] exportBySiteToWord(ReportDto.ReportBySite report) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph title = doc.createParagraph();
            title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(14);
            titleRun.setFontFamily("Times New Roman");
            titleRun.setText("ОТЧЕТ О НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО УЧАСТКУ " + report.getSiteName());

            org.apache.poi.xwpf.usermodel.XWPFParagraph period = doc.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun periodRun = period.createRun();
            periodRun.setFontSize(11);
            periodRun.setFontFamily("Times New Roman");
            periodRun.setText("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            doc.createParagraph();

            String[][] data = new String[report.getRows().size() + 2][];
            data[0] = new String[]{"Вид несоответствия", "Всего, т", "Доработано, т", "Вид доработки", "Брак, т"};
            for (int i = 0; i < report.getRows().size(); i++) {
                ReportDto.ReportBySite.DefectRow dr = report.getRows().get(i);
                data[i + 1] = new String[]{
                        dr.getDefectType(),
                        formatBigDecimal(dr.getTotal()),
                        formatBigDecimal(dr.getReworked()),
                        dr.getReworkType() != null ? dr.getReworkType() : "",
                        formatBigDecimal(dr.getDefect())
                };
            }
            data[data.length - 1] = new String[]{"Итого",
                    formatBigDecimal(report.getTotals().getTotal()),
                    formatBigDecimal(report.getTotals().getReworked()),
                    "",
                    formatBigDecimal(report.getTotals().getDefect())
            };

            addTable(doc, data);
            doc.createParagraph();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByProductTypeToWord(ReportDto.ReportByProductType report) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph title = doc.createParagraph();
            title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(14);
            titleRun.setFontFamily("Times New Roman");
            titleRun.setText("ОТЧЕТ О НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО УЧАСТКУ " + report.getSiteName() + " ПО ВИДУ ПРОДУКЦИИ");

            org.apache.poi.xwpf.usermodel.XWPFParagraph period = doc.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun periodRun = period.createRun();
            periodRun.setFontSize(11);
            periodRun.setFontFamily("Times New Roman");
            periodRun.setText("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            doc.createParagraph();

            for (ReportDto.ReportByProductType.ProductTypeGroup grp : report.getGroups()) {
                org.apache.poi.xwpf.usermodel.XWPFParagraph grpTitle = doc.createParagraph();
                org.apache.poi.xwpf.usermodel.XWPFRun grpRun = grpTitle.createRun();
                grpRun.setBold(true);
                grpRun.setFontSize(12);
                grpRun.setFontFamily("Times New Roman");
                grpRun.setText(grp.getProductType());

                String[][] data = new String[grp.getRows().size() + 2][];
                data[0] = new String[]{"Вид несоответствия", "Всего, т", "Доработано, т", "Вид доработки", "Брак, т"};
                for (int i = 0; i < grp.getRows().size(); i++) {
                    ReportDto.ReportByProductType.DefectRow dr = grp.getRows().get(i);
                    data[i + 1] = new String[]{
                            dr.getDefectType(),
                            formatBigDecimal(dr.getTotal()),
                            formatBigDecimal(dr.getReworked()),
                            dr.getReworkType() != null ? dr.getReworkType() : "",
                            formatBigDecimal(dr.getDefect())
                    };
                }
                data[data.length - 1] = new String[]{"Итого",
                        formatBigDecimal(grp.getGroupTotals().getTotal()),
                        formatBigDecimal(grp.getGroupTotals().getReworked()),
                        "",
                        formatBigDecimal(grp.getGroupTotals().getDefect())
                };

                addTable(doc, data);
                doc.createParagraph();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByPersonnelToWord(ReportDto.ReportByPersonnel report) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph title = doc.createParagraph();
            title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(14);
            titleRun.setFontFamily("Times New Roman");
            titleRun.setText("ОТЧЕТ О НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО УЧАСТКУ " + report.getSiteName() + " ПО ПЕРСОНАЛУ");

            org.apache.poi.xwpf.usermodel.XWPFParagraph period = doc.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun periodRun = period.createRun();
            periodRun.setFontSize(11);
            periodRun.setFontFamily("Times New Roman");
            periodRun.setText("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            doc.createParagraph();

            for (ReportDto.ReportByPersonnel.PersonnelGroup grp : report.getGroups()) {
                org.apache.poi.xwpf.usermodel.XWPFParagraph grpTitle = doc.createParagraph();
                org.apache.poi.xwpf.usermodel.XWPFRun grpRun = grpTitle.createRun();
                grpRun.setBold(true);
                grpRun.setFontSize(12);
                grpRun.setFontFamily("Times New Roman");
                grpRun.setText("Перс№/таб№ " + grp.getPersonnelNumber());

                String[][] data = new String[grp.getRows().size() + 2][];
                data[0] = new String[]{"Вид несоответствия", "Всего, т", "Брак, т", "Примечание"};
                for (int i = 0; i < grp.getRows().size(); i++) {
                    ReportDto.ReportByPersonnel.DefectRow dr = grp.getRows().get(i);
                    data[i + 1] = new String[]{
                            dr.getDefectType(),
                            formatBigDecimal(dr.getTotal()),
                            formatBigDecimal(dr.getDefect()),
                            dr.getNote() != null ? dr.getNote() : ""
                    };
                }
                data[data.length - 1] = new String[]{"Итого",
                        formatBigDecimal(grp.getGroupTotals().getTotal()),
                        formatBigDecimal(grp.getGroupTotals().getDefect()),
                        ""
                };

                addTable(doc, data);
                doc.createParagraph();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportByPlantToWord(ReportDto.ReportByPlant report) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph title = doc.createParagraph();
            title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(14);
            titleRun.setFontFamily("Times New Roman");
            titleRun.setText("ДАННЫЕ ПО НЕСООТВЕТСТВУЮЩЕЙ ПРОДУКЦИИ ПО ЦЕХУ");

            org.apache.poi.xwpf.usermodel.XWPFParagraph period = doc.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun periodRun = period.createRun();
            periodRun.setFontSize(11);
            periodRun.setFontFamily("Times New Roman");
            periodRun.setText("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            doc.createParagraph();

            String[][] data = new String[report.getRows().size() + 1][];
            data[0] = new String[]{"Участки", "Производство, т", "Несоответствующая, т", "Несоответствующая, %", "Допустимый уровень, %"};
            for (int i = 0; i < report.getRows().size(); i++) {
                ReportDto.ReportByPlant.SiteRow sr = report.getRows().get(i);
                data[i + 1] = new String[]{
                        sr.getSiteName(),
                        formatBigDecimal(sr.getProduced()),
                        formatBigDecimal(sr.getNonconforming()),
                        formatBigDecimal(sr.getNonconformingPercent()),
                        formatBigDecimal(sr.getAllowablePercent())
                };
            }

            addTable(doc, data);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    // ===== WORD EXPORT — Свод по актам =====
    public byte[] exportByActsToWord(ReportDto.ReportByAct report) throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setFontFamily(FONT_NAME);
            titleRun.setText("Свод актов и справок о браке по цеху");

            org.apache.poi.xwpf.usermodel.XWPFParagraph period = doc.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun periodRun = period.createRun();
            periodRun.setFontSize(11);
            periodRun.setFontFamily(FONT_NAME);
            periodRun.setText("Период: " + report.getPeriodFrom() + " — " + report.getPeriodTo());

            org.apache.poi.xwpf.usermodel.XWPFParagraph produced = doc.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun producedRun = produced.createRun();
            producedRun.setFontSize(11);
            producedRun.setFontFamily(FONT_NAME);
            producedRun.setText("Производство за период: " + report.getProducedWeight() + " т");

            doc.createParagraph();

            String[] headers = {"Документ", "Вид", "Цех", "Вид несоответствия", "Причина брака",
                    "Всего, т", "Доработано, т", "Вид доработки", "Брак, т"};
            List<String[]> tableRows = new java.util.ArrayList<>();
            for (ReportDto.ReportByAct.ActGroup group : report.getGroups()) {
                tableRows.add(new String[]{
                        group.getActNumber(), group.getDocumentType(), group.getSiteName(),
                        "Всего по документу:", "-",
                        String.valueOf(group.getGroupTotals().getTotal()),
                        String.valueOf(group.getGroupTotals().getReworked()), "-",
                        String.valueOf(group.getGroupTotals().getDefect())});
                for (ReportDto.ReportByAct.DefectRow r : group.getRows()) {
                    tableRows.add(new String[]{
                            "", "", "",
                            r.getDefectType(), r.getCause(),
                            String.valueOf(r.getTotal()), String.valueOf(r.getReworked()),
                            r.getReworkType(), String.valueOf(r.getDefect())});
                }
            }
            tableRows.add(new String[]{
                    "Итого:", "", "", "", "",
                    String.valueOf(report.getTotals().getTotal()),
                    String.valueOf(report.getTotals().getReworked()), "",
                    String.valueOf(report.getTotals().getDefect())});

            String[][] data = new String[1 + tableRows.size()][headers.length];
            System.arraycopy(headers, 0, data[0], 0, headers.length);
            for (int i = 0; i < tableRows.size(); i++) {
                data[i + 1] = tableRows.get(i);
            }
            addTable(doc, data);

            XWPFParagraph note = doc.createParagraph();
            XWPFRun noteRun = note.createRun();
            noteRun.setFontSize(9);
            noteRun.setFontFamily(FONT_NAME);
            noteRun.setText("Процент брака: " + report.getTotals().getDefectPercent() + "%");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    private void addTable(org.apache.poi.xwpf.usermodel.XWPFDocument doc, String[][] data) {
        org.apache.poi.xwpf.usermodel.XWPFTable table = doc.createTable(data.length, data[0].length);

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                org.apache.poi.xwpf.usermodel.XWPFTableRow row = table.getRow(i);
                if (row == null) {
                    row = table.createRow();
                }
                org.apache.poi.xwpf.usermodel.XWPFTableCell cell = row.getCell(j);
                if (cell == null) {
                    cell = row.addNewTableCell();
                }
                cell.setText(data[i][j]);

                for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : cell.getParagraphs()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFRun run : p.getRuns()) {
                        run.setFontFamily("Times New Roman");
                        run.setFontSize(10);
                        if (i == 0) {
                            run.setBold(true);
                        }
                    }
                }
            }
        }
    }

    // ===== HELPER METHODS =====

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setFontName(FONT_NAME);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints(FONT_SIZE);
        font.setFontName(FONT_NAME);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createNumStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        font.setFontName(FONT_NAME);
        style.setFont(font);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.000"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints(FONT_SIZE);
        font.setFontName(FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle createCenterStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private String formatBigDecimal(BigDecimal value) {
        if (value == null) return "";
        return String.format("%.3f", value.doubleValue());
    }
}
