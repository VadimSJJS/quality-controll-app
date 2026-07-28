package com.vadimsjjs.qualitycontrollapp.controller;

import com.vadimsjjs.qualitycontrollapp.dto.EquipmentDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.PersonnelDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.ReportDto;
import com.vadimsjjs.qualitycontrollapp.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/personnel-defects")
    public ResponseEntity<PersonnelDefectReport> getPersonnelDefectReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        PersonnelDefectReport report = reportService.getPersonnelDefectReport(dateFrom, dateTo);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/by-site")
    public ResponseEntity<ReportDto.ReportBySite> getReportBySite(
            @RequestParam String siteName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getReportBySite(siteName, dateFrom, dateTo));
    }

    @GetMapping("/by-product-type")
    public ResponseEntity<ReportDto.ReportByProductType> getReportByProductType(
            @RequestParam String siteName,
            @RequestParam(defaultValue = "диаметр") String productTypeField,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getReportByProductType(siteName, productTypeField, dateFrom, dateTo));
    }

    @GetMapping("/by-product-cause")
    public ResponseEntity<ReportDto.ReportByProductAndCause> getReportByProductAndCause(
            @RequestParam String siteName,
            @RequestParam(defaultValue = "диаметр") String productTypeField,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getReportByProductAndCause(siteName, productTypeField, dateFrom, dateTo));
    }

    @GetMapping("/by-brigade")
    public ResponseEntity<ReportDto.ReportByBrigade> getReportByBrigade(
            @RequestParam String siteName,
            @RequestParam Long brigadeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getReportByBrigade(siteName, brigadeId, dateFrom, dateTo));
    }

    @GetMapping("/by-equipment")
    public ResponseEntity<ReportDto.ReportByEquipment> getReportByEquipment(
            @RequestParam String siteName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getReportByEquipment(siteName, dateFrom, dateTo));
    }

    @GetMapping("/by-personnel")
    public ResponseEntity<ReportDto.ReportByPersonnel> getReportByPersonnel(
            @RequestParam String siteName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getReportByPersonnel(siteName, dateFrom, dateTo));
    }

    @GetMapping("/personnel-defects-v2")
    public ResponseEntity<PersonnelDefectReport> getPersonnelDefectReportV2(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getPersonnelDefectReportV2(dateFrom, dateTo));
    }

    @GetMapping("/by-plant")
    public ResponseEntity<ReportDto.ReportByPlant> getReportByPlant(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getReportByPlant(dateFrom, dateTo));
    }

    @GetMapping("/by-fault")
    public ResponseEntity<ReportDto.ReportByFault> getReportByFault(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getReportByFault(dateFrom, dateTo));
    }

    @GetMapping("/equipment-defects")
    public ResponseEntity<EquipmentDefectReport> getEquipmentDefectReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getEquipmentDefectReport(dateFrom, dateTo));
    }
}