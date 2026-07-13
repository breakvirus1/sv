package com.example.calculatorservice.service;

import com.example.calculatorservice.dto.GroupedOperationsResponse;
import com.example.calculatorservice.dto.OperationDto;
import com.example.calculatorservice.entity.Operation;
import com.example.calculatorservice.entity.OperationGroup;
import com.example.calculatorservice.exception.ResourceNotFoundException;
import com.example.calculatorservice.repository.MaterialOperationGroupRepository;
import com.example.calculatorservice.repository.OperationGroupRepository;
import com.example.calculatorservice.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository operationRepository;
    private final OperationGroupRepository operationGroupRepository;
    private final MaterialOperationGroupRepository materialOperationGroupRepository;

    public List<Operation> getAllOperations() {
        return operationRepository.findAll();
    }

    public Operation getOperationById(Long id) {
        return operationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Операция не найдена"));
    }

    public Operation save(Operation operation) {
        return operationRepository.save(operation);
    }

    public void deleteOperation(Long id) {
        Operation operation = getOperationById(id);
        operationRepository.delete(operation);
    }

    public List<OperationGroup> getAllOperationGroups() {
        return operationGroupRepository.findAll();
    }

    public OperationGroup getOperationGroupById(Long id) {
        return operationGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Группировка операций не найдена"));
    }

    public OperationGroup save(OperationGroup group) {
        return operationGroupRepository.save(group);
    }

    public void deleteOperationGroup(Long id) {
        OperationGroup group = getOperationGroupById(id);
        operationGroupRepository.delete(group);
    }

    public GroupedOperationsResponse getGroupedOperationsByMaterialId(Long materialId) {
        List<OperationGroup> allGroups = operationGroupRepository.findAll();
        List<Operation> allOperations = operationRepository.findAll();

        List<Operation> opsToShow = allOperations;
        if (materialId != null) {
            List<com.example.calculatorservice.entity.MaterialOperationGroup> mappings =
                    materialOperationGroupRepository.findAllByMaterialId(materialId);
            Set<Long> allowedOpIds = mappings.stream()
                    .map(com.example.calculatorservice.entity.MaterialOperationGroup::getOperationId)
                    .collect(Collectors.toSet());
            opsToShow = allOperations.stream()
                    .filter(op -> allowedOpIds.contains(op.getId()))
                    .toList();
        }

        Map<Long, Operation> operationMap = opsToShow.stream()
                .collect(Collectors.toMap(com.example.calculatorservice.entity.BaseEntity::getId, op -> op));

        List<GroupedOperationsResponse.GroupDto> result = new java.util.ArrayList<>();

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
}
