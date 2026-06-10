package com.example.materialservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO для создания этапа операции материала.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialOperationCreateRequest {
    @NotBlank(message = "Название операции обязательно")
    private String name;

    private String description;

    @NotNull(message = "Тип операции обязателен")
    private String operationType;

    @NotNull(message = "Базовая цена обязательна")
    private BigDecimal basePrice;

    private String unit = "шт";

    private BigDecimal wasteCoefficient = BigDecimal.ONE;

    private Boolean requiresDimensions = false;

    private Boolean allowsAdditionalMaterials = false;

    private String quantityFormula;

    private List<OperationParameterDto> parameters;

    private List<AdditionalMaterialDto> additionalMaterials;

    private Integer sortOrder = 0;

    private Boolean active = true;
}
