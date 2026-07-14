package com.example.calculatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialWithOperationsCreateRequest {
    private String name;
    private String unit;
    private BigDecimal price;
    private BigDecimal wasteCoefficient;
    private List<Long> operationIds;
}
