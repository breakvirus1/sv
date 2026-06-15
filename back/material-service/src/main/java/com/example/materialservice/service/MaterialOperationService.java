package com.example.materialservice.service;

import com.example.materialservice.dto.*;
import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialOperation;
import com.example.materialservice.entity.OperationType;
import com.example.materialservice.repository.MaterialOperationRepository;
import com.example.materialservice.repository.MaterialRepository;
import com.example.materialservice.util.TransliterationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialOperationService {

    private final MaterialOperationRepository operationRepository;
    private final MaterialRepository materialRepository;

    public List<MaterialOperationResponse> getOperationsByMaterialId(Long materialId) {
        if (!materialRepository.existsById(materialId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Материал не найден");
        }
        return operationRepository.findByMaterialIdAndActiveTrueOrderBySortOrderAsc(materialId).stream()
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

    /**
     * Расчёт количества для операции (люверсы, раскрой и др.).
     */
    public BigDecimal calculateQuantity(Long operationId, OperationCalculateRequest request) {
        MaterialOperation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Операция не найдена"));

        BigDecimal width = request.getWidth();
        BigDecimal height = request.getHeight();
        Integer itemCount = request.getItemCount();
        Map<String, Object> parameters = request.getParameters();

        if (width == null || height == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Width and height are required");
        }

        switch (operation.getOperationType()) {
            case EYELETS:
                // Конвертируем в мм
                int wmm = width.multiply(BigDecimal.valueOf(1000)).intValue();
                int hmm = height.multiply(BigDecimal.valueOf(1000)).intValue();

                Number step = null;
                Number edgeDistance = null;
                if (parameters != null) {
                    Object s = parameters.get("step");
                    Object e = parameters.get("edgeDistance");
                    if (s instanceof Number) step = (Number) s;
                    if (e instanceof Number) edgeDistance = (Number) e;
                }

                int s = (step != null) ? step.intValue() : 500;
                if (s <= 0) s = 500;

                int e = (edgeDistance != null) ? edgeDistance.intValue() : 50;
                if (e < 0) e = 0;

                int effW = wmm - 2 * e;
                int effH = hmm - 2 * e;
                if (effW < 0) effW = 0;
                if (effH < 0) effH = 0;

                int ceilW = (effW == 0) ? 0 : (effW + s - 1) / s;
                int ceilH = (effH == 0) ? 0 : (effH + s - 1) / s;

                int count = 2 * ceilW + 2 * ceilH;
                if (count < 4) {
                    count = 4;
                }

                int qty = (itemCount != null) ? itemCount : 1;
                return BigDecimal.valueOf((long) count * qty);

            case CUTTING:
                // Параметры: marginWidth (мм), marginHeight (мм), sides (шт)
                Number marginW = null;
                Number marginH = null;
                Number sidesNum = null;
                if (parameters != null) {
                    if (parameters.get("marginWidth") instanceof Number) marginW = (Number) parameters.get("marginWidth");
                    if (parameters.get("marginHeight") instanceof Number) marginH = (Number) parameters.get("marginHeight");
                    if (parameters.get("sides") instanceof Number) sidesNum = (Number) parameters.get("sides");
                }
                int mw = marginW != null ? marginW.intValue() : 50;
                int mh = marginH != null ? marginH.intValue() : 50;
                int sides = sidesNum != null ? sidesNum.intValue() : 1;

                // width/height в метрах -> переводим в мм для формулы: (marginW * width_mm * sides + marginH * height_mm * sides) / 1_000_000
                BigDecimal widthMm = width.multiply(BigDecimal.valueOf(1000));
                BigDecimal heightMm = height.multiply(BigDecimal.valueOf(1000));

                BigDecimal extraAreaMm2 = BigDecimal.valueOf(mw).multiply(widthMm).multiply(BigDecimal.valueOf(sides))
                        .add(BigDecimal.valueOf(mh).multiply(heightMm).multiply(BigDecimal.valueOf(sides)));

                int countItems = (itemCount != null) ? itemCount : 1;
                BigDecimal totalExtraAreaM2 = extraAreaMm2.divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(countItems));
                return totalExtraAreaM2;

            case PRINT:
                // Печать: площадь = width * height (м²) на одно изделие
                if (width == null || height == null) return BigDecimal.ONE;
                BigDecimal areaPerItem = width.multiply(height);
                return areaPerItem.multiply(BigDecimal.valueOf(itemCount != null ? itemCount : 1));

            case LAMINATION:
                // Ламинирование: площадь с припуском на нахлест (marginWidth, marginHeight в мм)
                if (width == null || height == null) return BigDecimal.ONE;
                Number lamMarginW = null;
                Number lamMarginH = null;
                if (parameters != null) {
                    if (parameters.get("marginWidth") instanceof Number) lamMarginW = (Number) parameters.get("marginWidth");
                    if (parameters.get("marginHeight") instanceof Number) lamMarginH = (Number) parameters.get("marginHeight");
                }
                double marginWm = (lamMarginW != null ? lamMarginW.doubleValue() : 20) / 1000.0;
                double marginHm = (lamMarginH != null ? lamMarginH.doubleValue() : 20) / 1000.0;
                BigDecimal effWlam = width.add(BigDecimal.valueOf(2 * marginWm));
                BigDecimal effHlam = height.add(BigDecimal.valueOf(2 * marginHm));
                BigDecimal areaLam = effWlam.multiply(effHlam);
                return areaLam.multiply(BigDecimal.valueOf(itemCount != null ? itemCount : 1));

            case WELDING:
                // Сварка: периметр = 2*(width+height) в метрах
                if (width == null || height == null) return BigDecimal.ONE;
                BigDecimal perimeter = width.add(height).multiply(BigDecimal.valueOf(2));
                return perimeter.multiply(BigDecimal.valueOf(itemCount != null ? itemCount : 1));

            default:
                // Для остальных типов возвращаем 1 или quantityFormula если будет добавлен позже
                return BigDecimal.ONE;
        }
    }

    private void applyFromCreateDto(MaterialOperation operation, MaterialOperationCreateRequest request) {
        operation.setName(request.getName());
        operation.setDescription(request.getDescription());
        operation.setOperationType(OperationType.valueOf(request.getOperationType()));
        operation.setBasePrice(request.getBasePrice());
        operation.setUnit(request.getUnit());
        operation.setWasteCoefficient(request.getWasteCoefficient());
        operation.setRequiresDimensions(request.getRequiresDimensions());
        operation.setAllowsAdditionalMaterials(request.getAllowsAdditionalMaterials());
        operation.setQuantityFormula(request.getQuantityFormula());
        operation.setSortOrder(request.getSortOrder());
        operation.setActive(request.getActive());
    }

    private void applyFromUpdateDto(MaterialOperation operation, MaterialOperationUpdateRequest request) {
        operation.setName(request.getName());
        operation.setDescription(request.getDescription());
        operation.setOperationType(OperationType.valueOf(request.getOperationType()));
        operation.setBasePrice(request.getBasePrice());
        operation.setUnit(request.getUnit());
        operation.setWasteCoefficient(request.getWasteCoefficient());
        operation.setRequiresDimensions(request.getRequiresDimensions());
        operation.setAllowsAdditionalMaterials(request.getAllowsAdditionalMaterials());
        operation.setQuantityFormula(request.getQuantityFormula());
        operation.setSortOrder(request.getSortOrder());
        operation.setActive(request.getActive());
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
                addMatDtos,
                op.getQuantityFormula()
        );
    }
}
