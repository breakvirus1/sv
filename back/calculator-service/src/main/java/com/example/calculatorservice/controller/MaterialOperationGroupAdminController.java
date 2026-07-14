package com.example.calculatorservice.controller;

import com.example.calculatorservice.dto.GroupedOperationsResponse;
import com.example.calculatorservice.dto.MaterialOperationGroupCreateRequest;
import com.example.calculatorservice.dto.MaterialOperationGroupDto;
import com.example.calculatorservice.dto.MaterialOperationGroupUpdateRequest;
import com.example.calculatorservice.service.MaterialOperationGroupService;
import com.example.calculatorservice.service.OperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/materials/{materialId}/operation-groups")
@RequiredArgsConstructor
@Slf4j
public class MaterialOperationGroupAdminController {

    private final MaterialOperationGroupService service;
    private final OperationService operationService;

    /**
     * Возвращает сгруппированные операции для материала.
     *
     * <p>Используется в админ-панели для отображения операций, назначенных материалу,
     * сгруппированных по группировкам. Возвращает список с одним элементом —
     * {@link GroupedOperationsResponse}.</p>
     *
     * @param materialId идентификатор материала
     * @return список, содержащий один элемент — сгруппированный ответ операций
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<List<GroupedOperationsResponse>> getGroupedOperations(@PathVariable Long materialId) {
        return ResponseEntity.ok(List.of(operationService.getGroupedOperationsByMaterialId(materialId)));
    }

    /**
     * Возвращает все связи материала с операциями.
     *
     * @param materialId идентификатор материала
     * @return список DTO связей материала с операциями
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<MaterialOperationGroupDto>> getAllMappings(@PathVariable Long materialId) {
        try {
            List<MaterialOperationGroupDto> result = service.getMaterialOperationGroups(materialId).stream()
                    .map(service::toDto)
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to load operation group mappings for material {}: {}", materialId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Добавляет связь материала с операцией через группировку.
     *
     * @param materialId идентификатор материала
     * @param request запрос с идентификаторами группировки и операции
     * @return DTO созданной связи
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<MaterialOperationGroupDto> addMapping(
            @PathVariable Long materialId,
            @RequestBody MaterialOperationGroupCreateRequest request) {
        request.setMaterialId(materialId);
        return ResponseEntity.ok(service.toDto(service.createMaterialOperationGroup(request)));
    }

    /**
     * Полностью заменяет набор операций для материала.
     *
     * <p>Удаляет все существующие связи материала с операциями и создаёт новые
     * на основе переданного списка идентификаторов операций. Для каждой операции
     * автоматически определяется подходящая группировка по названию.</p>
     *
     * @param materialId идентификатор материала
     * @param request запрос с новым списком идентификаторов операций
     * @return список DTO созданных связей
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/replace")
    public ResponseEntity<List<MaterialOperationGroupDto>> replaceMappings(
            @PathVariable Long materialId,
            @RequestBody MaterialOperationGroupUpdateRequest request) {
        request.setMaterialId(materialId);
        return ResponseEntity.ok(
                service.updateMaterialOperations(materialId, request).stream()
                        .map(service::toDto)
                        .toList()
        );
    }

    /**
     * Удаляет связь материала с операцией.
     *
     * @param materialId идентификатор материала
     * @param id идентификатор связи material_operation_groups
     * @return ответ 204 No Content
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long materialId, @PathVariable Long id) {
        service.deleteMaterialOperationGroup(id);
        return ResponseEntity.noContent().build();
    }
}
