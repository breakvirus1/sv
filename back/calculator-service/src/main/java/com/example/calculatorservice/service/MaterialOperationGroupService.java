package com.example.calculatorservice.service;

import com.example.calculatorservice.dto.GroupedOperationsResponse;
import com.example.calculatorservice.dto.MaterialOperationGroupCreateRequest;
import com.example.calculatorservice.dto.MaterialOperationGroupDto;
import com.example.calculatorservice.dto.MaterialOperationGroupUpdateRequest;
import com.example.calculatorservice.dto.OperationDto;
import com.example.calculatorservice.entity.MaterialOperationGroup;
import com.example.calculatorservice.entity.Operation;
import com.example.calculatorservice.entity.OperationGroup;
import com.example.calculatorservice.exception.ResourceNotFoundException;
import com.example.calculatorservice.repository.MaterialOperationGroupRepository;
import com.example.calculatorservice.repository.OperationGroupRepository;
import com.example.calculatorservice.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialOperationGroupService {

    private final MaterialOperationGroupRepository mogRepository;
    private final OperationGroupRepository operationGroupRepository;
    private final OperationRepository operationRepository;

    @Transactional(readOnly = true)
    public GroupedOperationsResponse getGroupedOperations(Long materialId) {
        List<OperationGroup> allGroups = operationGroupRepository.findAll();
        List<Operation> allOperations = operationRepository.findAll();

        Map<Long, Operation> operationMap = allOperations.stream()
                .collect(Collectors.toMap(com.example.calculatorservice.entity.BaseEntity::getId, op -> op));

        List<MaterialOperationGroup> mappings = materialId != null
                ? mogRepository.findAllByMaterialId(materialId)
                : Collections.emptyList();

        Set<Long> allowedOpIds = mappings.stream()
                .map(MaterialOperationGroup::getOperationId)
                .collect(Collectors.toSet());

        List<Operation> opsToShow = materialId != null
                ? allOperations.stream().filter(op -> allowedOpIds.contains(op.getId())).toList()
                : allOperations;

        List<GroupedOperationsResponse.GroupDto> result = new ArrayList<>();

        for (OperationGroup group : allGroups) {
            List<Operation> matchingOps = opsToShow.stream()
                    .filter(op -> op.getName() != null &&
                            op.getName().toLowerCase().contains(group.getName().toLowerCase()))
                    .collect(Collectors.toList());

            if (matchingOps.isEmpty()) continue;

            List<OperationDto> groupOperations = matchingOps.stream()
                    .map(op -> {
                        OperationDto dto = new OperationDto();
                        dto.setId(op.getId());
                        dto.setName(op.getName());
                        dto.setPrice(op.getPrice());
                        dto.setUnit(op.getUnit() != null ? op.getUnit().getDisplayName() : null);
                        dto.setHemWidthMm(op.getHemWidthMm());
                        dto.setHemCount(op.getHemCount());
                        return dto;
                    })
                    .collect(Collectors.toList());

            result.add(new GroupedOperationsResponse.GroupDto(
                    group.getId(), group.getName(), groupOperations
            ));
        }

        return new GroupedOperationsResponse(result);
    }

    @Transactional
    public MaterialOperationGroup createMaterialOperationGroup(MaterialOperationGroupCreateRequest request) {
        if (mogRepository.existsByMaterialIdAndOperationGroupIdAndOperationIdAndDeletedFalse(
                request.getMaterialId(), request.getOperationGroupId(), request.getOperationId())) {
            return mogRepository.findByMaterialIdAndDeletedFalse(request.getMaterialId()).stream()
                    .filter(mog -> mog.getOperationGroupId().equals(request.getOperationGroupId())
                            && mog.getOperationId().equals(request.getOperationId()))
                    .findFirst()
                    .orElse(null);
        }

        MaterialOperationGroup mog = new MaterialOperationGroup();
        mog.setMaterialId(request.getMaterialId());
        mog.setOperationGroupId(request.getOperationGroupId());
        mog.setOperationId(request.getOperationId());
        mog.setCreatedAt(LocalDateTime.now());
        mog.setUpdatedAt(LocalDateTime.now());
        return mogRepository.save(mog);
    }

    @Transactional
    public List<MaterialOperationGroup> updateMaterialOperations(Long materialId, MaterialOperationGroupUpdateRequest request) {
        mogRepository.hardDeleteByMaterialId(materialId);
        mogRepository.flush();

        List<MaterialOperationGroup> newMappings = new ArrayList<>();
        if (request.getOperationIds() != null) {
            for (Long opId : request.getOperationIds()) {
                Operation operation = operationRepository.findById(opId)
                        .orElseThrow(() -> new ResourceNotFoundException("Операция не найдена: " + opId));

                List<OperationGroup> allGroups = operationGroupRepository.findAll();
                for (OperationGroup group : allGroups) {
                    if (operation.getName() != null &&
                            operation.getName().toLowerCase().contains(group.getName().toLowerCase())) {
                        MaterialOperationGroupCreateRequest createReq = new MaterialOperationGroupCreateRequest();
                        createReq.setMaterialId(materialId);
                        createReq.setOperationGroupId(group.getId());
                        createReq.setOperationId(opId);
                        MaterialOperationGroup saved = createMaterialOperationGroup(createReq);
                        if (saved != null) newMappings.add(saved);
                    }
                }
            }
        }
        return newMappings;
    }

    @Transactional(readOnly = true)
    public List<MaterialOperationGroup> getMaterialOperationGroups(Long materialId) {
        return mogRepository.findAllByMaterialId(materialId);
    }

    @Transactional
    public void deleteMaterialOperationGroup(Long id) {
        MaterialOperationGroup mog = mogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Связь не найдена"));
        mogRepository.delete(mog);
    }

    public MaterialOperationGroupDto toDto(MaterialOperationGroup mog) {
        MaterialOperationGroupDto dto = new MaterialOperationGroupDto();
        dto.setId(mog.getId());
        dto.setMaterialId(mog.getMaterialId());

        try {
            OperationGroup group = operationGroupRepository.findById(mog.getOperationGroupId()).orElse(null);
            if (group != null) {
                com.example.calculatorservice.dto.OperationGroupDto groupDto = new com.example.calculatorservice.dto.OperationGroupDto();
                groupDto.setId(group.getId());
                groupDto.setName(group.getName());
                dto.setGroup(groupDto);
            }
        } catch (Exception e) {
            log.warn("Failed to load operation group {} for mapping {}: {}", mog.getOperationGroupId(), mog.getId(), e.getMessage());
        }

        try {
            Operation operation = operationRepository.findById(mog.getOperationId()).orElse(null);
            if (operation != null) {
                OperationDto opDto = new OperationDto();
                opDto.setId(operation.getId());
                opDto.setName(operation.getName());
                opDto.setPrice(operation.getPrice());
                opDto.setUnit(operation.getUnit() != null ? operation.getUnit().getDisplayName() : null);
                opDto.setHemWidthMm(operation.getHemWidthMm());
                opDto.setHemCount(operation.getHemCount());
                dto.setOperation(opDto);
            }
        } catch (Exception e) {
            log.warn("Failed to load operation {} for mapping {}: {}", mog.getOperationId(), mog.getId(), e.getMessage());
        }

        return dto;
    }
}
