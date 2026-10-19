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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialMapper materialMapper;

    @Cacheable(value = "materials", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + (#spec?.toString() ?: 'all') + '_' + (#companyId ?: 'all')")
    public Page<MaterialResponse> getAllMaterials(Specification<Material> spec, Pageable pageable, Long companyId) {
        Specification<Material> finalSpec = spec == null ? Specification.where(null) : spec;
        if (companyId != null) {
            finalSpec = finalSpec.and((root, query, cb) -> cb.or(
                    cb.isNull(root.get("companyId")),
                    cb.equal(root.get("companyId"), companyId)));
        }
        return materialRepository.findAll(finalSpec, pageable)
                .map(materialMapper::toDto);
    }

    @Cacheable(value = "materials", key = "'type_' + #type + '_' + (#companyId ?: 'all')")
    public List<Material> getMaterialsByType(MaterialType type, Long companyId) {
        List<Material> result = materialRepository.findByType(type);
        if (companyId != null) {
            result = result.stream()
                    .filter(m -> m.getCompanyId() == null || companyId.equals(m.getCompanyId()))
                    .collect(Collectors.toList());
        }
        return result;
    }

    @Cacheable(value = "materials", key = "'id_' + #id")
    @Transactional(readOnly = true)
    public MaterialResponse getMaterialById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден"));
        return materialMapper.toDto(material);
    }

    @CacheEvict(value = "materials", allEntries = true)
    public MaterialResponse createMaterial(MaterialCreateRequest request, Long companyId) {
        validateUnit(request.getUnit());
        Material material = materialMapper.toEntity(request);
        material.setType(MaterialType.MATERIAL);
        material.setCompanyId(companyId);
        Material saved = materialRepository.save(material);
        return materialMapper.toDto(saved);
    }

    @CacheEvict(value = "materials", allEntries = true)
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

    @CacheEvict(value = "materials", allEntries = true)
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
        if (unit == null || (!unit.equals("м2") && !unit.equals("м.п.") && !unit.equals("шт"))) {
            throw new IllegalArgumentException("Единица измерения должна быть 'м2', 'м.п.' или 'шт'");
        }
    }
}
