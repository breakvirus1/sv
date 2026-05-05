package com.example.materialservice.service;

import com.example.materialservice.dto.*;
import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialOperation;
import com.example.materialservice.entity.OperationType;
import com.example.materialservice.entity.OperationParameter;
import com.example.materialservice.entity.OperationAdditionalMaterial;
import com.example.materialservice.repository.MaterialOperationRepository;
import com.example.materialservice.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
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
public class MaterialOperationService {

    private final MaterialOperationRepository operationRepository;
    private final MaterialRepository materialRepository;

     public List<MaterialOperationResponse> getOperationsByMaterialId(Long materialId) {
         Material material = materialRepository.findById(materialId)
                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден"));
         return material.getOperations().stream()
                 .filter(op -> Boolean.TRUE.equals(op.getActive()))
                 .sorted((a, b) -> {
                     int cmp = Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                                               b.getSortOrder() != null ? b.getSortOrder() : 0);
                     if (cmp == 0) return a.getId().compareTo(b.getId());
                     return cmp;
                 })
                 .map(this::mapToResponse)
                 .collect(Collectors.toList());
     }


     public MaterialOperationResponse createOperation(Long materialId, MaterialOperationCreateRequest request) {
         Material material = materialRepository.findById(materialId)
                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден"));

         MaterialOperation operation = new MaterialOperation();
         operation.setMaterial(material);
         applyFromCreateDto(operation, request);

         MaterialOperation saved = operationRepository.save(operation);
         return mapToResponse(saved);
     }

     public MaterialOperationResponse updateOperation(Long materialId, Long operationId, MaterialOperationUpdateRequest request) {
         Material material = materialRepository.findById(materialId)
                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден"));

         MaterialOperation operation = operationRepository.findById(operationId)
                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Операция не найдена"));

         if (!operation.getMaterial().getId().equals(material.getId())) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Операция не принадлежит материалу");
         }

         applyFromUpdateDto(operation, request);
         MaterialOperation saved = operationRepository.save(operation);
         return mapToResponse(saved);
     }

      private void applyFromCreateDto(MaterialOperation operation, MaterialOperationCreateRequest request) {
          operation.setName(request.getName());
          operation.setDescription(request.getDescription());
          try {
              operation.setOperationType(OperationType.valueOf(request.getOperationType()));
          } catch (IllegalArgumentException e) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверный тип операции: " + request.getOperationType());
          }
          operation.setBasePrice(request.getBasePrice());
          operation.setUnit(request.getUnit() != null ? request.getUnit() : "шт");
          operation.setWasteCoefficient(request.getWasteCoefficient() != null ? request.getWasteCoefficient() : BigDecimal.ONE);
          operation.setRequiresDimensions(request.getRequiresDimensions() != null && request.getRequiresDimensions());
          operation.setAllowsAdditionalMaterials(request.getAllowsAdditionalMaterials() != null && request.getAllowsAdditionalMaterials());
          operation.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
          operation.setActive(request.getActive() != null ? request.getActive() : true);

          // Clear existing parameters & additional materials
          operation.getParameters().clear();
          operation.getAdditionalMaterials().clear();

          // Add parameters
          if (request.getParameters() != null) {
              for (OperationParameterDto paramDto : request.getParameters()) {
                  OperationParameter param = new OperationParameter();
                  param.setOperation(operation);
                  param.setParamKey(paramDto.getParamKey());
                  param.setDisplayName(paramDto.getDisplayName());
                  param.setType(paramDto.getType());
                  param.setUnit(paramDto.getUnit());
                  param.setDefaultValue(paramDto.getDefaultValue());
                  param.setRequired(Boolean.TRUE.equals(paramDto.getRequired()));
                  param.setSortOrder(paramDto.getSortOrder() != null ? paramDto.getSortOrder() : 0);
                  param.setDescription(paramDto.getDescription());
                  operation.getParameters().add(param);
              }
          }

         // Add additional materials
         if (request.getAdditionalMaterials() != null) {
             for (AdditionalMaterialDto matDto : request.getAdditionalMaterials()) {
                 Material additionalMat = materialRepository.findById(matDto.getMaterialId())
                         .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден: " + matDto.getMaterialId()));
                 OperationAdditionalMaterial opMat = new OperationAdditionalMaterial();
                 opMat.setOperation(operation);
                 opMat.setMaterial(additionalMat);
                 opMat.setDefaultQuantity(matDto.getDefaultQuantity());
                 opMat.setUnit(matDto.getUnit());
                 opMat.setPricePerUnit(matDto.getPricePerUnit());
                 operation.getAdditionalMaterials().add(opMat);
             }
         }
     }

      private void applyFromUpdateDto(MaterialOperation operation, MaterialOperationUpdateRequest request) {
          operation.setName(request.getName());
          operation.setDescription(request.getDescription());
          try {
              operation.setOperationType(OperationType.valueOf(request.getOperationType()));
          } catch (IllegalArgumentException e) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверный тип операции: " + request.getOperationType());
          }
          operation.setBasePrice(request.getBasePrice());
          operation.setUnit(request.getUnit() != null ? request.getUnit() : "шт");
          operation.setWasteCoefficient(request.getWasteCoefficient() != null ? request.getWasteCoefficient() : BigDecimal.ONE);
          operation.setRequiresDimensions(request.getRequiresDimensions() != null && request.getRequiresDimensions());
          operation.setAllowsAdditionalMaterials(request.getAllowsAdditionalMaterials() != null && request.getAllowsAdditionalMaterials());
          operation.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
          operation.setActive(request.getActive() != null ? request.getActive() : true);

          // Clear existing parameters & additional materials
          operation.getParameters().clear();
          operation.getAdditionalMaterials().clear();

          // Add parameters
          if (request.getParameters() != null) {
              for (OperationParameterDto paramDto : request.getParameters()) {
                  OperationParameter param = new OperationParameter();
                  param.setOperation(operation);
                  param.setParamKey(paramDto.getParamKey());
                  param.setDisplayName(paramDto.getDisplayName());
                  param.setType(paramDto.getType());
                  param.setUnit(paramDto.getUnit());
                  param.setDefaultValue(paramDto.getDefaultValue());
                  param.setRequired(Boolean.TRUE.equals(paramDto.getRequired()));
                  param.setSortOrder(paramDto.getSortOrder() != null ? paramDto.getSortOrder() : 0);
                  param.setDescription(paramDto.getDescription());
                  operation.getParameters().add(param);
              }
          }

         // Add additional materials
         if (request.getAdditionalMaterials() != null) {
             for (AdditionalMaterialDto matDto : request.getAdditionalMaterials()) {
                 Material additionalMat = materialRepository.findById(matDto.getMaterialId())
                         .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден: " + matDto.getMaterialId()));
                 OperationAdditionalMaterial opMat = new OperationAdditionalMaterial();
                 opMat.setOperation(operation);
                 opMat.setMaterial(additionalMat);
                 opMat.setDefaultQuantity(matDto.getDefaultQuantity());
                 opMat.setUnit(matDto.getUnit());
                 opMat.setPricePerUnit(matDto.getPricePerUnit());
                 operation.getAdditionalMaterials().add(opMat);
             }
         }
     }

    public void deleteOperation(Long materialId, Long operationId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден"));

        MaterialOperation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Операция не найдена"));

        if (!operation.getMaterial().getId().equals(material.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Операция не принадлежит материалу");
        }

        operationRepository.delete(operation);
    }

    private MaterialOperationResponse mapToResponse(MaterialOperation op) {
        List<OperationParameterDto> paramDtos = op.getParameters().stream()
                .map(p -> new OperationParameterDto(
                        p.getId(),
                        p.getParamKey(),
                        p.getDisplayName(),
                        p.getType(),
                        p.getUnit(),
                        p.getDefaultValue(),
                        p.getRequired(),
                        p.getSortOrder(),
                        p.getDescription()
                ))
                .collect(Collectors.toList());

        List<AdditionalMaterialDto> addMatDtos = op.getAdditionalMaterials().stream()
                .map(am -> new AdditionalMaterialDto(
                        am.getMaterial().getId(),
                        am.getMaterial().getName(),
                        am.getDefaultQuantity(),
                        am.getUnit(),
                        am.getPricePerUnit()
                ))
                .collect(Collectors.toList());

        return new MaterialOperationResponse(
                op.getId(),
                op.getMaterial().getId(),
                op.getMaterial().getName(),
                op.getName(),
                op.getDescription(),
                op.getOperationType() != null ? op.getOperationType().name() : null,
                op.getBasePrice(),
                op.getUnit(),
                op.getWasteCoefficient(),
                op.getRequiresDimensions(),
                op.getAllowsAdditionalMaterials(),
                op.getSortOrder(),
                op.getActive(),
                paramDtos,
                addMatDtos
        );
    }
}
