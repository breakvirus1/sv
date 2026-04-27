package com.example.materialservice.controller;

import com.example.common.entity.Material;
import com.example.materialservice.service.MaterialService;
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
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
@Tag(name = "Material Service", description = "API для управления материалами")
public class MaterialController {

    private final MaterialService materialService;

    @Operation(summary = "Получить список материалов")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    public ResponseEntity<Page<Material>> getAllMaterials(
            @RequestParam(required = false) String q,
            Pageable pageable) {

        Specification<Material> spec = Specification.where(null);
        if (q != null && !q.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"));
        }

        return ResponseEntity.ok(materialService.getAllMaterials(spec, pageable));
    }

    @Operation(summary = "Получить материал по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
    public ResponseEntity<Material> getMaterial(@Parameter(description = "ID материала") @PathVariable Long id) {
        return ResponseEntity.ok(materialService.getMaterialById(id));
    }

    @Operation(summary = "Создать новый материал")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Material> createMaterial(@RequestBody Material material) {
        return new ResponseEntity<>(materialService.createMaterial(material), HttpStatus.CREATED);
    }

    @Operation(summary = "Обновить материал")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Material> updateMaterial(
            @Parameter(description = "ID материала") @PathVariable Long id,
            @RequestBody Material material) {
        return ResponseEntity.ok(materialService.updateMaterial(id, material));
    }

    @Operation(summary = "Удалить материал")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMaterial(@Parameter(description = "ID материала") @PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }
}
