package com.example.materialservice.service;

import com.example.materialservice.dto.MaterialCreateRequest;
import com.example.materialservice.dto.MaterialResponse;
import com.example.materialservice.dto.MaterialUpdateRequest;
import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialType;
import com.example.materialservice.exception.ResourceNotFoundException;
import com.example.materialservice.mapper.MaterialMapper;
import com.example.materialservice.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialMapper materialMapper;

    public Page<MaterialResponse> getAllMaterials(Specification<Material> spec, Pageable pageable) {
        return materialRepository.findAll(spec, pageable)
                .map(materialMapper::toDto);
    }

    public List<Material> getMaterialsByType(MaterialType type) {
        return materialRepository.findByType(type);
    }

    @Transactional(readOnly = true)
    public MaterialResponse getMaterialById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден"));
        return materialMapper.toDto(material);
    }

    public MaterialResponse createMaterial(MaterialCreateRequest request) {
        validateUnit(request.getUnit());
        Material material = materialMapper.toEntity(request);
        material.setType(MaterialType.MATERIAL);
        Material saved = materialRepository.save(material);
        return materialMapper.toDto(saved);
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
        return materialMapper.toDto(saved);
    }

    public void deleteMaterial(Long id) {
        Material material = getMaterialEntity(id);
        materialRepository.delete(material);
    }

    @Transactional(readOnly = true)
    private Material getMaterialEntity(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден"));
    }

    private void validateUnit(String unit) {
        if (unit == null || (!unit.equals("м2") && !unit.equals("м.п."))) {
            throw new IllegalArgumentException("Единица измерения должна быть 'м2' или 'м.п.'");
        }
    }
}
