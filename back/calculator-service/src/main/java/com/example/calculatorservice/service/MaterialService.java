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

    public List<Material> getAllMaterials() {
        return materialRepository.findAll();
    }

    public List<Material> getMaterialsByType(MaterialType type) {
        return materialRepository.findByType(type);
    }

    public Material getMaterialById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден"));
    }
}
