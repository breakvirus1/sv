package com.example.calculatorservice.controller;

import com.example.calculatorservice.dto.MaterialWithOperationsCreateRequest;
import com.example.calculatorservice.dto.MaterialWithOperationsResponse;
import com.example.calculatorservice.service.MaterialOperationGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/materials-with-operations")
@RequiredArgsConstructor
@Slf4j
public class MaterialWithOperationsAdminController {

    private final MaterialOperationGroupService materialOperationGroupService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<MaterialWithOperationsResponse> createMaterialWithOperations(@RequestBody MaterialWithOperationsCreateRequest request) {
        MaterialWithOperationsResponse response = materialOperationGroupService.createMaterialWithOperations(request);
        return ResponseEntity.ok(response);
    }
}
