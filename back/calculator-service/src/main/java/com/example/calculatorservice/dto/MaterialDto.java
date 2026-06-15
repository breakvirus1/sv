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

    // Additional getters for frontend compatibility
    public String getUnit() {
        return "м2";
    }

    public BigDecimal getPrice() {
        return pricePerSquareMeter;
    }
}
