package com.example.materialservice.service;

import com.example.materialservice.dto.MaterialCreateRequest;
import com.example.materialservice.dto.MaterialOperationResponse;
import com.example.materialservice.dto.MaterialResponse;
import com.example.materialservice.dto.MaterialUpdateRequest;
import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialOperation;
import com.example.materialservice.repository.MaterialOperationRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialOperationRepository operationRepository;

    @Transactional(readOnly = true)
    public Page<MaterialResponse> getAllMaterials(Specification<Material> spec, Pageable pageable) {
        return materialRepository.findAll(spec, pageable)
                .map(material -> {
                    List<MaterialOperationResponse> ops = material.getOperations().stream()
                            .filter(MaterialOperation::getActive)
                            .sorted((a, b) -> {
                                int cmp = Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                                                          b.getSortOrder() != null ? b.getSortOrder() : 0);
                                return cmp != 0 ? cmp : a.getId().compareTo(b.getId());
                            })
                            .map(this::mapOperationToResponse)
                            .collect(Collectors.toList());
                    return mapToResponse(material, ops);
                });
    }

    @Transactional(readOnly = true)
    public MaterialResponse getMaterialById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден"));
        // Explicitly fetch operations to avoid lazy issues in DTO
        List<MaterialOperationResponse> ops = material.getOperations().stream()
                .filter(MaterialOperation::getActive)
                .sorted((a, b) -> {
                    int cmp = Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                                              b.getSortOrder() != null ? b.getSortOrder() : 0);
                    return cmp != 0 ? cmp : a.getId().compareTo(b.getId());
                })
                .map(this::mapOperationToResponse)
                .collect(Collectors.toList());
        return mapToResponse(material, ops);
    }

    public MaterialResponse createMaterial(MaterialCreateRequest request) {
        validateUnit(request.getUnit());
        Material material = new Material();
        material.setName(request.getName());
        material.setUnit(request.getUnit());
        material.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
        material.setWasteCoefficient(request.getWasteCoefficient() != null ? request.getWasteCoefficient() : BigDecimal.ONE);

        Material saved = materialRepository.save(material);

        List<MaterialOperationResponse> opsResponses = List.of();
        if (request.getOperations() != null && !request.getOperations().isEmpty()) {
            List<MaterialOperation> operations = request.getOperations().stream()
                .map(opReq -> {
                    MaterialOperation op = new MaterialOperation();
                    op.setMaterial(saved);
                    op.setName(opReq.getName());
                    op.setPricePerUnit(opReq.getPricePerUnit());
                    op.setNormSeconds(opReq.getNormSeconds());
                    op.setUnit(opReq.getUnit() != null ? opReq.getUnit() : "шт");
                    op.setSortOrder(opReq.getSortOrder() != null ? opReq.getSortOrder() : 0);
                    op.setActive(opReq.getActive() != null ? opReq.getActive() : true);
                    return op;
                })
                .collect(Collectors.toList());
            List<MaterialOperation> savedOps = operationRepository.saveAll(operations);
            opsResponses = savedOps.stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(
                            a.getSortOrder() != null ? a.getSortOrder() : 0,
                            b.getSortOrder() != null ? b.getSortOrder() : 0);
                    if (cmp != 0) return cmp;
                    return a.getId().compareTo(b.getId());
                })
                .map(this::mapOperationToResponse)
                .collect(Collectors.toList());
        }

        return mapToResponse(saved, opsResponses);
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
        List<MaterialOperationResponse> ops = saved.getOperations().stream()
                .filter(MaterialOperation::getActive)
                .sorted((a, b) -> {
                    int cmp = Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                                              b.getSortOrder() != null ? b.getSortOrder() : 0);
                    return cmp != 0 ? cmp : a.getId().compareTo(b.getId());
                })
                .map(this::mapOperationToResponse)
                .collect(Collectors.toList());
        return mapToResponse(saved, ops);
    }

    private void validateUnit(String unit) {
        if (unit == null || (!unit.equals("м2") && !unit.equals("м.п.") && !unit.equals("шт"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Единица измерения должна быть 'м2', 'м.п.' или 'шт'");
        }
    }

    public void deleteMaterial(Long id) {
        Material material = getMaterialEntity(id);
        materialRepository.delete(material);
    }

    @Transactional(readOnly = true)
    private Material getMaterialEntity(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден"));
    }

    @Transactional(readOnly = true)
    private MaterialResponse mapToResponse(Material material, List<MaterialOperationResponse> operations) {
        return new MaterialResponse(
                material.getId(),
                material.getName(),
                material.getUnit(),
                material.getPrice(),
                material.getWasteCoefficient(),
                operations
        );
    }

    @Transactional(readOnly = true)
    private MaterialOperationResponse mapOperationToResponse(MaterialOperation op) {
        return new MaterialOperationResponse(
                op.getId(),
                op.getName(),
                op.getPricePerUnit(),
                op.getNormSeconds(),
                op.getUnit(),
                op.getSortOrder(),
                op.getActive()
        );
    }
}
