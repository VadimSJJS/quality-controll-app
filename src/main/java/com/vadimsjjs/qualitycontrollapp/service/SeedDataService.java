package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.entity.*;
import com.vadimsjjs.qualitycontrollapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedDataService {

    private final NonconformingProductRepository nonconformingRepository;
    private final ProductionSiteRepository productionSiteRepository;
    private final DefectTypeRepository defectTypeRepository;
    private final DefectCauseRepository defectCauseRepository;
    private final DetectionSourceRepository detectionSourceRepository;
    private final ReworkTypeRepository reworkTypeRepository;
    private final DiameterRepository diameterRepository;
    private final SteelGradeRepository steelGradeRepository;
    private final ProductionReportRepository productionReportRepository;

    @Transactional
    public void seedTestData() {
        log.info("=== НАЧАЛО ЗАПОЛНЕНИЯ ТЕСТОВЫМИ ДАННЫМИ ===");

        long existingCount = nonconformingRepository.count();
        if (existingCount > 0) {
            log.warn("В таблице уже {} записей. Очистите перед запуском.", existingCount);
            return;
        }

        ProductionSite utvMk = productionSiteRepository.findBySiteCode("УТВ_МК")
                .orElseThrow(() -> new RuntimeException("Участок УТВ_МК не найден"));
        ProductionSite ku1 = productionSiteRepository.findBySiteCode("КУ-1")
                .orElseThrow(() -> new RuntimeException("Участок КУ-1 не найден"));
        ProductionSite ku2 = productionSiteRepository.findBySiteCode("КУ-2")
                .orElseThrow(() -> new RuntimeException("Участок КУ-2 не найден"));
        ProductionSite gsv = productionSiteRepository.findBySiteCode("ГСВ")
                .orElseThrow(() -> new RuntimeException("Участок ГСВ не найден"));
        ProductionSite ttgu = productionSiteRepository.findBySiteCode("ТТГУ")
                .orElseThrow(() -> new RuntimeException("Участок ТТГУ не найден"));
        ProductionSite tu = productionSiteRepository.findBySiteCode("ТУ")
                .orElseThrow(() -> new RuntimeException("Участок ТУ не найден"));
        ProductionSite sk = productionSiteRepository.findBySiteCode("СК")
                .orElseThrow(() -> new RuntimeException("Участок СК не найден"));
        ProductionSite utvRml = productionSiteRepository.findBySiteCode("УТВ_РМЛ")
                .orElseThrow(() -> new RuntimeException("Участок УТВ_РМЛ не найден"));

        DefectType defectNamat = defectTypeRepository.findById(1L).orElse(null);
        DefectType defectKoltso = defectTypeRepository.findById(2L).orElse(null);
        DefectType defectNepryamolin = defectTypeRepository.findById(4L).orElse(null);
        DefectType defectKrz = defectTypeRepository.findById(6L).orElse(null);
        DefectType defectSboyUkladchik = defectTypeRepository.findById(7L).orElse(null);
        DefectType defectPokrytie = defectTypeRepository.findById(8L).orElse(null);
        DefectType defectTravlenie = defectTypeRepository.findById(9L).orElse(null);
        DefectType defectPeresort = defectTypeRepository.findById(10L).orElse(null);
        DefectType defectBrak = defectTypeRepository.findById(11L).orElse(null);

        DefectCause causeControl = defectCauseRepository.findById(2L).orElse(null);
        DefectCause causeControlSub = defectCauseRepository.findById(41L).orElse(null);
        DefectCause causeWear = defectCauseRepository.findById(4L).orElse(null);
        DefectCause causeHuman = defectCauseRepository.findById(8L).orElse(null);
        DefectCause causeHumanSub = defectCauseRepository.findById(82L).orElse(null);
        DefectCause causeTeh = defectCauseRepository.findById(6L).orElse(null);
        DefectCause causeTehSub = defectCauseRepository.findById(61L).orElse(null);
        DefectCause causeZagot = defectCauseRepository.findById(7L).orElse(null);
        DefectCause causeZagotSub = defectCauseRepository.findById(71L).orElse(null);

        DetectionSource otk = detectionSourceRepository.findById(1L).orElse(null);
        DetectionSource cehe = detectionSourceRepository.findById(2L).orElse(null);
        DetectionSource li = detectionSourceRepository.findById(3L).orElse(null);
        DetectionSource czl = detectionSourceRepository.findById(4L).orElse(null);

        ReworkType reworkVosstan = reworkTypeRepository.findById(1L).orElse(null);
        ReworkType reworkPereznach = reworkTypeRepository.findById(2L).orElse(null);

        List<NonconformingProduct> records = List.of(
            createRecord(utvMk, defectNamat, causeControl, causeControlSub,
                    "Акт Т46-2026 №1", LocalDate.of(2026, 7, 28), "К-1001", "Стан-14",
                    "2х0,30НТ", "95Г2Ф", BigDecimal.valueOf(0.050), null, null, null),
            createRecord(utvMk, defectNamat, causeControl, causeControlSub,
                    "Акт Т46-2026 №2", LocalDate.of(2026, 7, 28), "К-1002", "Стан-14",
                    "2х0,30НТ", "95Г2Ф", BigDecimal.valueOf(0.030), LocalDate.of(2026, 7, 29), reworkVosstan, BigDecimal.valueOf(0.030)),
            createRecord(utvMk, defectKoltso, causeControl, causeControlSub,
                    "Акт Т46-2026 №3", LocalDate.of(2026, 7, 28), "К-1003", "Стан-15",
                    "2х0,30НТ", "08ГФ", BigDecimal.valueOf(0.020), LocalDate.of(2026, 7, 29), reworkPereznach, BigDecimal.valueOf(0.020)),
            createRecord(utvMk, defectNepryamolin, causeWear, null,
                    "Акт Т46-2026 №4", LocalDate.of(2026, 7, 29), "К-1004", "Стан-15",
                    "3+2х0,30НТ", "08ГФ", BigDecimal.valueOf(0.018), LocalDate.of(2026, 7, 30), reworkVosstan, BigDecimal.valueOf(0.018)),
            createRecord(utvMk, defectNamat, causeTeh, causeTehSub,
                    "Акт Т46-2026 №5", LocalDate.of(2026, 7, 29), "К-1005", "Стан-16",
                    "3+2х0,30НТ", "95Г2Ф", BigDecimal.valueOf(0.025), LocalDate.of(2026, 7, 30), reworkVosstan, BigDecimal.valueOf(0.025)),
            createRecord(utvMk, defectPokrytie, causeTeh, causeTehSub,
                    "Акт Т46-2026 №6", LocalDate.of(2026, 7, 30), "К-1006", "Стан-14",
                    "2х0,30НТ", "95Г2Ф", BigDecimal.valueOf(0.015), null, null, null),
            createRecord(utvMk, defectNamat, causeWear, null,
                    "Акт Т46-2026 №7", LocalDate.of(2026, 7, 30), "К-1007", "Стан-14",
                    "2х0,30НТ", "95Г2Ф", BigDecimal.valueOf(0.022), LocalDate.of(2026, 7, 31), reworkVosstan, BigDecimal.valueOf(0.022)),
            createRecord(utvMk, defectNepryamolin, causeHuman, causeHumanSub,
                    "Акт Т46-2026 №8", LocalDate.of(2026, 7, 31), "К-1008", "Стан-15",
                    "3+2х0,30НТ", "08ГФ", BigDecimal.valueOf(0.020), LocalDate.of(2026, 8, 1), reworkVosstan, BigDecimal.valueOf(0.020)),

            createRecord(ku1, defectKrz, causeControl, causeControlSub,
                    "Акт Т46-2026 №9", LocalDate.of(2026, 7, 28), "К-2001", "Канатная-1",
                    "2х0,30НТ", "95Г2Ф", BigDecimal.valueOf(0.100), LocalDate.of(2026, 7, 29), reworkVosstan, BigDecimal.valueOf(0.080)),
            createRecord(ku1, defectKrz, causeZagot, null,
                    "Акт Т46-2026 №10", LocalDate.of(2026, 7, 28), "К-2002", "Канатная-2",
                    "3+2х0,30НТ", "08ГФ", BigDecimal.valueOf(0.020), LocalDate.of(2026, 7, 29), reworkPereznach, BigDecimal.valueOf(0.020)),
            createRecord(ku1, defectBrak, causeHuman, causeHumanSub,
                    "Акт Т46-2026 №11", LocalDate.of(2026, 7, 29), "К-2003", "Канатная-1",
                    "2х0,30НТ", "95Г2Ф", BigDecimal.valueOf(0.018), null, null, null),
            createRecord(ku1, defectSboyUkladchik, causeWear, null,
                    "Акт Т46-2026 №12", LocalDate.of(2026, 7, 30), "К-2004", "Канатная-2",
                    "3+2х0,30НТ", "08ГФ", BigDecimal.valueOf(0.018), LocalDate.of(2026, 7, 31), reworkVosstan, BigDecimal.valueOf(0.018)),
            createRecord(ku1, defectPeresort, causeZagot, causeZagotSub,
                    "Акт Т46-2026 №13", LocalDate.of(2026, 7, 31), "К-2005", "Канатная-1",
                    "2х0,30НТ", "95Г2Ф", BigDecimal.valueOf(0.085), null, null, null),

            createRecord(ku2, defectKrz, causeControl, causeControlSub,
                    "Акт Т46-2026 №24", LocalDate.of(2026, 7, 28), "К-9001", "Канатная-3",
                    "2х0,35НТ", "95Г2Ф", BigDecimal.valueOf(0.120), LocalDate.of(2026, 7, 29), reworkVosstan, BigDecimal.valueOf(0.100)),
            createRecord(ku2, defectPeresort, causeZagot, causeZagotSub,
                    "Акт Т46-2026 №25", LocalDate.of(2026, 7, 31), "К-9002", "Канатная-4",
                    "3+2х0,35НТ", "08ГФ", BigDecimal.valueOf(0.065), null, null, null),

            createRecord(gsv, defectNamat, causeWear, null,
                    "Акт Т46-2026 №14", LocalDate.of(2026, 7, 28), "К-3001", "Стан-Г-1",
                    null, "65Г", BigDecimal.valueOf(0.040), LocalDate.of(2026, 7, 29), reworkVosstan, BigDecimal.valueOf(0.040)),
            createRecord(gsv, defectKoltso, causeControl, causeControlSub,
                    "Акт Т46-2026 №15", LocalDate.of(2026, 7, 29), "К-3002", "Стан-Г-2",
                    null, "65Г", BigDecimal.valueOf(0.090), LocalDate.of(2026, 7, 30), reworkVosstan, BigDecimal.valueOf(0.090)),

            createRecord(ttgu, defectTravlenie, causeTeh, causeTehSub,
                    "Акт Т46-2026 №16", LocalDate.of(2026, 7, 28), "К-4001", "Травма-1",
                    null, "80Х", BigDecimal.valueOf(0.285), LocalDate.of(2026, 7, 29), reworkVosstan, BigDecimal.valueOf(0.285)),
            createRecord(ttgu, defectPokrytie, causeTeh, causeTehSub,
                    "Акт Т46-2026 №17", LocalDate.of(2026, 7, 30), "К-4002", "Травма-2",
                    null, "85Х", BigDecimal.valueOf(0.050), null, null, null),

            createRecord(tu, defectTravlenie, causeTeh, causeTehSub,
                    "Акт Т46-2026 №18", LocalDate.of(2026, 7, 28), "Б-1001", "Травилка-1",
                    null, "65Г", BigDecimal.valueOf(0.065), LocalDate.of(2026, 7, 29), reworkVosstan, BigDecimal.valueOf(0.065)),
            createRecord(tu, defectPokrytie, causeHuman, causeHumanSub,
                    "Акт Т46-2026 №19", LocalDate.of(2026, 7, 29), "Б-1002", "Травилка-2",
                    null, "70Г", BigDecimal.valueOf(0.035), LocalDate.of(2026, 7, 30), reworkVosstan, BigDecimal.valueOf(0.035)),

            createRecord(sk, defectNamat, causeZagot, null,
                    "Акт Т46-2026 №20", LocalDate.of(2026, 7, 28), "Бунт-2001", null,
                    null, "08ГФ", BigDecimal.valueOf(0.012), LocalDate.of(2026, 7, 29), reworkVosstan, BigDecimal.valueOf(0.012)),
            createRecord(sk, defectPeresort, causeZagot, null,
                    "Акт Т46-2026 №21", LocalDate.of(2026, 7, 30), "Бунт-2002", null,
                    null, "95Г2Ф", BigDecimal.valueOf(0.008), null, null, null),

            createRecord(utvRml, defectNamat, causeWear, null,
                    "Акт Т46-2026 №22", LocalDate.of(2026, 7, 28), "К-6001", "Стан-Р-1",
                    null, "65Г", BigDecimal.valueOf(0.015), LocalDate.of(2026, 7, 29), reworkVosstan, BigDecimal.valueOf(0.015)),
            createRecord(utvRml, defectKoltso, causeWear, null,
                    "Акт Т46-2026 №23", LocalDate.of(2026, 7, 30), "К-6002", "Стан-Р-2",
                    null, "70Г", BigDecimal.valueOf(0.028), LocalDate.of(2026, 7, 31), reworkVosstan, BigDecimal.valueOf(0.028))
        );

        nonconformingRepository.saveAll(records);
        log.info("=== ДОБАВЛЕНО {} ЗАПИСЕЙ ===", records.size());

        List<NonconformingProduct> saved = nonconformingRepository.findAll();
        for (NonconformingProduct r : saved) {
            log.info("  [{}] {} | масса={} | оборудование={} | дефект={}",
                    r.getDetectionDate(),
                    r.getProductionSite().getSiteName(),
                    r.getWeightTonnes(),
                    r.getEquipmentKey(),
                    r.getDefectType() != null ? r.getDefectType().getDefectName() : "null");
        }
    }

    private NonconformingProduct createRecord(ProductionSite site, DefectType defectType,
                                               DefectCause cause, DefectCause subcause,
                                               String note, LocalDate date, String reelNumber,
                                               String equipmentKey, String steelCordConstruction,
                                               String steelGrade, BigDecimal weight,
                                               LocalDate reworkDate, ReworkType reworkType,
                                               BigDecimal reworkWeight) {
        NonconformingProduct r = new NonconformingProduct();
        r.setDetectionDate(date);
        r.setProductionSite(site);
        r.setDetectionSource(detectionSourceRepository.findById(1L).orElse(null));
        r.setWeightTonnes(weight);
        r.setDefectType(defectType);
        r.setDefectCause(cause);
        r.setDefectSubcause(subcause);
        r.setNote(note);
        r.setReelNumber(reelNumber);
        r.setEquipmentKey(equipmentKey);
        r.setSteelCordConstruction(steelCordConstruction);
        r.setSteelGrade(steelGrade);
        r.setIrreparableWeightTonnes((reworkType == null && defectType != null && !defectType.getReworkable())
                ? weight : null);
        r.setReworkDate(reworkDate);
        r.setReworkType(reworkType);
        r.setReworkWeightTonnes(reworkWeight);
        return r;
    }
}
