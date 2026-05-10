package com.example.calculatorservice.mapper;

import com.example.calculatorservice.entity.Calculation;
import com.example.calculatorservice.entity.CalculationOperation;
import com.example.calculatorservice.entity.Eyelet;
import com.example.calculatorservice.dto.response.CalculationResponseDto;
import com.example.calculatorservice.dto.response.EyeletResultDto;
import com.example.calculatorservice.dto.response.OperationResultDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.AfterMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring")
public interface CalculationMapper {

    @Mapping(target = "materialName", source = "material.name")
    @Mapping(target = "operations", source = "selectedOperations")
    @Mapping(target = "eyelet", ignore = true) // will be set manually
    CalculationResponseDto toResponseDto(Calculation calculation);

    @Mapping(target = "operationName", source = "operation.name")
    @Mapping(target = "unit", source = "operation.unit.displayName")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "pricePerUnit", source = "pricePerUnit")
    @Mapping(target = "subtotal", source = "subtotal")
    OperationResultDto toOperationResult(CalculationOperation op);

    // Helper to compute eyelet quantity
    @Named("computeEyeletQuantity")
    default BigDecimal computeEyeletQuantity(Calculation calc) {
        if (calc.getEyelet() == null) return BigDecimal.ZERO;
        BigDecimal perimeter = calc.getWidthM().add(calc.getHeightM()).multiply(BigDecimal.valueOf(2));
        BigDecimal perimeterCm = perimeter.multiply(BigDecimal.valueOf(100));
        Integer step = calc.getEyeletStepCm();
        if (step == null || step <= 0) step = 40;
        return perimeterCm.divide(BigDecimal.valueOf(step), 0, RoundingMode.UP);
    }

    // Helper to compute eyelet subtotal
    @Named("computeEyeletSubtotal")
    default BigDecimal computeEyeletSubtotal(Calculation calc) {
        BigDecimal qty = computeEyeletQuantity(calc);
        Eyelet eyelet = calc.getEyelet();
        if (eyelet == null || eyelet.getPricePerPiece() == null) return BigDecimal.ZERO;
        return qty.multiply(eyelet.getPricePerPiece());
    }

    // After mapping to set eyelet breakdown
    @AfterMapping
    default void setEyelet(Calculation calculation, CalculationResponseDto dto) {
        if (calculation.getEyelet() != null) {
            Eyelet eyelet = calculation.getEyelet();
            BigDecimal qty = computeEyeletQuantity(calculation);
            BigDecimal price = eyelet.getPricePerPiece();
            BigDecimal subtotal = computeEyeletSubtotal(calculation);
            EyeletResultDto eyeletDto = new EyeletResultDto();
            eyeletDto.setName(eyelet.getName());
            eyeletDto.setQuantity(qty);
            eyeletDto.setPricePerUnit(price);
            eyeletDto.setSubtotal(subtotal);
            dto.setEyelet(eyeletDto);
        }
    }
}
