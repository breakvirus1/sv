package com.example.calculatorservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EyeletDto {
    private Long id;
    private String name;
    private BigDecimal pricePerPiece;
    private Integer diameterMm;
}
