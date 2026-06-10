package com.example.materialservice.controller;

import com.example.materialservice.dto.MaterialOperationCreateRequest;
import com.example.materialservice.dto.MaterialOperationResponse;
import com.example.materialservice.dto.MaterialOperationUpdateRequest;
import com.example.materialservice.dto.OperationCalculateRequest;
import com.example.materialservice.service.MaterialOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/materials/{materialId}/operations")
@RequiredArgsConstructor
@Tag(name = "Material Operations", description = "API для управления операциями материалов")
public class MaterialOperationController {

    private final MaterialOperationService operationService;

    @GetMapping
    @Operation(summary = "Получить все операции материала")
    public ResponseEntity<List<MaterialOperationResponse>> getOperations(@PathVariable Long materialId) {
        return ResponseEntity.ok(operationService.getOperationsByMaterialId(materialId));
    }

    @PostMapping
    @Operation(summary = "Добавить операцию к материалу")
    public ResponseEntity<MaterialOperationResponse> createOperation(
            @PathVariable Long materialId,
            @Valid @RequestBody MaterialOperationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operationService.createOperation(materialId, request));
    }

    @PutMapping("/{operationId}")
    @Operation(summary = "Обновить операцию материала")
    public ResponseEntity<MaterialOperationResponse> updateOperation(
            @PathVariable Long materialId,
            @PathVariable Long operationId,
            @Valid @RequestBody MaterialOperationUpdateRequest request) {
        return ResponseEntity.ok(operationService.updateOperation(materialId, operationId, request));
    }

    @DeleteMapping("/{operationId}")
    @Operation(summary = "Удалить операцию материала")
    public ResponseEntity<Void> deleteOperation(
            @PathVariable Long materialId,
            @PathVariable Long operationId) {
        operationService.deleteOperation(materialId, operationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{operationId}/calculate-quantity")
    @Operation(summary = "Рассчитать количество операции (например, люверсов)")
    public ResponseEntity<BigDecimal> calculateQuantity(
            @PathVariable("materialId") Long materialId,
            @PathVariable("operationId") Long operationId,
            @RequestBody OperationCalculateRequest request) {
        if (request.getWidth() == null || request.getHeight() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Width and height are required");
        }
        BigDecimal quantity = operationService.calculateQuantity(operationId, request);
        return ResponseEntity.ok(quantity);
    }
}
