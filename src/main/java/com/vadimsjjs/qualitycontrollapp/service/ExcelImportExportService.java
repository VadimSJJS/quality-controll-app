package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.ExcelImportRequest;
import com.vadimsjjs.qualitycontrollapp.entity.*;
import com.vadimsjjs.qualitycontrollapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportExportService {

    private final NonconformingProductRepository productRepository;
    private final ProductionSiteRepository siteRepository;
    private final DetectionSourceRepository detectionSourceRepository;
    private final DefectTypeRepository defectTypeRepository;
    private final DefectCauseRepository defectCauseRepository;
    private final ReworkTypeRepository reworkTypeRepository;
    private final DiameterRepository diameterRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ввод данных");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Ввод данных по несоответствующей продукции (Рисунок 1)");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 18));

            String[] columns = {
                    "дата выявления", "бригада", "диаметр", "код", "номер катушки",
                    "номер плавки", "марка стали", "номер стана", "ключ заготовки",
                    "табельный/персональный номер", "бригада изготовителя", "количество, шт",
                    "примечание", "масса, т", "вид несоответствия", "причина", "подпричина",
                    "кем выявлено"
            };

            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            Row exampleRow = sheet.createRow(2);
            String[] exampleData = {
                    "20.07.2026", "2", "2.50", "1001", "К-001",
                    "П-2026-045", "80", "Стан №3", "Ключ-001",
                    "12345", "2", "15",
                    "Выявлено при входном контроле", "1.250", "Царапина", "Обрыв волоки", "Преждевременный износ волоки",
                    "ОТК"
            };
            for (int i = 0; i < exampleData.length; i++) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(exampleData[i]);
                cell.setCellStyle(textStyle);
            }

            Row reworkLabelRow = sheet.createRow(3);
            String[] reworkLabels = {"дата доработки", "вид доработки", "количество доработанного", "масса доработанного", "примечание доработки"};
            for (int i = 0; i < reworkLabels.length; i++) {
                Cell cell = reworkLabelRow.createCell(i);
                cell.setCellValue(reworkLabels[i]);
                cell.setCellStyle(headerStyle);
            }

            Row reworkExampleRow = sheet.createRow(4);
            String[] reworkExample = {"21.07.2026", "Восстановление", "14", "1.200", "Доработка выполнена"};
            for (int i = 0; i < reworkExample.length; i++) {
                Cell cell = reworkExampleRow.createCell(i);
                cell.setCellValue(reworkExample[i]);
                cell.setCellStyle(textStyle);
            }

            Row noteRow = sheet.createRow(5);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("Примечание: * Заполните данные по шаблону. Начинайте с 7-й строки (индекс 6).");
            noteCell.setCellStyle(textStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(5, 5, 0, 18));

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    @Transactional
    public List<NonconformingProduct> importFromExcel(MultipartFile file) throws IOException {
        List<NonconformingProduct> products = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 6; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                if (isEmptyRow(row)) continue;

                try {
                    NonconformingProduct product = parseRow(row);
                    products.add(product);
                } catch (Exception e) {
                    log.error("Ошибка при обработке строки {}: {}", rowIndex, e.getMessage());
                    throw new RuntimeException("Ошибка в строке " + (rowIndex + 1) + ": " + e.getMessage());
                }
            }
        }

        return productRepository.saveAll(products);
    }

    private NonconformingProduct parseRow(Row row) {
        NonconformingProduct product = new NonconformingProduct();

        product.setDetectionDate(getDateCell(row, 0));

        if (getLongCell(row, 1) != null) {
            product.setBrigade(getLongCell(row, 1));
        }

        String diameterValue = getStringCell(row, 2);
        if (diameterValue != null && !diameterValue.isEmpty()) {
            Diameter diameter = diameterRepository.findAll().stream()
                    .filter(d -> d.getDiameter().equals(diameterValue))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Диаметр не найден: " + diameterValue));
            product.setDiameter(diameter);
        }

        if (getLongCell(row, 3) != null) {
            product.setProductCode(getLongCell(row, 3));
        }

        product.setReelNumber(getStringCell(row, 4));
        product.setHeatNumber(getStringCell(row, 5));
        product.setSteelGrade(getStringCell(row, 6));
        product.setEquipmentKey(getStringCell(row, 7));
        product.setWorkpieceKey(getStringCell(row, 8));

        if (getLongCell(row, 9) != null) {
            product.setOperatorPersonalNumber(getLongCell(row, 9));
        }

        if (getLongCell(row, 10) != null) {
            product.setManufacturerBrigade(getLongCell(row, 10));
        }

        if (getIntegerCell(row, 11) != null) {
//            product.setCount(getIntegerCell(row, 11));
        }

        product.setNote(getStringCell(row, 12));

        if (getDoubleCell(row, 13) != null) {
            product.setWeightTonnes(BigDecimal.valueOf(getDoubleCell(row, 13)));
        }

        String defectTypeName = getStringCell(row, 14);
        if (defectTypeName != null && !defectTypeName.isEmpty()) {
            DefectType defectType = defectTypeRepository.findByDefectName(defectTypeName)
                    .orElseThrow(() -> new RuntimeException("Вид несоответствия не найден: " + defectTypeName));
            product.setDefectType(defectType);
        }

        String causeName = getStringCell(row, 15);
        if (causeName != null && !causeName.isEmpty()) {
            DefectCause cause = defectCauseRepository.findByCauseName(causeName)
                    .orElseThrow(() -> new RuntimeException("Причина не найдена: " + causeName));
            product.setDefectCause(cause);
        }

        String subcauseName = getStringCell(row, 16);
        if (subcauseName != null && !subcauseName.isEmpty()) {
            DefectCause subcause = defectCauseRepository.findByCauseName(subcauseName)
                    .orElseThrow(() -> new RuntimeException("Подпричина не найдена: " + subcauseName));
            product.setDefectSubcause(subcause);
        }

        String sourceName = getStringCell(row, 17);
        if (sourceName != null && !sourceName.isEmpty()) {
            DetectionSource source = detectionSourceRepository.findBySourceName(sourceName)
                    .orElseThrow(() -> new RuntimeException("Источник выявления не найден: " + sourceName));
            product.setDetectionSource(source);
        }

        LocalDate reworkDate = getDateCell(row, 18);
        if (reworkDate == null) {
            Row reworkRow = row.getSheet().getRow(4);
            if (reworkRow != null) {
                reworkDate = getDateCell(reworkRow, 0);
            }
        }
        product.setReworkDate(reworkDate);

        String reworkTypeName = getStringCell(row, 19);
        if (reworkTypeName == null || reworkTypeName.isEmpty()) {
            Row reworkRow = row.getSheet().getRow(4);
            if (reworkRow != null) {
                reworkTypeName = getStringCell(reworkRow, 1);
            }
        }
        if (reworkTypeName != null && !reworkTypeName.isEmpty()) {
//            ReworkType reworkType = reworkTypeRepository.findByReworkName(reworkTypeName)
//                    .orElseThrow(() -> new RuntimeException("Вид доработки не найден: " + reworkTypeName));
//            product.setReworkType(reworkType);
        }

        Integer reworkQuantity = getIntegerCell(row, 20);
        if (reworkQuantity == null) {
            Row reworkRow = row.getSheet().getRow(4);
            if (reworkRow != null) {
                reworkQuantity = getIntegerCell(reworkRow, 2);
            }
        }
        if (reworkQuantity != null) {
            product.setReworkQuantity(reworkQuantity);
        }

        Double reworkWeight = getDoubleCell(row, 21);
        if (reworkWeight == null) {
            Row reworkRow = row.getSheet().getRow(4);
            if (reworkRow != null) {
                reworkWeight = getDoubleCell(reworkRow, 3);
            }
        }
        if (reworkWeight != null) {
            product.setReworkWeightTonnes(BigDecimal.valueOf(reworkWeight));
        }

        String reworkNote = getStringCell(row, 22);
        if (reworkNote == null || reworkNote.isEmpty()) {
            Row reworkRow = row.getSheet().getRow(4);
            if (reworkRow != null) {
                reworkNote = getStringCell(reworkRow, 4);
            }
        }
        if (reworkNote != null && !reworkNote.isEmpty()) {
            if (product.getNote() == null || product.getNote().isEmpty()) {
                product.setNote(reworkNote);
            } else {
                product.setNote(product.getNote() + " | " + reworkNote);
            }
        }

        return product;
    }

    private String getStringCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        return cell.getCellType() == CellType.STRING ? cell.getStringCellValue() :
                cell.getCellType() == CellType.NUMERIC ? String.valueOf(cell.getNumericCellValue()) : null;
    }

    private Long getLongCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }
        return null;
    }

    private Integer getIntegerCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        return null;
    }

    private Double getDoubleCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        return null;
    }

    private LocalDate getDateCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return LocalDate.parse(cell.getStringCellValue(), DATE_FORMATTER);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (int i = 0; i < 18; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
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

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy"));
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.000"));
        return style;
    }

    private CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }
}