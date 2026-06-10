package com.example.materialservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialResponse {
    private Long id;
    private String name;
    private String unit;
    private BigDecimal price;
    private BigDecimal wasteCoefficient;
    /** Список операций, связанных с материалом */
    private List<MaterialOperationResponse> operations;
}
