package com.example.materialservice.service;

import com.example.common.entity.Material;
import com.example.materialservice.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialService {

    private final MaterialRepository materialRepository;

    public Page<Material> getAllMaterials(Specification<Material> spec, Pageable pageable) {
        return materialRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Material getMaterialById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Материал не найден"));
    }

    public Material createMaterial(Material material) {
        validateUnit(material.getUnit());
        return materialRepository.save(material);
    }

    public Material updateMaterial(Long id, Material materialDetails) {
        Material material = getMaterialById(id);
        validateUnit(materialDetails.getUnit());
        material.setName(materialDetails.getName());
        material.setUnit(materialDetails.getUnit());
        material.setPrice(materialDetails.getPrice());
        material.setWasteCoefficient(materialDetails.getWasteCoefficient());
        return materialRepository.save(material);
    }

    private void validateUnit(String unit) {
        if (unit == null || (!unit.equals("м2") && !unit.equals("м.п."))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Единица измерения должна быть 'м2' или 'м.п.'");
        }
    }

    public void deleteMaterial(Long id) {
        Material material = getMaterialById(id);
        materialRepository.delete(material);
    }
}
