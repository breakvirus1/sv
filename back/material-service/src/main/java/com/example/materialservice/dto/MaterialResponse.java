package com.example.materialservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialResponse {
    private Long id;
    private String name;
    private String unit;
    private BigDecimal price;
    private BigDecimal wasteCoefficient;
    /** Ширина по умолчанию в метрах */
    private BigDecimal defaultWidthM;
    /** Высота по умолчанию в метрах */
    private BigDecimal defaultHeightM;
}
