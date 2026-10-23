package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.dto.DefectFilterDto;
import com.vadimsjjs.qualitycontrollapp.dto.DistributionReport;
import com.vadimsjjs.qualitycontrollapp.dto.EquipmentDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.ParetoReport;
import com.vadimsjjs.qualitycontrollapp.dto.PersonnelDefectReport;
import com.vadimsjjs.qualitycontrollapp.dto.ReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Фасад отчётов: делегирует расчёты специализированным сервисам,
 * сохраняя прежний API для контроллеров.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SiteReportService siteReportService;
    private final PersonnelDefectReportService personnelDefectReportService;
    private final FaultReportService faultReportService;
    private final EquipmentDefectReportService equipmentDefectReportService;
    private final ParetoReportService paretoReportService;
    private final DistributionReportService distributionReportService;

    @Transactional(readOnly = true)
    public ReportDto.ReportBySite getReportBySite(String siteCode, DefectFilterDto filter) {
        return siteReportService.getReportBySite(siteCode, filter);
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByProductType getReportByProductType(String siteCode, String productTypeField, DefectFilterDto filter) {
        return siteReportService.getReportByProductType(siteCode, productTypeField, filter);
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByProductAndCause getReportByProductAndCause(String siteCode, String productTypeField, DefectFilterDto filter) {
        return siteReportService.getReportByProductAndCause(siteCode, productTypeField, filter);
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByBrigade getReportByBrigade(String siteCode, Long brigadeId, DefectFilterDto filter) {
        return siteReportService.getReportByBrigade(siteCode, brigadeId, filter);
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByEquipment getReportByEquipment(String siteCode, DefectFilterDto filter) {
        return siteReportService.getReportByEquipment(siteCode, filter);
    }

    @Transactional(readOnly = true)
    public PersonnelDefectReport getPersonnelDefectReport(DefectFilterDto filter) {
        return personnelDefectReportService.getPersonnelDefectReport(filter);
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByPersonnel getReportByPersonnel(String siteCode, DefectFilterDto filter) {
        return siteReportService.getReportByPersonnel(siteCode, filter);
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByPlant getReportByPlant(DefectFilterDto filter) {
        return siteReportService.getReportByPlant(filter);
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByFault getReportByFault(DefectFilterDto filter) {
        return faultReportService.getReportByFault(filter);
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportByAct getReportByActs(DefectFilterDto filter) {
        return faultReportService.getReportByActs(filter);
    }

    @Transactional(readOnly = true)
    public EquipmentDefectReport getEquipmentDefectReport(DefectFilterDto filter) {
        return equipmentDefectReportService.getEquipmentDefectReport(filter);
    }

    @Transactional(readOnly = true)
    public ParetoReport getParetoReport(String siteCode, String groupingType, DefectFilterDto filter) {
        return paretoReportService.getParetoReport(siteCode, groupingType, filter);
    }

    @Transactional(readOnly = true)
    public DistributionReport getDistributionReport(String dimension, DefectFilterDto filter) {
        return distributionReportService.getDistributionReport(dimension, filter);
    }
}
