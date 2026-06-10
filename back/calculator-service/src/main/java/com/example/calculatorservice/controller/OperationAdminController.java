package com.example.calculatorservice.controller;

import com.example.calculatorservice.dto.OperationCreateRequest;
import com.example.calculatorservice.dto.OperationDto;
import com.example.calculatorservice.dto.OperationUpdateRequest;
import com.example.calculatorservice.entity.Operation;
import com.example.calculatorservice.mapper.OperationMapper;
import com.example.calculatorservice.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/operations")
@RequiredArgsConstructor
public class OperationAdminController {

    private final OperationService operationService;
    private final OperationMapper operationMapper;

     @PreAuthorize("hasRole('ADMIN')")
     @PostMapping
     public ResponseEntity<OperationDto> createOperation(@RequestBody OperationCreateRequest request) {
         Operation operation = operationMapper.toEntity(request);
         Operation saved = operationService.save(operation);
         return ResponseEntity.ok(operationMapper.toDto(saved));
     }

     @PreAuthorize("hasRole('ADMIN')")
     @PutMapping("/{id}")
     public ResponseEntity<OperationDto> updateOperation(@PathVariable Long id, @RequestBody OperationUpdateRequest request) {
         Operation operation = operationService.getOperationById(id);
         operationMapper.updateEntityFromRequest(request, operation);
         Operation updated = operationService.save(operation);
         return ResponseEntity.ok(operationMapper.toDto(updated));
     }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperation(@PathVariable Long id) {
        operationService.deleteOperation(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<OperationDto>> getAllOperations() {
        return ResponseEntity.ok(
                operationService.getAllOperations().stream()
                        .map(operationMapper::toDto)
                        .toList()
        );
    }
}
