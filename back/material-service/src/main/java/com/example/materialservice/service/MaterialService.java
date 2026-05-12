package com.example.materialservice.service;

import com.example.materialservice.dto.MaterialCreateRequest;
import com.example.materialservice.dto.MaterialResponse;
import com.example.materialservice.dto.MaterialUpdateRequest;
import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialType;
import com.example.materialservice.exception.ResourceNotFoundException;
import com.example.materialservice.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;

    /**
     * Получить пагинированный и фильтрованный список активных материалов.
     * <p>
     * Метод выполняет поиск материалов с应用ом (applying) указанного {@link Specification}
     * и преобразует сущности {@link Material} в DTO {@link MaterialResponse} через {@link #mapToResponse(Material)}.
     * Фильтрация по умолчанию excludes soft-deleted записи благодаря {@link org.hibernate.annotations.Where} на сущности.
     *
     * @param spec     спецификация JPA для динамической фильтрации (например, по имени); может быть {@code null}
     * @param pageable объект пагинации и сортировки (номер страницы, размер, сортировка)
     * @return страница {@link MaterialResponse} с содержимым материалов и мета-информацией пагинации
     */
    public Page<MaterialResponse> getAllMaterials(Specification<Material> spec, Pageable pageable) {
        return materialRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Получить список активных материалов определённого типа (MATERIAL или OPERATION).
     * <p>
     * Используется для выборки материалов или операций отдельно.
     * Возвращает только не удалённые записи (soft-deleted excluded).
     *
     * @param type тип материала (из enum {@link MaterialType})
     * @return список активных материалов указанного типа
     */
    public List<Material> getMaterialsByType(MaterialType type) {
        return materialRepository.findByType(type);
    }

    /**
     * Получить материал по его идентификатору.
     * <p>
     * Выполняет поиск по {@code id} среди активных (не удалённых) записей.
     * Если материал не найден или был soft-deleted, выбрасывается {@link ResourceNotFoundException}.
     *
     * @param id идентификатор материала (не {@code null})
     * @return DTO {@link MaterialResponse} с данными материала
     * @throws ResourceNotFoundException если материал с указанным {@code id} не существует
     */
    @Transactional(readOnly = true)
    public MaterialResponse getMaterialById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден"));
        return mapToResponse(material);
    }

    /**
     * Создать новый материал.
     * <p>
     * Валидирует единицу измерения (должна быть "м2" или "м.п.").
     * Устанавливает цену {@code 0.00} и коэффициент отходов {@code 1.0} если не указаны.
     * Сохраняет сущность в БД и возвращает DTO созданного материала.
     *
     * @param request DTO с данными для создания (имя, единица, цена, коэффициент)
     * @return DTO {@link MaterialResponse} созданного материала с присвоенным {@code id}
     * @throws IllegalArgumentException если единица измерения некорректна
     */
    public MaterialResponse createMaterial(MaterialCreateRequest request) {
        validateUnit(request.getUnit());
        Material material = new Material();
        material.setName(request.getName());
        material.setUnit(request.getUnit());
        material.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
        material.setWasteCoefficient(request.getWasteCoefficient() != null ? request.getWasteCoefficient() : BigDecimal.ONE);

        Material saved = materialRepository.save(material);
        return mapToResponse(saved);
    }

    /**
     * Обновить существующий материал.
     * <p>
     * Частичное обновление: изменяются только те поля, которые присутствуют в {@link MaterialUpdateRequest} (не {@code null}).
     * При изменении единицы измерения выполняется валидация.
     * Обновлённая сущность сохраняется и возвращается в виде DTO.
     *
     * @param id      идентификатор обновляемого материала
     * @param request DTO с полями для обновления (может содержать null для пропуска字段)
     * @return DTO {@link MaterialResponse} обновлённого материала
     * @throws ResourceNotFoundException если материал с {@code id} не найден
     * @throws IllegalArgumentException  если новая единица измерения некорректна
     */
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
        return mapToResponse(saved);
    }

    /**
     * Удалить (мягко) материал по ID.
     * <p>
     * Выполняет soft delete: устанавливает флаг {@code deleted = true} вместо физического удаления записи.
     * На сущности используется {@link org.hibernate.annotations.SQLDelete} для генерации UPDATE-запроса.
     *
     * @param id идентификатор материала для удаления
     * @throws ResourceNotFoundException если материал с {@code id} не найден
     */
    public void deleteMaterial(Long id) {
        Material material = getMaterialEntity(id);
        materialRepository.delete(material);
    }

    @Transactional(readOnly = true)
    private Material getMaterialEntity(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден"));
    }

    @Transactional(readOnly = true)
    private MaterialResponse mapToResponse(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getName(),
                material.getUnit(),
                material.getPrice(),
                material.getWasteCoefficient()
        );
    }

    private void validateUnit(String unit) {
        if (unit == null || (!unit.equals("м2") && !unit.equals("м.п."))) {
            throw new IllegalArgumentException("Единица измерения должна быть 'м2' или 'м.п.'");
        }
    }
}
