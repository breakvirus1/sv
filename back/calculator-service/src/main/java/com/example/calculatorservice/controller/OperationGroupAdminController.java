package com.example.calculatorservice.controller;

import com.example.calculatorservice.dto.OperationGroupCreateRequest;
import com.example.calculatorservice.dto.OperationGroupDto;
import com.example.calculatorservice.dto.OperationGroupUpdateRequest;
import com.example.calculatorservice.entity.OperationGroup;
import com.example.calculatorservice.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/operation-groups")
@RequiredArgsConstructor
public class OperationGroupAdminController {

    private final OperationService operationService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<List<OperationGroupDto>> getAllOperationGroups() {
        return ResponseEntity.ok(
                operationService.getAllOperationGroups().stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<OperationGroupDto> createOperationGroup(@RequestBody OperationGroupCreateRequest request) {
        OperationGroup group = new OperationGroup();
        group.setName(request.getName());
        OperationGroup saved = operationService.save(group);
        return ResponseEntity.ok(toDto(saved));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<OperationGroupDto> updateOperationGroup(@PathVariable Long id, @RequestBody OperationGroupUpdateRequest request) {
        OperationGroup group = operationService.getOperationGroupById(id);
        group.setName(request.getName());
        OperationGroup updated = operationService.save(group);
        return ResponseEntity.ok(toDto(updated));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperationGroup(@PathVariable Long id) {
        operationService.deleteOperationGroup(id);
        return ResponseEntity.noContent().build();
    }

    private OperationGroupDto toDto(OperationGroup group) {
        OperationGroupDto dto = new OperationGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        return dto;
    }
}
