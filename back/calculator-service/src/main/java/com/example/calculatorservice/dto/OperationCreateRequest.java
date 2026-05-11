package com.example.calculatorservice.dto;

 import lombok.Data;

 import java.math.BigDecimal;

 import com.example.calculatorservice.entity.ApplicableType;
 import com.example.calculatorservice.entity.UnitType;

@Data
public class OperationCreateRequest {
    private String name;
    private BigDecimal price;
    private UnitType unit;
    private ApplicableType applicableTo;
    private boolean isDefault;
    private Integer hemWidthMm;
    private Integer hemCount;
}
