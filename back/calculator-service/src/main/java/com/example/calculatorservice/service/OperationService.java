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

import java.util.Collections;
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

    /**
     * Возвращает все операции из справочника.
     *
     * @return список всех операций
     */
    public List<Operation> getAllOperations() {
        return operationRepository.findAll();
    }

    /**
     * Возвращает операцию по идентификатору.
     *
     * @param id идентификатор операции
     * @return найденная операция
     * @throws ResourceNotFoundException если операция не найдена
     */
    public Operation getOperationById(Long id) {
        return operationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Операция не найдена"));
    }

    /**
     * Сохраняет операцию в базе данных (создаёт или обновляет).
     *
     * @param operation сущность операции
     * @return сохранённая операция
     */
    public Operation save(Operation operation) {
        return operationRepository.save(operation);
    }

    /**
     * Удаляет операцию по идентификатору.
     *
     * @param id идентификатор операции
     */
    public void deleteOperation(Long id) {
        Operation operation = getOperationById(id);
        operationRepository.delete(operation);
    }

    /**
     * Возвращает все группировки операций из справочника.
     *
     * @return список всех группировок
     */
    public List<OperationGroup> getAllOperationGroups() {
        return operationGroupRepository.findAll();
    }

    /**
     * Возвращает группировку операций по идентификатору.
     *
     * @param id идентификатор группировки
     * @return найденная группировка
     * @throws ResourceNotFoundException если группировка не найдена
     */
    public OperationGroup getOperationGroupById(Long id) {
        return operationGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Группировка операций не найдена"));
    }

    /**
     * Сохраняет группировку операций в базе данных (создаёт или обновляет).
     *
     * @param group сущность группировки
     * @return сохранённая группировка
     */
    public OperationGroup save(OperationGroup group) {
        return operationGroupRepository.save(group);
    }

    /**
     * Удаляет группировку операций по идентификатору.
     *
     * @param id идентификатор группировки
     */
    public void deleteOperationGroup(Long id) {
        OperationGroup group = getOperationGroupById(id);
        operationGroupRepository.delete(group);
    }

    /**
     * Формирует сгруппированный ответ операций, опционально фильтруя их по материалу.
     *
     * <p>Если передан {@code materialId}, возвращаются только те операции, которые
     * связаны с этим материалом через {@code material_operation_groups}. В противном случае
     * возвращаются все операции. Операции распределяются по группам по совпадению
     * названия операции с названием группировки (без учёта регистра). Операции, не
     * попавшие ни в одну группу, возвращаются отдельным списком {@code ungroupedOperations}.</p>
     *
     * @param materialId идентификатор материала или {@code null}, если фильтр не требуется
     * @return сгруппированный ответ операций
     */
    public GroupedOperationsResponse getGroupedOperationsByMaterialId(Long materialId) {
        List<Operation> allOperations = operationRepository.findAll();

        List<com.example.calculatorservice.entity.MaterialOperationGroup> mappings = Collections.emptyList();
        if (materialId != null) {
            mappings = materialOperationGroupRepository.findAllByMaterialId(materialId);
        }

        if (materialId == null) {
            List<OperationGroup> allGroups = operationGroupRepository.findAll();
            List<GroupedOperationsResponse.GroupDto> result = new java.util.ArrayList<>();
            List<OperationDto> ungroupedOperations = new java.util.ArrayList<>();

            for (OperationGroup group : allGroups) {
                List<Operation> matchingOps = allOperations.stream()
                        .filter(op -> op.getName() != null &&
                                op.getName().toLowerCase().contains(group.getName().toLowerCase()))
                        .toList();

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
                        .toList();

                result.add(new GroupedOperationsResponse.GroupDto(
                        group.getId(), group.getName(), groupOperations
                ));
            }

            Set<Long> matchedOpIds = result.stream()
                    .flatMap(g -> g.getOperations().stream())
                    .map(OperationDto::getId)
                    .collect(Collectors.toSet());

            List<Operation> remainingOps = allOperations.stream()
                    .filter(op -> !matchedOpIds.contains(op.getId()))
                    .toList();

            if (!remainingOps.isEmpty()) {
                for (Operation op : remainingOps) {
                    OperationDto dto = new OperationDto();
                    dto.setId(op.getId());
                    dto.setName(op.getName());
                    dto.setPrice(op.getPrice());
                    dto.setUnit(op.getUnit() != null ? op.getUnit().getDisplayName() : null);
                    dto.setHemWidthMm(op.getHemWidthMm());
                    dto.setHemCount(op.getHemCount());
                    ungroupedOperations.add(dto);
                }
            }

            return new GroupedOperationsResponse(result, ungroupedOperations);
        }

        Map<Long, List<com.example.calculatorservice.entity.MaterialOperationGroup>> mappingsByGroup = mappings.stream()
                .collect(Collectors.groupingBy(com.example.calculatorservice.entity.MaterialOperationGroup::getOperationGroupId));

        List<GroupedOperationsResponse.GroupDto> result = new java.util.ArrayList<>();
        List<OperationDto> ungroupedOperations = new java.util.ArrayList<>();

        for (Map.Entry<Long, List<com.example.calculatorservice.entity.MaterialOperationGroup>> entry : mappingsByGroup.entrySet()) {
            Long groupId = entry.getKey();
            List<com.example.calculatorservice.entity.MaterialOperationGroup> groupMappings = entry.getValue();

            if (groupId == null) {
                for (com.example.calculatorservice.entity.MaterialOperationGroup mog : groupMappings) {
                    Operation op = allOperations.stream()
                            .filter(o -> o.getId().equals(mog.getOperationId()))
                            .findFirst()
                            .orElse(null);
                    if (op != null) {
                        OperationDto dto = new OperationDto();
                        dto.setId(op.getId());
                        dto.setName(op.getName());
                        dto.setPrice(op.getPrice());
                        dto.setUnit(op.getUnit() != null ? op.getUnit().getDisplayName() : null);
                        dto.setHemWidthMm(op.getHemWidthMm());
                        dto.setHemCount(op.getHemCount());
                        ungroupedOperations.add(dto);
                    }
                }
                continue;
            }

            OperationGroup group = operationGroupRepository.findById(groupId).orElse(null);
            if (group == null) continue;

            List<OperationDto> groupOperations = groupMappings.stream()
                    .map(mog -> allOperations.stream()
                            .filter(op -> op.getId().equals(mog.getOperationId()))
                            .findFirst()
                            .orElse(null))
                    .filter(java.util.Objects::nonNull)
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

            if (!groupOperations.isEmpty()) {
                result.add(new GroupedOperationsResponse.GroupDto(
                        group.getId(), group.getName(), groupOperations
                ));
            }
        }

        GroupedOperationsResponse response = new GroupedOperationsResponse();
        response.setGroups(result);
        response.setUngroupedOperations(ungroupedOperations);
        return response;
    }
}
