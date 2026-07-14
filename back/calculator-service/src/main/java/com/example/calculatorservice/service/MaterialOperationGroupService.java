package com.example.calculatorservice.service;

import com.example.calculatorservice.dto.GroupedOperationsResponse;
import com.example.calculatorservice.dto.MaterialOperationGroupCreateRequest;
import com.example.calculatorservice.dto.MaterialOperationGroupDto;
import com.example.calculatorservice.dto.MaterialOperationGroupUpdateRequest;
import com.example.calculatorservice.dto.OperationDto;
import com.example.calculatorservice.entity.MaterialOperationGroup;
import com.example.calculatorservice.entity.Operation;
import com.example.calculatorservice.entity.OperationGroup;
import com.example.calculatorservice.exception.BadRequestException;
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

    /**
     * Создаёт связь материала с операцией через группировку.
     *
     * <p>Перед созданием проверяет, что такая связь ещё не существует
     * (материал + группировка + операция). Если связь уже есть,
     * выбрасывает {@link BadRequestException}.</p>
     *
     * @param request запрос с идентификаторами материала, группировки и операции
     * @return созданная связь материала с операцией
     * @throws BadRequestException если связь уже существует
     */
    @Transactional
    public MaterialOperationGroup createMaterialOperationGroup(MaterialOperationGroupCreateRequest request) {
        if (mogRepository.existsByMaterialIdAndOperationGroupIdAndOperationIdAndDeletedFalse(
                request.getMaterialId(), request.getOperationGroupId(), request.getOperationId())) {
            throw new BadRequestException("Связь материала с операцией уже существует");
        }

        MaterialOperationGroup mog = new MaterialOperationGroup();
        mog.setMaterialId(request.getMaterialId());
        mog.setOperationGroupId(request.getOperationGroupId());
        mog.setOperationId(request.getOperationId());
        mog.setCreatedAt(LocalDateTime.now());
        mog.setUpdatedAt(LocalDateTime.now());
        return mogRepository.save(mog);
    }

    /**
     * Обновляет набор операций для материала. Удаляет все существующие связи
     * материала с операциями и создаёт новые на основе переданного списка
     * идентификаторов операций.
     *
     * <p>Для каждой операции определяется подходящая группировка по совпадению
     * названия операции с названием группировки (без учёта регистра). Если операция
     * не попала ни в одну группу, она помещается в группу «Прочие». Если такой
     * группы нет в базе, она создаётся автоматически.</p>
     *
     * @param materialId идентификатор материала
     * @param request запрос с новым списком идентификаторов операций
     * @return список созданных связей материала с операциями
     * @throws ResourceNotFoundException если одна из операций не найдена
     */
    @Transactional
    public List<MaterialOperationGroup> updateMaterialOperations(Long materialId, MaterialOperationGroupUpdateRequest request) {
        mogRepository.hardDeleteByMaterialId(materialId);
        mogRepository.flush();

        List<MaterialOperationGroup> newMappings = new ArrayList<>();
        if (request.getOperationIds() != null) {
            OperationGroup otherGroup = operationGroupRepository.findByName("Прочие")
                    .orElseGet(() -> {
                        OperationGroup g = new OperationGroup();
                        g.setName("Прочие");
                        return operationGroupRepository.save(g);
                    });

            for (Long opId : request.getOperationIds()) {
                Operation operation = operationRepository.findById(opId)
                        .orElseThrow(() -> new ResourceNotFoundException("Операция не найдена: " + opId));

                List<OperationGroup> allGroups = operationGroupRepository.findAll();
                boolean matched = false;
                for (OperationGroup group : allGroups) {
                    if (operation.getName() != null &&
                            operation.getName().toLowerCase().contains(group.getName().toLowerCase())) {
                        MaterialOperationGroupCreateRequest createReq = new MaterialOperationGroupCreateRequest();
                        createReq.setMaterialId(materialId);
                        createReq.setOperationGroupId(group.getId());
                        createReq.setOperationId(opId);
                        MaterialOperationGroup saved = createMaterialOperationGroup(createReq);
                        newMappings.add(saved);
                        matched = true;
                    }
                }

                if (!matched) {
                    MaterialOperationGroupCreateRequest createReq = new MaterialOperationGroupCreateRequest();
                    createReq.setMaterialId(materialId);
                    createReq.setOperationGroupId(otherGroup.getId());
                    createReq.setOperationId(opId);
                    MaterialOperationGroup saved = createMaterialOperationGroup(createReq);
                    newMappings.add(saved);
                }
            }
        }
        return newMappings;
    }

    /**
     * Возвращает все связи материала с операциями.
     *
     * @param materialId идентификатор материала
     * @return список связей материала с операциями
     */
    @Transactional(readOnly = true)
    public List<MaterialOperationGroup> getMaterialOperationGroups(Long materialId) {
        return mogRepository.findAllByMaterialId(materialId);
    }

    /**
     * Удаляет связь материала с операцией по идентификатору связи.
     *
     * @param id идентификатор связи material_operation_groups
     * @throws ResourceNotFoundException если связь не найдена
     */
    @Transactional
    public void deleteMaterialOperationGroup(Long id) {
        MaterialOperationGroup mog = mogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Связь не найдена"));
        mogRepository.delete(mog);
    }

    /**
     * Преобразует сущность связи материала с операцией в DTO.
     *
     * <p>Дополнительно загружает связанные сущности группировки и операции,
     * чтобы вернуть полную информацию. Если связанная сущность не найдена,
     * соответствующие поля в DTO останутся {@code null}.</p>
     *
     * @param mog сущность связи материала с операцией
     * @return DTO связи материала с операцией
     */
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
