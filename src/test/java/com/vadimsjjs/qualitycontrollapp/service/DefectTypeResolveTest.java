package com.vadimsjjs.qualitycontrollapp.service;

import com.vadimsjjs.qualitycontrollapp.controller.DefectTypeController;
import com.vadimsjjs.qualitycontrollapp.dto.DefectTypeRequest;
import com.vadimsjjs.qualitycontrollapp.entity.DefectType;
import com.vadimsjjs.qualitycontrollapp.repository.DefectTypeRepository;
import com.vadimsjjs.qualitycontrollapp.repository.NonconformingProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Проверка работы справочника видов дефектов:
 * добавляем новое значение, защищаем справочник от дублей и от удаления используемых значений.
 */
@ExtendWith(MockitoExtension.class)
class DefectTypeResolveTest {

    @Mock
    private DefectTypeRepository defectTypeRepository;

    @Mock
    private NonconformingProductRepository nonconformingProductRepository;

    private DefectTypeController controller() {
        return new DefectTypeController(defectTypeRepository, nonconformingProductRepository);
    }

    @Test
    void generatesNextCodeWhenCodeNotGiven() {
        DefectType existing = new DefectType();
        existing.setId(1L);
        existing.setDefectCode("007");
        existing.setDefectName("Намот");

        when(defectTypeRepository.findByDefectNameIgnoreCase("Рывок проволоки"))
                .thenReturn(Optional.empty());
        when(defectTypeRepository.findAll()).thenReturn(List.of(existing));
        when(defectTypeRepository.save(any(DefectType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DefectType created = controller().create(DefectTypeRequest.builder()
                .defectName("  Рывок проволоки  ")
                .reworkable(true)
                .build()).getBody();

        assertEquals("Рывок проволоки", created.getDefectName(), "название должно обрезаться от пробелов");
        assertEquals("008", created.getDefectCode(), "код должен стать следующим свободным");
        assertTrue(created.getReworkable());
    }

    @Test
    void rejectsDuplicateNameIgnoringCase() {
        DefectType existing = new DefectType();
        existing.setId(5L);
        existing.setDefectName("Намот");

        when(defectTypeRepository.findByDefectNameIgnoreCase("НАМОТ")).thenReturn(Optional.of(existing));

        try {
            controller().create(DefectTypeRequest.builder().defectName("НАМОТ").build());
            throw new AssertionError("ожидалась ошибка о дубликате");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("уже есть"));
        }

        verify(defectTypeRepository, never()).save(any());
    }

    @Test
    void refusesToDeleteDefectUsedInRecords() {
        DefectType used = new DefectType();
        used.setId(3L);
        used.setDefectName("Некачественная свивка");

        when(defectTypeRepository.findById(3L)).thenReturn(Optional.of(used));
        when(nonconformingProductRepository.existsByDefectType_Id(3L)).thenReturn(true);

        try {
            controller().delete(3L);
            throw new AssertionError("ожидался запрет удаления");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("уже используется"));
        }

        verify(defectTypeRepository, never()).delete(any());
    }

    @Test
    void deletesUnusedDefect() {
        DefectType unused = new DefectType();
        unused.setId(9L);
        unused.setDefectName("Новый дефект");

        when(defectTypeRepository.findById(9L)).thenReturn(Optional.of(unused));
        when(nonconformingProductRepository.existsByDefectType_Id(9L)).thenReturn(false);

        assertEquals(204, controller().delete(9L).getStatusCode().value());

        ArgumentCaptor<DefectType> captor = ArgumentCaptor.forClass(DefectType.class);
        verify(defectTypeRepository).delete(captor.capture());
        assertEquals(9L, captor.getValue().getId());
    }

    @Test
    void searchReturnsEmptyForBlankQuery() {
        assertTrue(controller().search("   ").isEmpty());
    }
}
