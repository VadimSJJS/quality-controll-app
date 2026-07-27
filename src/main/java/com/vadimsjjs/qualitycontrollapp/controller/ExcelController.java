package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.entity.NonconformingProduct;
import com.vadimsjjs.qualitycontrollapp.service.ExcelImportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelImportExportService excelService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] template = excelService.generateTemplate();

            String filename = "Шаблон_ввода_данных_по_несоответствующей_продукции.xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                            java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                                    .replace("+", "%20"))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(template);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            List<NonconformingProduct> imported = excelService.importFromExcel(file);
            return ResponseEntity.ok("Импортировано " + imported.size() + " записей");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка импорта: " + e.getMessage());
        }
    }
}