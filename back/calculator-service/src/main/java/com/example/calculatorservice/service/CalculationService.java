package com.example.calculatorservice.service;

import com.example.calculatorservice.entity.*;
import com.example.calculatorservice.dto.request.CalculationRequestDto;
import com.example.calculatorservice.dto.response.CalculationResponseDto;
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
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new BadRequestException("Материал не найден"));

        // Validate material type matches request (basic check)
        if (material instanceof Banner) {
            if (request.getMaterialType() != null && !request.getMaterialType().equals("BANNER")) {
                throw new BadRequestException("Тип материала не совпадает");
            }
        } else if (material instanceof Plenka) {
            if (request.getMaterialType() != null && !request.getMaterialType().equals("PLENKA")) {
                throw new BadRequestException("Тип материала не совпадает");
            }
        } else {
            throw new BadRequestException("Неподдерживаемый тип материала");
        }

        Calculation calc = new Calculation();
        calc.setMaterial(material);
        calc.setWidthM(request.getWidthM());
        calc.setHeightM(request.getHeightM());
        calc.setDpi(request.getDpi());
        calc.setPodvorotMmHorizontal(request.getPodvorotMmHorizontal());
        calc.setPodvorotMmVertical(request.getPodvorotMmVertical());
        calc.setPodvorotCountPerSide(request.getPodvorotCountPerSide());
        calc.setEyeletStepCm(request.getEyeletStepCm());
        calc.setCreatedAt(LocalDateTime.now());

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

        validateCalculation(calc);
        calculateTotalPrice(calc);

        Calculation saved = calculationRepository.save(calc);
        return calculationMapper.toResponseDto(saved);
    }

    public CalculationResponseDto getById(Long id) {
        Calculation calc = calculationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Расчёт не найден"));
        return calculationMapper.toResponseDto(calc);
    }

    private void calculateTotalPrice(Calculation calc) {
        // 1. Material area considering podvorot for banners
        BigDecimal materialArea = calculateMaterialArea(calc);

        // Material cost = area * pricePerSquareMeter * wasteCoefficient
        BigDecimal materialPrice = calc.getMaterial().getPricePerSquareMeter();
        BigDecimal wasteCoeff = calc.getMaterial().getWasteCoefficient();
        if (wasteCoeff == null) wasteCoeff = BigDecimal.ONE;
        BigDecimal materialCost = materialArea.multiply(materialPrice).multiply(wasteCoeff);

        BigDecimal total = materialCost;

        // 2. Operations cost
        for (CalculationOperation calcOp : calc.getSelectedOperations()) {
            Operation op = calcOp.getOperation();
            BigDecimal quantity = calculateOperationQuantity(calc, op);
            calcOp.setQuantity(quantity);
            BigDecimal pricePerUnit = calcOp.getPricePerUnit();
            BigDecimal subtotal = quantity.multiply(pricePerUnit);
            calcOp.setSubtotal(subtotal);
            total = total.add(subtotal);
        }

        // 3. Eyelet hardware cost
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

        if (c.getMaterial() instanceof Banner && c.hasPodvorot()) {
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

        switch (unit) {
            case SQUARE_METER:
                // For all area-based operations (printing, lamination, application)
                return calculateMaterialArea(c);
            case LINEAR_METER:
                // For all linear meter operations (cutting, hem, welding)
                return perimeter(c);
            case PIECE:
                // Typically eyelet installation
                if (name.contains("люверс") || name.contains("установка")) {
                    return calculateEyeletsQuantity(c);
                }
                // other piece-based operations maybe quantity = 1?
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
        if (calc.getWidthM() == null || calc.getHeightM() == null ||
            calc.getWidthM().compareTo(BigDecimal.ZERO) <= 0 ||
            calc.getHeightM().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Размеры должны быть больше нуля");
        }

        // Подворот только для баннера
        if (calc.hasPodvorot() && !(calc.getMaterial() instanceof Banner)) {
            throw new BadRequestException("Подворот доступен только для баннеров");
        }

        // Люверсы только для баннера
        if (calc.getEyelet() != null && !(calc.getMaterial() instanceof Banner)) {
            throw new BadRequestException("Люверсы доступны только для баннеров");
        }

        if (calc.getPodvorotCountPerSide() != null && calc.getPodvorotCountPerSide() < 1) {
            throw new BadRequestException("Количество подворотов на сторону должно быть больше 0");
        }

        if (calc.getEyeletStepCm() != null && calc.getEyeletStepCm() <= 0) {
            throw new BadRequestException("Шаг люверсов должен быть больше 0");
        }
    }
}
