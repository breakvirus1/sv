package com.example.calculatorservice.controller;

import com.example.calculatorservice.dto.OperationDto;
import com.example.calculatorservice.mapper.OperationMapper;
import com.example.calculatorservice.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/operations/materials")
@RequiredArgsConstructor
public class MaterialOperationAdminController {

    private final OperationService operationService;
    private final OperationMapper operationMapper;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/{materialId}/operations")
    public ResponseEntity<List<OperationDto>> getMaterialOperations(@PathVariable Long materialId) {
        return ResponseEntity.ok(
                operationService.getOperationsByMaterialId(materialId).stream()
                        .map(operationMapper::toDto)
                        .collect(Collectors.toList())
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{materialId}/operations")
    public ResponseEntity<Void> setMaterialOperations(@PathVariable Long materialId, @RequestBody List<Long> operationIds) {
        operationService.setMaterialOperations(materialId, operationIds);
        return ResponseEntity.ok().build();
    }
}
