package com.example.materialservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO для материала (номенклатура).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialResponse {
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
    /** Ширина по умолчанию в миллиметрах */
    private BigDecimal defaultWidthMm;
    /** Высота по умолчанию в миллиметрах */
    private BigDecimal defaultHeightMm;
}
