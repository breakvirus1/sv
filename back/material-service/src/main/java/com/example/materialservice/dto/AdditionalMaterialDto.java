package com.example.materialservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для дополнительного материала в операции.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdditionalMaterialDto {
    private Long materialId;

    private String materialName;

    private BigDecimal defaultQuantity = BigDecimal.ONE;

    private String unit = "шт";

    private BigDecimal pricePerUnit;
}
