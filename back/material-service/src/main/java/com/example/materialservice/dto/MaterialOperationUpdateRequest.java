package com.example.materialservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO для обновления этапа операции материала.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialOperationUpdateRequest {
    @NotBlank(message = "Название операции обязательно")
    private String name;

    private String description;

    @NotNull(message = "Тип операции обязателен")
    private String operationType;

    @NotNull(message = "Базовая цена обязательна")
    private BigDecimal basePrice;

    private String unit;

    private BigDecimal wasteCoefficient;

    private Boolean requiresDimensions;

    private Boolean allowsAdditionalMaterials;

    private String quantityFormula;

    private List<OperationParameterDto> parameters;

    private List<AdditionalMaterialDto> additionalMaterials;

    private Integer sortOrder;

    private Boolean active;
}
