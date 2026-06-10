package com.example.orderservice.controller;

import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Workshop;
import com.example.orderservice.service.WorkshopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workshops")
@RequiredArgsConstructor
@Tag(name = "Workshop Service", description = "API для управления цехами")
public class WorkshopController {

    private final WorkshopService workshopService;

    @Operation(summary = "Получить список цехов")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT')")
    public ResponseEntity<Page<WorkshopResponse>> getAllWorkshops(Pageable pageable) {
        Page<WorkshopResponse> page = workshopService.getAllWorkshops(Specification.where(null), pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Получить цех по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT')")
    public ResponseEntity<WorkshopResponse> getWorkshop(@PathVariable Long id) {
        return ResponseEntity.ok(workshopService.getWorkshopById(id));
    }

    @Operation(summary = "Создать новый цех")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<WorkshopResponse> createWorkshop(@RequestBody WorkshopCreateRequest request) {
        return new ResponseEntity<>(workshopService.createWorkshop(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить цех")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<WorkshopResponse> updateWorkshop(
            @PathVariable Long id,
            @RequestBody WorkshopUpdateRequest request) {
        return ResponseEntity.ok(workshopService.updateWorkshop(id, request));
    }

    @Operation(summary = "Удалить цех (мягкое удаление)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteWorkshop(@PathVariable Long id) {
        workshopService.deleteWorkshop(id);
        return ResponseEntity.noContent().build();
    }
}
