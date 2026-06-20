package com.example.calculatorservice.mapper;

import com.example.calculatorservice.entity.Calculation;
import com.example.calculatorservice.entity.CalculationOperation;
import com.example.calculatorservice.dto.response.CalculationResponseDto;
import com.example.calculatorservice.dto.response.OperationResultDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CalculationMapper {

    @Mapping(target = "materialName", source = "material.name")
    @Mapping(target = "operations", source = "selectedOperations")
    @Mapping(target = "eyelet", ignore = true)
    CalculationResponseDto toResponseDto(Calculation calculation);

    @Mapping(target = "operationId", source = "operation.id")
    @Mapping(target = "operationName", source = "operation.name")
    @Mapping(target = "unit", source = "operation", qualifiedByName = "operationUnit")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "pricePerUnit", source = "pricePerUnit")
    @Mapping(target = "subtotal", source = "subtotal")
    OperationResultDto toOperationResult(CalculationOperation op);

    @Named("operationUnit")
    static String operationUnit(com.example.calculatorservice.entity.Operation operation) {
        if (operation == null || operation.getUnit() == null) {
            return null;
        }
        return operation.getUnit().getDisplayName();
    }
}
