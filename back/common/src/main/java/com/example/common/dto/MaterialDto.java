package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для материала (номенклатура).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialDto {
    /** ID материала */
    private Long id;
    /** Наименование материала или операции */
    private String name;
    /** Единица измерения (м2, шт, п.м. и т.д.) */
    private String unit;
    /** Цена за единицу */
    private BigDecimal price;
    /** Коэффициент отхода */
    private BigDecimal wasteCoefficient;
}