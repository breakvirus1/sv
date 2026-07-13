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

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<List<GroupedOperationsResponse>> getGroupedOperations(@PathVariable Long materialId) {
        return ResponseEntity.ok(List.of(operationService.getGroupedOperationsByMaterialId(materialId)));
    }

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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<MaterialOperationGroupDto> addMapping(
            @PathVariable Long materialId,
            @RequestBody MaterialOperationGroupCreateRequest request) {
        request.setMaterialId(materialId);
        return ResponseEntity.ok(service.toDto(service.createMaterialOperationGroup(request)));
    }

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

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long materialId, @PathVariable Long id) {
        service.deleteMaterialOperationGroup(id);
        return ResponseEntity.noContent().build();
    }
}
