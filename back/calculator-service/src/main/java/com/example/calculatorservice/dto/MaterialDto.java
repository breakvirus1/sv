package com.example.calculatorservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MaterialDto {
    private Long id;
    private String name;
    private BigDecimal pricePerSquareMeter;
    private String type; // BANNER or PLENKA
    private BigDecimal wasteCoefficient;
}
