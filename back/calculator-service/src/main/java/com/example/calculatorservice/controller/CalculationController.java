package com.example.calculatorservice.controller;

import com.example.calculatorservice.dto.MaterialDto;
import com.example.calculatorservice.dto.OperationDto;
import com.example.calculatorservice.dto.request.AreaCalculationRequest;
import com.example.calculatorservice.dto.request.CalculationRequestDto;
import com.example.calculatorservice.dto.request.ItemCostRequest;
import com.example.calculatorservice.dto.response.CalculationResponseDto;
import com.example.calculatorservice.dto.GroupedOperationsResponse;
import com.example.calculatorservice.entity.Eyelet;
import com.example.calculatorservice.entity.Material;
import com.example.calculatorservice.entity.MaterialType;
import com.example.calculatorservice.mapper.OperationMapper;
import com.example.calculatorservice.service.CalculationService;
import com.example.calculatorservice.service.EyeletService;
import com.example.calculatorservice.service.MaterialService;
import com.example.calculatorservice.service.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calculations")
@RequiredArgsConstructor
@Slf4j
public class CalculationController {

    private final CalculationService calculationService;
    private final MaterialService materialService;
    private final OperationService operationService;
    private final EyeletService eyeletService;
    private final OperationMapper operationMapper;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/materials")
    public ResponseEntity<List<MaterialDto>> getAllMaterials() {
        return ResponseEntity.ok(
                materialService.getAllMaterials().stream()
                        .map(operationMapper::toMaterialDto)
                        .toList()
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/materials/{type}")
    public ResponseEntity<List<MaterialDto>> getMaterialsByType(@PathVariable String type) {
        MaterialType matType;
        try {
            matType = MaterialType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
                materialService.getMaterialsByType(matType).stream()
                        .map(operationMapper::toMaterialDto)
                        .toList()
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/eyelets")
    public ResponseEntity<List<Eyelet>> getAllEyelets() {
        return ResponseEntity.ok(eyeletService.getAllEyelets());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/operations")
    public ResponseEntity<List<OperationDto>> getAllOperations() {
        return ResponseEntity.ok(
                operationService.getAllOperations().stream()
                        .map(operationMapper::toDto)
                        .toList()
        );
    }

     @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
     @PostMapping
     public ResponseEntity<CalculationResponseDto> createCalculation(
             @Valid @RequestBody CalculationRequestDto request) {
         log.info("Received calculation request: materialId={}, materialType={}, widthM={}, heightM={}, operationIds={}",
                 request.getMaterialId(), request.getMaterialType(), request.getWidthM(), request.getHeightM(), request.getOperationIds());
         CalculationResponseDto response = calculationService.createAndCalculate(request);
         return ResponseEntity.ok(response);
     }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/{id}")
    public ResponseEntity<CalculationResponseDto> getCalculation(@PathVariable Long id) {
        return ResponseEntity.ok(calculationService.getById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @PostMapping("/calculate-area")
    public ResponseEntity<BigDecimal> calculateArea(
            @Valid @RequestBody AreaCalculationRequest request) {
        BigDecimal area = calculationService.calculateAreaWithPodvorot(
                request.getWidthM(),
                request.getHeightM(),
                request.getPodvorotMmHorizontal(),
                request.getPodvorotMmVertical(),
                request.getPodvorotCountPerSide()
        );
        return ResponseEntity.ok(area);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @PostMapping("/estimate-item")
    public ResponseEntity<BigDecimal> estimateItemCost(@Valid @RequestBody ItemCostRequest request) {
        BigDecimal cost = calculationService.calculateItemCost(
                request.getMaterialId(),
                request.getWidthM(),
                request.getHeightM(),
                request.getPodvorotMmHorizontal(),
                request.getPodvorotMmVertical(),
                request.getPodvorotCountPerSide(),
                request.getOperationIds()
        );
        return ResponseEntity.ok(cost);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @PostMapping("/preview")
    public ResponseEntity<CalculationResponseDto> preview(@Valid @RequestBody CalculationRequestDto request) {
        CalculationResponseDto dto = calculationService.calculateWithoutSaving(request);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/materials/{materialId}/grouped-operations")
    public ResponseEntity<GroupedOperationsResponse> getGroupedOperations(@PathVariable Long materialId) {
        return ResponseEntity.ok(operationService.getGroupedOperationsByMaterialId(materialId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/operations/grouped")
    public ResponseEntity<GroupedOperationsResponse> getAllGroupedOperations(@RequestParam(required = false) Long materialId) {
        return ResponseEntity.ok(operationService.getGroupedOperationsByMaterialId(materialId));
    }
}
