package com.example.calculatorservice.service;

import com.example.calculatorservice.entity.Operation;
import com.example.calculatorservice.entity.OperationGroup;
import com.example.calculatorservice.entity.MaterialOperation;
import com.example.calculatorservice.exception.ResourceNotFoundException;
import com.example.calculatorservice.repository.OperationGroupRepository;
import com.example.calculatorservice.repository.MaterialOperationRepository;
import com.example.calculatorservice.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository operationRepository;
    private final OperationGroupRepository operationGroupRepository;
    private final MaterialOperationRepository materialOperationRepository;

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

    public List<Operation> getOperationsByMaterialId(Long materialId) {
        List<MaterialOperation> links = materialOperationRepository.findByMaterialIdAndDeletedFalse(materialId);
        List<Long> operationIds = links.stream()
                .map(MaterialOperation::getOperationId)
                .collect(Collectors.toList());
        return operationRepository.findAllById(operationIds);
    }

    public void setMaterialOperations(Long materialId, List<Long> operationIds) {
        materialOperationRepository.deleteByMaterialIdAndDeletedFalse(materialId);
        List<MaterialOperation> links = operationIds.stream()
                .distinct()
                .map(opId -> {
                    MaterialOperation link = new MaterialOperation();
                    link.setMaterialId(materialId);
                    link.setOperationId(opId);
                    return link;
                })
                .collect(Collectors.toList());
        materialOperationRepository.saveAll(links);
    }
}
