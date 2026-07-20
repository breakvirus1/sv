package com.example.calculatorservice.controller;

import com.example.calculatorservice.dto.OperationGroupCreateRequest;
import com.example.calculatorservice.dto.OperationGroupDto;
import com.example.calculatorservice.dto.OperationGroupUpdateRequest;
import com.example.calculatorservice.entity.OperationGroup;
import com.example.calculatorservice.exception.BadRequestException;
import com.example.calculatorservice.repository.OperationGroupRepository;
import com.example.calculatorservice.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/operation-groups")
@RequiredArgsConstructor
public class OperationGroupAdminController {

    private final OperationService operationService;
    private final OperationGroupRepository operationGroupRepository;

    /**
     * Возвращает список всех группировок операций, включая удалённые.
     *
     * @return список DTO группировок операций
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<List<OperationGroupDto>> getAllOperationGroups() {
        return ResponseEntity.ok(
                operationService.getAllOperationGroups().stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    /**
     * Создаёт новую группировку операций.
     *
     * <p>Проверяет уникальность названия среди всех записей, включая удалённые.
     * Если группировка с таким названием уже существует, возвращает 400.</p>
     *
     * @param request запрос с названием новой группировки
     * @return созданная группировка операций
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<OperationGroupDto> createOperationGroup(@RequestBody OperationGroupCreateRequest request) {
        if (operationGroupRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Группировка с таким названием уже существует");
        }
        OperationGroup group = new OperationGroup();
        group.setName(request.getName());
        OperationGroup saved = operationService.save(group);
        return ResponseEntity.ok(toDto(saved));
    }

    /**
     * Обновляет название группировки операций.
     *
     * <p>Проверяет, что новое название не занято другой группировкой
     * (исключая текущую). Если название уже используется, возвращает 400.</p>
     *
     * @param id идентификатор группировки
     * @param request запрос с новым названием
     * @return обновлённая группировка операций
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<OperationGroupDto> updateOperationGroup(@PathVariable Long id, @RequestBody OperationGroupUpdateRequest request) {
        operationGroupRepository.findByNameExcludingId(request.getName(), id).ifPresent(existing -> {
            throw new BadRequestException("Группировка с таким названием уже существует");
        });
        OperationGroup group = operationService.getOperationGroupById(id);
        group.setName(request.getName());
        OperationGroup updated = operationService.save(group);
        return ResponseEntity.ok(toDto(updated));
    }

    /**
     * Удаляет группировку операций по идентификатору.
     *
     * @param id идентификатор группировки
     * @return ответ 204 No Content
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperationGroup(@PathVariable Long id) {
        operationService.deleteOperationGroup(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Преобразует сущность группировки операций в DTO.
     *
     * @param group сущность группировки
     * @return DTO группировки операций
     */
    private OperationGroupDto toDto(OperationGroup group) {
        OperationGroupDto dto = new OperationGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        return dto;
    }
}
