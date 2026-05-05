package com.example.materialservice.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO для создания нового материала.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialCreateRequest {
    /** Наименование материала или операции */
    private String name;
    /** Единица измерения (м2, шт, п.м. и т.д.) */
    private String unit;
    /** Цена за единицу */
    private BigDecimal price;
    /** Коэффициент отхода */
    private BigDecimal wasteCoefficient;
    /** Список операций (справочник работ) для материала */
    @Valid
    private List<MaterialOperationCreateRequest> operations;
}
