package com.example.calculatorservice.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EyeletResultDto {
    private String name;
    private BigDecimal quantity; // number of eyelets
    private BigDecimal pricePerUnit;
    private BigDecimal subtotal;
}
