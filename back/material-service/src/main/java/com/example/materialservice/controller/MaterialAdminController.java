package com.example.materialservice.controller;

import com.example.common.entity.Material;
import com.example.materialservice.service.MaterialAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/materials")
@RequiredArgsConstructor
@Tag(name = "Material Admin", description = "Админ API для управления материалами")
public class MaterialAdminController {

    private final MaterialAdminService materialAdminService;

    @Operation(summary = "Сгенерировать 20 тестовых материалов")
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Material>> generateTestMaterials() {
        List<Material> materials = materialAdminService.generateTestMaterials(20);
        return ResponseEntity.ok(materials);
    }
}
