package com.example.calculatorservice.controller;

 import com.example.calculatorservice.dto.MaterialDto;
 import com.example.calculatorservice.dto.OperationDto;
 import com.example.calculatorservice.dto.request.CalculationRequestDto;
 import com.example.calculatorservice.dto.response.CalculationResponseDto;
 import com.example.calculatorservice.entity.Banner;
 import com.example.calculatorservice.entity.Eyelet;
 import com.example.calculatorservice.entity.Material;
 import com.example.calculatorservice.entity.MaterialType;
 import com.example.calculatorservice.entity.Plenka;
 import com.example.calculatorservice.service.CalculationService;
 import com.example.calculatorservice.service.EyeletService;
 import com.example.calculatorservice.service.MaterialService;
 import com.example.calculatorservice.service.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/calculations")
@RequiredArgsConstructor
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
        CalculationResponseDto response = calculationService.createAndCalculate(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    @GetMapping("/{id}")
    public ResponseEntity<CalculationResponseDto> getCalculation(@PathVariable Long id) {
        return ResponseEntity.ok(calculationService.getById(id));
    }

    // Helper mapping methods
    private MaterialDto toMaterialDto(Material material) {
        MaterialDto dto = new MaterialDto();
        dto.setId(material.getId());
        dto.setName(material.getName());
        dto.setPricePerSquareMeter(material.getPricePerSquareMeter());
        dto.setWasteCoefficient(material.getWasteCoefficient());
        if (material instanceof Banner) {
            dto.setType("BANNER");
        } else if (material instanceof Plenka) {
            dto.setType("PLENKA");
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
        return dto;
    }
}
