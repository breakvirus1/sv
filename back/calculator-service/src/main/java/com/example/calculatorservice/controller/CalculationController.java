package com.example.calculatorservice.controller;

import com.example.calculatorservice.dto.MaterialDto;
import com.example.calculatorservice.dto.OperationDto;
import com.example.calculatorservice.dto.request.AreaCalculationRequest;
import com.example.calculatorservice.dto.request.CalculationRequestDto;
import com.example.calculatorservice.dto.request.ItemCostRequest;
import com.example.calculatorservice.dto.response.CalculationResponseDto;
import com.example.calculatorservice.entity.Eyelet;
import com.example.calculatorservice.entity.Material;
import com.example.calculatorservice.entity.MaterialType;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/calculations")
@RequiredArgsConstructor
@Slf4j
public class CalculationController {

    private final CalculationService calculationService;
    private final MaterialService materialService;
    private final OperationService operationService;
    private final EyeletService eyeletService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/materials")
    public ResponseEntity<List<MaterialDto>> getAllMaterials() {
        return ResponseEntity.ok(
                materialService.getAllMaterials().stream()
                        .map(this::toMaterialDto)
                        .collect(Collectors.toList())
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
                        .map(this::toMaterialDto)
                        .collect(Collectors.toList())
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
                        .map(this::toOperationDto)
                        .collect(Collectors.toList())
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/operations/{applicableTo}")
    public ResponseEntity<List<OperationDto>> getOperationsByApplicableType(@PathVariable String applicableTo) {
        com.example.calculatorservice.entity.ApplicableType appType;
        try {
            appType = com.example.calculatorservice.entity.ApplicableType.valueOf(applicableTo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
                operationService.getOperationsByApplicableType(appType).stream()
                        .map(this::toOperationDto)
                        .collect(Collectors.toList())
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

    /**
     * Real-time calculation of material area with podvorot (no persistence).
     * Used by frontend for live price updates when entering dimensions.
     */
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

    /**
     * Расчет стоимости одной позиции материала с учетом подворотов и операций.
     * Используется для real-time обновления суммы в интерфейсе создания заказа.
     */
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

    /**
     * Предварительный расчет полной стоимости позиции (материал + операции)
     * без сохранения в базу. Возвращает детализацию по операциям.
     * Используется order-service для расчета итогов заказа.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @PostMapping("/preview")
    public ResponseEntity<CalculationResponseDto> preview(@Valid @RequestBody CalculationRequestDto request) {
        CalculationResponseDto dto = calculationService.calculateWithoutSaving(request);
        return ResponseEntity.ok(dto);
    }

    // Helper mapping methods
    private MaterialDto toMaterialDto(Material material) {
        MaterialDto dto = new MaterialDto();
        dto.setId(material.getId());
        dto.setName(material.getName());
        dto.setPricePerSquareMeter(material.getPricePerSquareMeter());
        dto.setWasteCoefficient(material.getWasteCoefficient());
        // Determine type based on name
        String name = material.getName();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("баннер")) {
                dto.setType("BANNER");
            } else if (lower.contains("плёнка") || lower.contains("пленка")) {
                dto.setType("PLENKA");
            } else {
                dto.setType("UNKNOWN");
            }
        } else {
            dto.setType("UNKNOWN");
        }
        return dto;
    }

    private OperationDto toOperationDto(com.example.calculatorservice.entity.Operation op) {
        OperationDto dto = new OperationDto();
        dto.setId(op.getId());
        dto.setName(op.getName());
        dto.setPrice(op.getPrice());
        dto.setUnit(op.getUnit().getDisplayName());
        dto.setApplicableTo(op.getApplicableTo().name());
        dto.setDefault(op.isDefault());
        dto.setHemWidthMm(op.getHemWidthMm());
        dto.setHemCount(op.getHemCount());
        return dto;
    }
}
