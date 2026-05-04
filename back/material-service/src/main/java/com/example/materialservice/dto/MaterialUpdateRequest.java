package com.example.materialservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO для обновления материала.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialUpdateRequest {
    /** Наименование материала или операции */
    private String name;
    /** Единица измерения (м2, шт, п.м. и т.д.) */
    private String unit;
    /** Цена за единицу */
    private BigDecimal price;
    /** Коэффициент отхода */
    private BigDecimal wasteCoefficient;
}
