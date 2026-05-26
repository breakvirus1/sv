package com.example.calculatorservice.service;

import com.example.calculatorservice.entity.*;
import com.example.calculatorservice.dto.request.CalculationRequestDto;
import com.example.calculatorservice.dto.response.CalculationResponseDto;
import com.example.calculatorservice.dto.response.EyeletResultDto;
import com.example.calculatorservice.exception.BadRequestException;
import com.example.calculatorservice.exception.ResourceNotFoundException;
import com.example.calculatorservice.mapper.CalculationMapper;
import com.example.calculatorservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CalculationService {

    private final MaterialRepository materialRepository;
    private final EyeletRepository eyeletRepository;
    private final OperationRepository operationRepository;
    private final CalculationRepository calculationRepository;
    private final CalculationMapper calculationMapper;

    public CalculationResponseDto createAndCalculate(CalculationRequestDto request) {
        Material material = materialRepository.findByIdAndDeletedFalse(request.getMaterialId())
                .orElseThrow(() -> new BadRequestException("Материал не найден"));

        Calculation calc = buildCalculationFromRequest(request, material);
        validateCalculation(calc);
        calculateTotalPrice(calc);

        Calculation saved = calculationRepository.save(calc);
        CalculationResponseDto dto = calculationMapper.toResponseDto(saved);
        populateEyeletResultIfPresent(saved, dto);
        return dto;
    }

    /**
     * Вычисляет себестоимость позиции заказа без сохранения в БД.
     * Используется order-service для расчёта итогов по материалам и операциям.
     */
    public CalculationResponseDto calculateWithoutSaving(CalculationRequestDto request) {
        Material material = materialRepository.findByIdAndDeletedFalse(request.getMaterialId())
                .orElseThrow(() -> new BadRequestException("Материал не найден"));

        Calculation calc = buildCalculationFromRequest(request, material);
        calc.setCreatedAt(LocalDateTime.now());
        validateCalculation(calc);
        calculateTotalPrice(calc);
        // Не сохраняем в БД — сразу маппим в DTO (id будет null)
        CalculationResponseDto dto = calculationMapper.toResponseDto(calc);
        populateEyeletResultIfPresent(calc, dto);
        return dto;
    }

    private Calculation buildCalculationFromRequest(CalculationRequestDto request, Material material) {
        Calculation calc = new Calculation();
        calc.setMaterial(material);
        calc.setWidthM(request.getWidthM());
        calc.setHeightM(request.getHeightM() != null ? request.getHeightM() : BigDecimal.ONE);
        calc.setDpi(request.getDpi());
        calc.setPodvorotMmHorizontal(request.getPodvorotMmHorizontal());
        calc.setPodvorotMmVertical(request.getPodvorotMmVertical());
        calc.setPodvorotCountPerSide(request.getPodvorotCountPerSide() != null ? request.getPodvorotCountPerSide() : 2);
        calc.setEyeletStepCm(request.getEyeletStepCm());

        if (request.getEyeletId() != null) {
            Eyelet eyelet = eyeletRepository.findById(request.getEyeletId())
                    .orElseThrow(() -> new BadRequestException("Люверс не найден"));
            calc.setEyelet(eyelet);
        }

        if (request.getOperationIds() != null) {
            for (Long opId : request.getOperationIds()) {
                Operation op = operationRepository.findById(opId)
                        .orElseThrow(() -> new BadRequestException("Операция с ID " + opId + " не найдена"));
                CalculationOperation calcOp = new CalculationOperation();
                calcOp.setOperation(op);
                calcOp.setPricePerUnit(op.getPrice());
                calc.addOperation(calcOp);
            }
        }
        return calc;
    }

    /**
     * Вычисляет площадь материала с учётом подворотов (без сохранения).
     * Используется для оперативного расчёта в интерфейсе.
     */
    public BigDecimal calculateAreaWithPodvorot(BigDecimal widthM, BigDecimal heightM,
                                                 BigDecimal podvorotMmHorizontal,
                                                 BigDecimal podvorotMmVertical,
                                                 Integer podvorotCountPerSide) {
        if (podvorotCountPerSide == null) podvorotCountPerSide = 2;

        BigDecimal effectiveWidth = widthM;
        BigDecimal effectiveHeight = heightM;

        if (podvorotMmHorizontal != null && podvorotMmHorizontal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal extraW = podvorotMmHorizontal
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(podvorotCountPerSide * 2));
            effectiveWidth = effectiveWidth.add(extraW);
        }

        if (podvorotMmVertical != null && podvorotMmVertical.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal extraH = podvorotMmVertical
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(podvorotCountPerSide * 2));
            effectiveHeight = effectiveHeight.add(extraH);
        }

        return effectiveWidth.multiply(effectiveHeight);
    }

    /**
     * Рассчитывает стоимость одной позиции материала с учётом подворотов и операций (без сохранения).
     * Используется для оперативного расчёта в интерфейсе.
     *
     * @return итоговая стоимость = стоимость материала + стоимость операций
     */
    public BigDecimal calculateItemCost(Long materialId, BigDecimal widthM, BigDecimal heightM,
                                         BigDecimal podvorotMmHorizontal, BigDecimal podvorotMmVertical,
                                         Integer podvorotCountPerSide, List<Long> operationIds) {
        Material material = materialRepository.findByIdAndDeletedFalse(materialId)
                .orElseThrow(() -> new BadRequestException("Материал не найден"));

        // Создаём временный объект Calculation (не сохраняем), чтобы переиспользовать существующую логику
        Calculation calc = new Calculation();
        calc.setMaterial(material);
        calc.setWidthM(widthM);
        calc.setHeightM(heightM);
        calc.setPodvorotMmHorizontal(podvorotMmHorizontal);
        calc.setPodvorotMmVertical(podvorotMmVertical);
        calc.setPodvorotCountPerSide(podvorotCountPerSide != null ? podvorotCountPerSide : 2);

        // Присоединяем операции, если они указаны
        if (operationIds != null) {
            for (Long opId : operationIds) {
                Operation op = operationRepository.findById(opId)
                        .orElseThrow(() -> new BadRequestException("Операция с ID " + opId + " не найдена"));
                CalculationOperation calcOp = new CalculationOperation();
                calcOp.setOperation(op);
                calcOp.setPricePerUnit(op.getPrice());
                calc.addOperation(calcOp);
            }
        }

        // Вычисляем площадь материала с учётом подворотов
        BigDecimal materialArea = calculateAreaWithPodvorot(widthM, heightM, podvorotMmHorizontal, podvorotMmVertical, calc.getPodvorotCountPerSide());
        BigDecimal materialPrice = material.getPricePerSquareMeter();
        BigDecimal wasteCoeff = material.getWasteCoefficient();
        if (wasteCoeff == null) wasteCoeff = BigDecimal.ONE;
        BigDecimal materialCost = materialArea.multiply(materialPrice).multiply(wasteCoeff);

        // Вычисляем стоимость операций
        BigDecimal operationsCost = BigDecimal.ZERO;
        for (CalculationOperation calcOp : calc.getSelectedOperations()) {
            Operation op = calcOp.getOperation();
            BigDecimal quantity = calculateOperationQuantity(calc, op);
            calcOp.setQuantity(quantity);
            BigDecimal subtotal = quantity.multiply(calcOp.getPricePerUnit());
            calcOp.setSubtotal(subtotal);
            operationsCost = operationsCost.add(subtotal);
        }

        BigDecimal total = materialCost.add(operationsCost);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

     public CalculationResponseDto getById(Long id) {
        Calculation calc = calculationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Расчёт не найден"));
        CalculationResponseDto dto = calculationMapper.toResponseDto(calc);
        populateEyeletResultIfPresent(calc, dto);
        return dto;
    }

    private void calculateTotalPrice(Calculation calc) {
        // 1. Площадь материала (с учётом подворотов, если указаны)
        BigDecimal materialArea = calculateMaterialArea(calc);

        // Стоимость материала = площадь × цена за м² × коэффициент отхода
        BigDecimal materialPrice = calc.getMaterial().getPricePerSquareMeter();
        BigDecimal wasteCoeff = calc.getMaterial().getWasteCoefficient();
        if (wasteCoeff == null) wasteCoeff = BigDecimal.ONE;
        BigDecimal materialCost = materialArea.multiply(materialPrice).multiply(wasteCoeff);

        BigDecimal total = materialCost;

        // 2. Стоимость операций
        for (CalculationOperation calcOp : calc.getSelectedOperations()) {
            Operation op = calcOp.getOperation();
            BigDecimal quantity = calculateOperationQuantity(calc, op);
            calcOp.setQuantity(quantity);
            BigDecimal pricePerUnit = calcOp.getPricePerUnit();
            BigDecimal subtotal = quantity.multiply(pricePerUnit);
            calcOp.setSubtotal(subtotal);
            total = total.add(subtotal);
        }

        // 3. Стоимость люверсов (фурнитура)
        if (calc.getEyelet() != null) {
            BigDecimal eyeletQty = calculateEyeletsQuantity(calc);
            BigDecimal eyeletPrice = calc.getEyelet().getPricePerPiece();
            BigDecimal eyeletCost = eyeletQty.multiply(eyeletPrice);
            total = total.add(eyeletCost);
        }

        calc.setTotalPrice(total.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateMaterialArea(Calculation c) {
        BigDecimal width = c.getWidthM();
        BigDecimal height = c.getHeightM();

        // Применяем подвороты (припуски), если указаны
        if (c.hasPodvorot()) {
            BigDecimal extraW = BigDecimal.ZERO;
            BigDecimal extraH = BigDecimal.ZERO;

            if (c.getPodvorotMmHorizontal() != null && c.getPodvorotMmHorizontal().compareTo(BigDecimal.ZERO) > 0) {
                extraW = c.getPodvorotMmHorizontal()
                        .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(c.getPodvorotCountPerSide() * 2));
            }
            if (c.getPodvorotMmVertical() != null && c.getPodvorotMmVertical().compareTo(BigDecimal.ZERO) > 0) {
                extraH = c.getPodvorotMmVertical()
                        .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(c.getPodvorotCountPerSide() * 2));
            }
            width = width.add(extraW);
            height = height.add(extraH);
        }

        return width.multiply(height);
    }

    private BigDecimal calculateOperationQuantity(Calculation c, Operation op) {
        UnitType unit = op.getUnit();
        String name = op.getName().toLowerCase();

        // Операция "подворот": если заданы параметры подворота, вычисляем количество по формуле
        if (op.getHemWidthMm() != null && op.getHemCount() != null) {
            // Переводим размеры в миллиметры
            BigDecimal widthMm = c.getWidthM().multiply(BigDecimal.valueOf(1000));
            BigDecimal heightMm = c.getHeightM().multiply(BigDecimal.valueOf(1000));
            // Добавка по каждому измерению = hemWidthMm * hemCount
            BigDecimal added = BigDecimal.valueOf(op.getHemWidthMm() * op.getHemCount());
            BigDecimal newWidth = widthMm.add(added);
            BigDecimal newHeight = heightMm.add(added);
            // Площадь в мм²
            BigDecimal areaMm2 = newWidth.multiply(newHeight);
            // Переводим в м²
            return areaMm2.divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP);
        }

        switch (unit) {
            case SQUARE_METER:
                // Для всех операций, измеряемых в квадратных метрах (печать, ламинация, аппликация)
                return calculateMaterialArea(c);
            case LINEAR_METER:
                // Для всех операций, измеряемых в погонных метрах (резка, подворот, сварка)
                return perimeter(c);
            case PIECE:
                // Обычно — установка люверсов
                if (name.contains("люверс") || name.contains("установка")) {
                    return calculateEyeletsQuantity(c);
                }
                // Для прочих операций "по штукам" обычно quantity = 1
                return BigDecimal.ONE;
            default:
                return BigDecimal.ONE;
        }
    }

    private BigDecimal perimeter(Calculation c) {
        return c.getWidthM().add(c.getHeightM()).multiply(BigDecimal.valueOf(2));
    }

    private BigDecimal calculateEyeletsQuantity(Calculation c) {
        BigDecimal perimeterM = perimeter(c);
        BigDecimal perimeterCm = perimeterM.multiply(BigDecimal.valueOf(100));
        Integer stepCm = c.getEyeletStepCm();
        if (stepCm == null || stepCm <= 0) stepCm = 40;
        return perimeterCm.divide(BigDecimal.valueOf(stepCm), 0, RoundingMode.UP);
    }

    private void validateCalculation(Calculation calc) {
        if (calc.getWidthM() == null || calc.getWidthM().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Ширина должна быть больше нуля");
        }
        if (calc.getHeightM() == null) {
            throw new BadRequestException("Высота должна быть указана");
        }

        if (calc.getPodvorotCountPerSide() != null && calc.getPodvorotCountPerSide() < 1) {
            throw new BadRequestException("Количество подворотов на сторону должно быть больше 0");
        }

        if (calc.getEyeletStepCm() != null && calc.getEyeletStepCm() <= 0) {
            throw new BadRequestException("Шаг люверсов должен быть больше 0");
        }
    }

    /**
     * Заполняет блок с информацией о люверсах в DTO (если люверсы указаны в расчёте).
     * Вычисляет количество, цену за штуку и итог по люверсам.
     */
    private void populateEyeletResultIfPresent(Calculation calc, CalculationResponseDto dto) {
        if (calc.getEyelet() != null) {
            BigDecimal qty = calculateEyeletsQuantity(calc);
            BigDecimal price = calc.getEyelet().getPricePerPiece();
            BigDecimal subtotal = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);

            EyeletResultDto eyeletDto = new EyeletResultDto();
            eyeletDto.setName(calc.getEyelet().getName());
            eyeletDto.setQuantity(qty);
            eyeletDto.setPricePerUnit(price);
            eyeletDto.setSubtotal(subtotal);

            dto.setEyelet(eyeletDto);
            dto.setEyeletCost(subtotal);
        }
    }
}
