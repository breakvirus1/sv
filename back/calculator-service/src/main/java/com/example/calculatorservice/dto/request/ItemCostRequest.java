package com.example.calculatorservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for real-time item cost estimation with podvorot and operations.
 * Endpoint: POST /api/v1/calculations/estimate-item
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCostRequest {

    @NotNull(message = "Материал обязателен")
    private Long materialId;

    @NotNull(message = "Ширина обязательна")
    private BigDecimal widthM;

    @NotNull(message = "Высота обязательна")
    private BigDecimal heightM;

    /**
     * Horizontal podvorot in mm (per side)
     */
    private BigDecimal podvorotMmHorizontal;

    /**
     * Vertical podvorot in mm (per side)
     */
    private BigDecimal podvorotMmVertical;

    /**
     * Number of folds per side (default = 2)
     */
    private Integer podvorotCountPerSide = 2;

    /**
     * Selected operation IDs to include in cost calculation
     */
    private List<Long> operationIds;
}
