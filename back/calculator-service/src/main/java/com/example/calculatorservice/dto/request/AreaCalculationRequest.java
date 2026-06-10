package com.example.calculatorservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for real-time area calculation with podvorot.
 * Not persisted, just for calculation endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AreaCalculationRequest {

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
}
