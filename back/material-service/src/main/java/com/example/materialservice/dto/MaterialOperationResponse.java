package com.example.materialservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO для операции материала (этап работ).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialOperationResponse {
    private Long id;
    private Long materialId;
    private String materialName;
    private String name;
    private String description;
    private String operationType;
    private BigDecimal basePrice;
    private String unit;
    private BigDecimal wasteCoefficient;
    private Boolean requiresDimensions;
    private Boolean allowsAdditionalMaterials;
    private Integer sortOrder;
    private Boolean active;
    private List<OperationParameterDto> parameters;
    private List<AdditionalMaterialDto> additionalMaterials;
    private String quantityFormula;
}
