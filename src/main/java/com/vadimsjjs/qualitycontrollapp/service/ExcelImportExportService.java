package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.ExcelImportResult;
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
    private static final int DATA_START_ROW = 6;

    private static final int COL_DETECTION_DATE = 0;
    private static final int COL_SITE = 1;
    private static final int COL_BRIGADE = 2;
    private static final int COL_DIAMETER = 3;
    private static final int COL_PRODUCT_CODE = 4;
    private static final int COL_REEL_NUMBER = 5;
    private static final int COL_HEAT_NUMBER = 6;
    private static final int COL_STEEL_GRADE = 7;
    private static final int COL_EQUIPMENT_KEY = 8;
    private static final int COL_WORKPIECE_KEY = 9;
    private static final int COL_OPERATOR_PERSONAL_NUMBER = 10;
    private static final int COL_MANUFACTURER_BRIGADE = 11;
    private static final int COL_QUANTITY = 12;
    private static final int COL_NOTE = 13;
    private static final int COL_WEIGHT_TONNES = 14;
    private static final int COL_DEFECT_TYPE = 15;
    private static final int COL_CAUSE = 16;
    private static final int COL_SUBCAUSE = 17;
    private static final int COL_DETECTION_SOURCE = 18;
    private static final int COL_REWORK_DATE = 19;
    private static final int COL_REWORK_TYPE = 20;
    private static final int COL_REWORK_QUANTITY = 21;
    private static final int COL_REWORK_WEIGHT = 22;
    private static final int COL_REWORK_NOTE = 23;

    private static final int TOTAL_COLUMNS = 24;

    public byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ввод данных");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Ввод данных по несоответствующей продукции (Рисунок 1)");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, TOTAL_COLUMNS - 1));

            String[] columns = {
                    "дата выявления", "участок", "бригада", "диаметр", "код",
                    "номер катушки", "номер плавки", "марка стали", "номер стана",
                    "ключ заготовки", "табельный/персональный номер",
                    "бригада изготовителя", "количество, шт", "примечание",
                    "масса, т", "вид несоответствия", "причина", "подпричина",
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
                    "20.07.2026", "КУ-2", "2", "2.50", "1001",
                    "К-001", "П-2026-045", "80", "Стан №3",
                    "Ключ-001", "12345", "2", "15",
                    "Выявлено при входном контроле", "1.250",
                    "Царапина", "Обрыв волоки", "Преждевременный износ волоки",
                    "ОТК"
            };
            for (int i = 0; i < exampleData.length; i++) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(exampleData[i]);
                cell.setCellStyle(textStyle);
            }

            Row reworkLabelRow = sheet.createRow(3);
            String[] reworkLabels = {
                    "дата доработки", "вид доработки", "количество доработанного",
                    "масса доработанного", "примечание доработки"
            };
            for (int i = 0; i < reworkLabels.length; i++) {
                Cell cell = reworkLabelRow.createCell(COL_REWORK_DATE + i);
                cell.setCellValue(reworkLabels[i]);
                cell.setCellStyle(headerStyle);
            }

            Row reworkExampleRow = sheet.createRow(4);
            String[] reworkExample = {"21.07.2026", "Восстановление", "14", "1.200", "Доработка выполнена"};
            for (int i = 0; i < reworkExample.length; i++) {
                Cell cell = reworkExampleRow.createCell(COL_REWORK_DATE + i);
                cell.setCellValue(reworkExample[i]);
                cell.setCellStyle(textStyle);
            }

            // Row 5: note
            Row noteRow = sheet.createRow(5);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("Примечание: * Заполните данные по шаблону. Начинайте с 7-й строки (индекс 6). " +
                    "Поля, отмеченные * обязательны: дата выявления, участок, масса, вид несоответствия, кем выявлено.");
            noteCell.setCellStyle(textStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(5, 5, 0, TOTAL_COLUMNS - 1));

            for (int i = 0; i < TOTAL_COLUMNS; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    @Transactional
    public ExcelImportResult importFromExcel(MultipartFile file) throws IOException {
        List<NonconformingProduct> products = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row)) continue;

                try {
                    NonconformingProduct product = parseRow(row);
                    products.add(product);
                } catch (Exception e) {
                    String msg = "Строка " + (rowIndex + 1) + ": " + e.getMessage();
                    errors.add(msg);
                    log.error(msg);
                }
            }
        }

        if (products.isEmpty()) {
            if (!errors.isEmpty()) {
                throw new RuntimeException("Импорт не выполнен:\n" + String.join("\n", errors));
            }
            throw new RuntimeException("Файл не содержит данных для импорта (начиная с 7-й строки)");
        }

        productRepository.saveAll(products);

        if (!errors.isEmpty()) {
            log.warn("Импорт с ошибками ({} из {} записей):\n{}",
                    errors.size(), errors.size() + products.size(), String.join("\n", errors));
        }

        return ExcelImportResult.builder()
                .imported(products.size())
                .errors(errors)
                .build();
    }

    private NonconformingProduct parseRow(Row row) {
        NonconformingProduct product = new NonconformingProduct();

        // ===== REQUIRED: detectionDate =====
        LocalDate detectionDate = getDateCell(row, COL_DETECTION_DATE);
        if (detectionDate == null) {
            throw new RuntimeException("Не указана дата выявления (поле A)");
        }
        product.setDetectionDate(detectionDate);

        // ===== REQUIRED: productionSite =====
        String siteCode = getStringCell(row, COL_SITE);
        if (siteCode == null || siteCode.trim().isEmpty()) {
            throw new RuntimeException("Не указан участок (поле B)");
        }
        ProductionSite site = siteRepository.findBySiteCode(siteCode.trim())
                .orElseThrow(() -> new RuntimeException("Участок не найден: " + siteCode));
        product.setProductionSite(site);

        // ===== OPTIONAL: brigade =====
        if (getLongCell(row, COL_BRIGADE) != null) {
            product.setBrigade(getLongCell(row, COL_BRIGADE));
        }

        // ===== OPTIONAL: diameter =====
        String diameterValue = getStringCell(row, COL_DIAMETER);
        if (diameterValue != null && !diameterValue.trim().isEmpty()) {
            String dim = diameterValue.trim();
            Diameter diameter = diameterRepository.findAll().stream()
                    .filter(d -> dim.equals(d.getDiameter()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Диаметр не найден: " + dim));
            product.setDiameter(diameter);
        }

        // ===== OPTIONAL: productCode =====
        if (getLongCell(row, COL_PRODUCT_CODE) != null) {
            product.setProductCode(getLongCell(row, COL_PRODUCT_CODE));
        }

        // ===== OPTIONAL: reelNumber =====
        product.setReelNumber(getStringCell(row, COL_REEL_NUMBER));

        // ===== OPTIONAL: heatNumber =====
        product.setHeatNumber(getStringCell(row, COL_HEAT_NUMBER));

        // ===== OPTIONAL: steelGrade =====
        String steelGradeStr = getStringCell(row, COL_STEEL_GRADE);
        if (steelGradeStr != null && !steelGradeStr.trim().isEmpty()) {
            product.setSteelGrade(steelGradeStr.trim());
        }

        // ===== OPTIONAL: equipmentKey =====
        product.setEquipmentKey(getStringCell(row, COL_EQUIPMENT_KEY));

        // ===== OPTIONAL: workpieceKey =====
        product.setWorkpieceKey(getStringCell(row, COL_WORKPIECE_KEY));

        // ===== OPTIONAL: operatorPersonalNumber =====
        if (getLongCell(row, COL_OPERATOR_PERSONAL_NUMBER) != null) {
            product.setOperatorPersonalNumber(getLongCell(row, COL_OPERATOR_PERSONAL_NUMBER));
        }

        // ===== OPTIONAL: manufacturerBrigade =====
        if (getLongCell(row, COL_MANUFACTURER_BRIGADE) != null) {
            product.setManufacturerBrigade(getLongCell(row, COL_MANUFACTURER_BRIGADE));
        }

        // ===== OPTIONAL: quantity =====
        if (getIntegerCell(row, COL_QUANTITY) != null) {
            product.setQuantity(getIntegerCell(row, COL_QUANTITY));
        }

        // ===== OPTIONAL: note =====
        product.setNote(getStringCell(row, COL_NOTE));

        // ===== REQUIRED: weightTonnes =====
        Double weight = getDoubleCell(row, COL_WEIGHT_TONNES);
        if (weight == null || weight <= 0) {
            throw new RuntimeException("Не указана или некорректна масса (поле N)");
        }
        product.setWeightTonnes(BigDecimal.valueOf(weight));

        // ===== REQUIRED: defectType =====
        String defectTypeName = getStringCell(row, COL_DEFECT_TYPE);
        if (defectTypeName == null || defectTypeName.trim().isEmpty()) {
            throw new RuntimeException("Не указан вид несоответствия (поле O)");
        }
        DefectType defectType = defectTypeRepository.findByDefectName(defectTypeName.trim())
                .orElseThrow(() -> new RuntimeException("Вид несоответствия не найден: " + defectTypeName));
        product.setDefectType(defectType);

        // ===== OPTIONAL: cause =====
        String causeName = getStringCell(row, COL_CAUSE);
        if (causeName != null && !causeName.trim().isEmpty()) {
            DefectCause cause = defectCauseRepository.findByCauseName(causeName.trim())
                    .orElseThrow(() -> new RuntimeException("Причина не найдена: " + causeName));
            product.setDefectCause(cause);
        }

        // ===== OPTIONAL: subcause =====
        String subcauseName = getStringCell(row, COL_SUBCAUSE);
        if (subcauseName != null && !subcauseName.trim().isEmpty()) {
            DefectCause subcause = defectCauseRepository.findByCauseName(subcauseName.trim())
                    .orElseThrow(() -> new RuntimeException("Подпричина не найдена: " + subcauseName));
            product.setDefectSubcause(subcause);
        }

        // ===== REQUIRED: detectionSource =====
        String sourceName = getStringCell(row, COL_DETECTION_SOURCE);
        if (sourceName == null || sourceName.trim().isEmpty()) {
            throw new RuntimeException("Не указан источник выявления (поле S)");
        }
        DetectionSource source = detectionSourceRepository.findBySourceName(sourceName.trim())
                .orElseThrow(() -> new RuntimeException("Источник выявления не найден: " + sourceName));
        product.setDetectionSource(source);

        // ===== REWORK FIELDS (all optional) =====
        product.setReworkDate(getDateCell(row, COL_REWORK_DATE));

        String reworkTypeName = getStringCell(row, COL_REWORK_TYPE);
        if (reworkTypeName != null && !reworkTypeName.trim().isEmpty()) {
            ReworkType reworkType = reworkTypeRepository.findByReworkName(reworkTypeName.trim())
                    .orElseThrow(() -> new RuntimeException("Вид доработки не найден: " + reworkTypeName));
            product.setReworkType(reworkType);
        }

        if (getIntegerCell(row, COL_REWORK_QUANTITY) != null) {
            product.setReworkQuantity(getIntegerCell(row, COL_REWORK_QUANTITY));
        }

        Double reworkWeight = getDoubleCell(row, COL_REWORK_WEIGHT);
        if (reworkWeight != null) {
            product.setReworkWeightTonnes(BigDecimal.valueOf(reworkWeight));
        }

        String reworkNote = getStringCell(row, COL_REWORK_NOTE);
        if (reworkNote != null && !reworkNote.trim().isEmpty()) {
            if (product.getNote() == null || product.getNote().isEmpty()) {
                product.setNote(reworkNote.trim());
            } else {
                product.setNote(product.getNote() + " | " + reworkNote.trim());
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
        for (int i = 0; i < TOTAL_COLUMNS; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && cell.getCellType() != CellType._NONE
                    && !getStringCellValueSafe(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getStringCellValueSafe(Cell cell) {
        if (cell == null) return "";
        try {
            if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
            if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        } catch (Exception ignored) {
        }
        return "";
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

    private CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }
}