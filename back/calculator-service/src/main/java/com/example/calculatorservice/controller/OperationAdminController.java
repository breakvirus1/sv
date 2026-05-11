package com.example.calculatorservice.controller;

 import com.example.calculatorservice.dto.OperationCreateRequest;
 import com.example.calculatorservice.dto.OperationDto;
 import com.example.calculatorservice.dto.OperationUpdateRequest;
 import com.example.calculatorservice.entity.Operation;
 import com.example.calculatorservice.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/operations")
@RequiredArgsConstructor
public class OperationAdminController {

    private final OperationService operationService;

     @PreAuthorize("hasRole('ADMIN')")
     @PostMapping
     public ResponseEntity<OperationDto> createOperation(@RequestBody OperationCreateRequest request) {
         Operation operation = new Operation();
         operation.setName(request.getName());
         operation.setPrice(request.getPrice());
         operation.setUnit(request.getUnit());
         operation.setApplicableTo(request.getApplicableTo());
         operation.setDefault(request.isDefault());
         operation.setHemWidthMm(request.getHemWidthMm());
         operation.setHemCount(request.getHemCount());

         Operation saved = operationService.save(operation);
         return ResponseEntity.ok(toOperationDto(saved));
     }

     @PreAuthorize("hasRole('ADMIN')")
     @PutMapping("/{id}")
     public ResponseEntity<OperationDto> updateOperation(@PathVariable Long id, @RequestBody OperationUpdateRequest request) {
         Operation operation = operationService.getOperationById(id);
         if (request.getName() != null) operation.setName(request.getName());
         if (request.getPrice() != null) operation.setPrice(request.getPrice());
         if (request.getUnit() != null) operation.setUnit(request.getUnit());
         if (request.getApplicableTo() != null) operation.setApplicableTo(request.getApplicableTo());
         operation.setDefault(request.isDefault());
         if (request.getHemWidthMm() != null) operation.setHemWidthMm(request.getHemWidthMm());
         if (request.getHemCount() != null) operation.setHemCount(request.getHemCount());

         Operation updated = operationService.save(operation);
         return ResponseEntity.ok(toOperationDto(updated));
     }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperation(@PathVariable Long id) {
        operationService.deleteOperation(id);
        return ResponseEntity.noContent().build();
    }

    private OperationDto toOperationDto(Operation op) {
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
