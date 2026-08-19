package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.dto.EquipmentDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.ParetoReport;
import com.vadimsjjs.qualitycontrollapp.dto.PersonnelDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.ReportDto;
import com.vadimsjjs.qualitycontrollapp.service.ExcelExportService;
import com.vadimsjjs.qualitycontrollapp.service.ReportExportService;
import com.vadimsjjs.qualitycontrollapp.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;
    private final ExcelExportService excelExportService;

    @GetMapping("/personnel-defects")
    public ResponseEntity<PersonnelDefectReport> getPersonnelDefectReport(
            @ModelAttribute @Valid DefectFilterDto filter) {
        PersonnelDefectReport report = reportService.getPersonnelDefectReport(filter);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/by-site")
    public ResponseEntity<ReportDto.ReportBySite> getReportBySite(
            @RequestParam String siteCode,
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getReportBySite(siteCode, filter));
    }

    @GetMapping("/by-product-type")
    public ResponseEntity<ReportDto.ReportByProductType> getReportByProductType(
            @RequestParam String siteCode,
            @RequestParam(defaultValue = "диаметр") String productTypeField,
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getReportByProductType(siteCode, productTypeField, filter));
    }

    @GetMapping("/by-product-cause")
    public ResponseEntity<ReportDto.ReportByProductAndCause> getReportByProductAndCause(
            @RequestParam String siteCode,
            @RequestParam(defaultValue = "диаметр") String productTypeField,
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getReportByProductAndCause(siteCode, productTypeField, filter));
    }

    @GetMapping("/by-brigade")
    public ResponseEntity<ReportDto.ReportByBrigade> getReportByBrigade(
            @RequestParam String siteCode,
            @RequestParam Long brigadeId,
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getReportByBrigade(siteCode, brigadeId, filter));
    }

    @GetMapping("/by-equipment")
    public ResponseEntity<ReportDto.ReportByEquipment> getReportByEquipment(
            @RequestParam String siteCode,
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getReportByEquipment(siteCode, filter));
    }

    @GetMapping("/by-personnel")
    public ResponseEntity<ReportDto.ReportByPersonnel> getReportByPersonnel(
            @RequestParam String siteCode,
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getReportByPersonnel(siteCode, filter));
    }

    @GetMapping("/personnel-defects-v2")
    public ResponseEntity<PersonnelDefectReport> getPersonnelDefectReportV2(
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getPersonnelDefectReportV2(filter));
    }

    @GetMapping("/by-plant")
    public ResponseEntity<ReportDto.ReportByPlant> getReportByPlant(
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getReportByPlant(filter));
    }
    @GetMapping("/by-fault")
    public ResponseEntity<ReportDto.ReportByFault> getReportByFault(
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getReportByFault(filter));
    }
    @GetMapping("/equipment-defects")
    public ResponseEntity<EquipmentDefectReport> getEquipmentDefectReport(
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getEquipmentDefectReport(filter));
    }

    @GetMapping("/pareto")
    public ResponseEntity<ParetoReport> getParetoReport(
            @RequestParam String siteCode,
            @RequestParam(defaultValue = "defect") String groupingType,
            @ModelAttribute @Valid DefectFilterDto filter) {
        return ResponseEntity.ok(reportService.getParetoReport(siteCode, groupingType, filter));
    }

    // ===== EXPORT ENDPOINTS =====

    @GetMapping("/export/excel/by-site")
    public ResponseEntity<byte[]> exportBySiteToExcel(
            @RequestParam String siteCode,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportBySite report = reportService.getReportBySite(siteCode, filter);
        byte[] data = reportExportService.exportBySiteToExcel(report);
        return downloadResponse(data, "отчет_по_участку.xlsx");
    }

    @GetMapping("/export/excel/by-product-type")
    public ResponseEntity<byte[]> exportByProductTypeToExcel(
            @RequestParam String siteCode,
            @RequestParam(defaultValue = "диаметр") String productTypeField,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByProductType report = reportService.getReportByProductType(siteCode, productTypeField, filter);
        byte[] data = reportExportService.exportByProductTypeToExcel(report);
        return downloadResponse(data, "отчет_по_виду_продукции.xlsx");
    }

    @GetMapping("/export/excel/by-product-cause")
    public ResponseEntity<byte[]> exportByProductAndCauseToExcel(
            @RequestParam String siteCode,
            @RequestParam(defaultValue = "диаметр") String productTypeField,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByProductAndCause report = reportService.getReportByProductAndCause(siteCode, productTypeField, filter);
        byte[] data = reportExportService.exportByProductAndCauseToExcel(report);
        return downloadResponse(data, "отчет_по_видам_и_причинам.xlsx");
    }

    @GetMapping("/export/excel/by-brigade")
    public ResponseEntity<byte[]> exportByBrigadeToExcel(
            @RequestParam String siteCode,
            @RequestParam Long brigadeId,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByBrigade report = reportService.getReportByBrigade(siteCode, brigadeId, filter);
        byte[] data = reportExportService.exportByBrigadeToExcel(report);
        return downloadResponse(data, "отчет_по_бригаде.xlsx");
    }

    @GetMapping("/export/excel/by-equipment")
    public ResponseEntity<byte[]> exportByEquipmentToExcel(
            @RequestParam String siteCode,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByEquipment report = reportService.getReportByEquipment(siteCode, filter);
        byte[] data = reportExportService.exportByEquipmentToExcel(report);
        return downloadResponse(data, "отчет_по_оборудованию.xlsx");
    }

    @GetMapping("/export/excel/by-personnel")
    public ResponseEntity<byte[]> exportByPersonnelToExcel(
            @RequestParam String siteCode,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByPersonnel report = reportService.getReportByPersonnel(siteCode, filter);
        byte[] data = reportExportService.exportByPersonnelToExcel(report);
        return downloadResponse(data, "отчет_по_персоналу.xlsx");
    }

    @GetMapping("/export/excel/by-plant")
    public ResponseEntity<byte[]> exportByPlantToExcel(
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByPlant report = reportService.getReportByPlant(filter);
        byte[] data = reportExportService.exportByPlantToExcel(report);
        return downloadResponse(data, "отчет_по_цеху.xlsx");
    }

    @GetMapping("/export/excel/by-fault")
    public ResponseEntity<byte[]> exportByFaultToExcel(
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByFault report = reportService.getReportByFault(filter);
        byte[] data = reportExportService.exportByFaultToExcel(report);
        return downloadResponse(data, "отчет_по_вине.xlsx");
    }

    @GetMapping("/export/excel/pareto")
    public ResponseEntity<byte[]> exportParetoToExcel(
            @RequestParam String siteCode,
            @RequestParam(defaultValue = "defect") String groupingType,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ParetoReport report = reportService.getParetoReport(siteCode, groupingType, filter);
        byte[] data = reportExportService.exportParetoToExcel(report);
        return downloadResponse(data, "диаграмма_парето.xlsx");
    }

    // ===== WORD EXPORT =====

    @GetMapping("/export/word/by-site")
    public ResponseEntity<byte[]> exportBySiteToWord(
            @RequestParam String siteCode,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportBySite report = reportService.getReportBySite(siteCode, filter);
        byte[] data = reportExportService.exportBySiteToWord(report);
        return downloadResponse(data, "отчет_по_участку.doc");
    }

    @GetMapping("/export/word/by-product-type")
    public ResponseEntity<byte[]> exportByProductTypeToWord(
            @RequestParam String siteCode,
            @RequestParam(defaultValue = "диаметр") String productTypeField,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByProductType report = reportService.getReportByProductType(siteCode, productTypeField, filter);
        byte[] data = reportExportService.exportByProductTypeToWord(report);
        return downloadResponse(data, "отчет_по_виду_продукции.doc");
    }

    @GetMapping("/export/word/by-personnel")
    public ResponseEntity<byte[]> exportByPersonnelToWord(
            @RequestParam String siteCode,
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByPersonnel report = reportService.getReportByPersonnel(siteCode, filter);
        byte[] data = reportExportService.exportByPersonnelToWord(report);
        return downloadResponse(data, "отчет_по_персоналу.doc");
    }

    @GetMapping("/export/word/by-plant")
    public ResponseEntity<byte[]> exportByPlantToWord(
            @ModelAttribute @Valid DefectFilterDto filter) throws Exception {
        ReportDto.ReportByPlant report = reportService.getReportByPlant(filter);
        byte[] data = reportExportService.exportByPlantToWord(report);
        return downloadResponse(data, "отчет_по_цеху.docx");
    }

    private ResponseEntity<byte[]> downloadResponse(byte[] data, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}