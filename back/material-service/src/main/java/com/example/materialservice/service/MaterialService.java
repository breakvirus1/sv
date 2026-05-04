package com.example.materialservice.service;

import com.example.materialservice.dto.MaterialCreateRequest;
import com.example.materialservice.dto.MaterialResponse;
import com.example.materialservice.dto.MaterialUpdateRequest;
import com.example.materialservice.entity.Material;
import com.example.materialservice.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialService {

    private final MaterialRepository materialRepository;

    public Page<MaterialResponse> getAllMaterials(Specification<Material> spec, Pageable pageable) {
        return materialRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public MaterialResponse getMaterialById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Материал не найден"));
        return mapToResponse(material);
    }

    public MaterialResponse createMaterial(MaterialCreateRequest request) {
        validateUnit(request.getUnit());
        Material material = new Material();
        material.setName(request.getName());
        material.setUnit(request.getUnit());
        material.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
        material.setWasteCoefficient(request.getWasteCoefficient() != null ? request.getWasteCoefficient() : BigDecimal.ONE);

        Material saved = materialRepository.save(material);
        return mapToResponse(saved);
    }

    public MaterialResponse updateMaterial(Long id, MaterialUpdateRequest request) {
        Material material = getMaterialEntity(id);
        if (request.getName() != null) {
            material.setName(request.getName());
        }
        if (request.getUnit() != null) {
            validateUnit(request.getUnit());
            material.setUnit(request.getUnit());
        }
        if (request.getPrice() != null) {
            material.setPrice(request.getPrice());
        }
        if (request.getWasteCoefficient() != null) {
            material.setWasteCoefficient(request.getWasteCoefficient());
        }
        Material saved = materialRepository.save(material);
        return mapToResponse(saved);
    }

    private void validateUnit(String unit) {
        if (unit == null || (!unit.equals("м2") && !unit.equals("м.п."))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Единица измерения должна быть 'м2' или 'м.п.'");
        }
    }

    public void deleteMaterial(Long id) {
        Material material = getMaterialEntity(id);
        materialRepository.delete(material);
    }

    @Transactional(readOnly = true)
    private Material getMaterialEntity(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Материал не найден"));
    }

    @Transactional(readOnly = true)
    private MaterialResponse mapToResponse(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getName(),
                material.getUnit(),
                material.getPrice(),
                material.getWasteCoefficient()
        );
    }
}
