package com.example.calculatorservice.service;

import com.example.calculatorservice.entity.Material;
import com.example.calculatorservice.entity.MaterialType;
import com.example.calculatorservice.exception.ResourceNotFoundException;
import com.example.calculatorservice.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;

    /**
     * Возвращает только активные материалы (deleted = false),
     * отфильтрованные по companyId (NULL = глобальные, видны всем).
     */
    public List<Material> getAllMaterials(Long companyId) {
        return materialRepository.findAll().stream()
                .filter(m -> !m.getDeleted())
                .filter(m -> companyId == null || m.getCompanyId() == null
                        || companyId.equals(m.getCompanyId()))
                .collect(Collectors.toList());
    }

    public List<Material> getAllMaterials() {
        return getAllMaterials(null);
    }

    public List<Material> getMaterialsByType(MaterialType type, Long companyId) {
        return materialRepository.findByType(type).stream()
                .filter(m -> !m.getDeleted())
                .filter(m -> companyId == null || m.getCompanyId() == null
                        || companyId.equals(m.getCompanyId()))
                .collect(Collectors.toList());
    }

    public List<Material> getMaterialsByType(MaterialType type) {
        return getMaterialsByType(type, null);
    }

    public Material getMaterialById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден"));
    }
}
