package com.example.zakazservice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.dto.SubZakazDto;
import com.example.common.entity.Material;
import com.example.common.entity.SubZakaz;
import com.example.common.entity.Zakaz;
import com.example.zakazservice.repository.MaterialRepository;
import com.example.zakazservice.mapper.SubZakazMapper;
import com.example.zakazservice.repository.SubZakazRepository;
import com.example.zakazservice.repository.ZakazRepository;
import com.example.zakazservice.response.SubZakazResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubZakazService {

    private final SubZakazRepository subZakazRepository;
    private final ZakazRepository zakazRepository;
    private final SubZakazMapper subZakazMapper;
    private final MaterialRepository materialRepository;

    public SubZakazResponse addSubZakaz(Long zakazId, SubZakazDto dto) {
        Zakaz zakaz = zakazRepository.findById(zakazId)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + zakazId));

        SubZakaz subZakaz = subZakazMapper.toEntity(dto);

        Material material = materialRepository.findById(dto.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Material not found with ID: " + dto.getMaterialId()));
        subZakaz.setMaterial(material);

        subZakaz.setZakaz(zakaz);

        // Calculate price = width * height * material.price
        double price = subZakaz.getWidth() * subZakaz.getHeight() * material.getPrice();
        subZakaz.setPrice(price);
        subZakaz.setCena(price);

        SubZakaz saved = subZakazRepository.save(subZakaz);
        return subZakazMapper.toSubZakazResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubZakazResponse> getAllByZakazId(Long zakazId) {
        Zakaz zakaz = zakazRepository.findById(zakazId)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + zakazId));
        List<SubZakaz> subZakazList = subZakazRepository.findAllByZakazId(zakaz.getId());
        return subZakazList.stream()
                .map(subZakazMapper::toSubZakazResponse)
                .collect(Collectors.toList());
    }

    public SubZakazResponse updateSubZakaz(Long id, SubZakazDto dto) {
        SubZakaz subZakaz = subZakazRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubZakaz not found with ID: " + id));

        if (dto.getMaterialId() != null) {
            Material material = materialRepository.findById(dto.getMaterialId())
                    .orElseThrow(() -> new RuntimeException("Material not found with ID: " + dto.getMaterialId()));
    subZakaz.setMaterial(material);
        }
        if (dto.getWidth() != null) {
            subZakaz.setWidth(dto.getWidth());
        }
        if (dto.getHeight() != null) {
            subZakaz.setHeight(dto.getHeight());
        }

        // Recalculate price
        double price = subZakaz.getWidth() * subZakaz.getHeight() * subZakaz.getMaterial().getPrice();
        subZakaz.setPrice(price);
        subZakaz.setCena(price);

        SubZakaz saved = subZakazRepository.save(subZakaz);
        return subZakazMapper.toSubZakazResponse(saved);
    }

    public void deleteSubZakaz(Long id) {
        subZakazRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Double calculateZakazSum(Long zakazId) {
        List<SubZakaz> subZakazList = subZakazRepository.findAllByZakazId(zakazId);
        return subZakazList.stream()
                .mapToDouble(SubZakaz::getPrice)
                .sum();
    }
}
