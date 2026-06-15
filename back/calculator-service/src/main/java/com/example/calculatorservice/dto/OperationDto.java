package com.example.calculatorservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OperationDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private String unit;
    private String applicableTo;
    private boolean isDefault;
    private Integer hemWidthMm;
    private Integer hemCount;
}
