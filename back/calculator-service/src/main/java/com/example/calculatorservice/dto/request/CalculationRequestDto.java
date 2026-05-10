package com.example.calculatorservice.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CalculationRequestDto {
    private Long materialId;
    private String materialType; // "BANNER" or "PLENKA"

    private BigDecimal widthM;
    private BigDecimal heightM;
    private Integer dpi;

    // Подворот parameters (only for banner)
    private BigDecimal podvorotMmHorizontal;
    private BigDecimal podvorotMmVertical;
    private Integer podvorotCountPerSide = 2;

    // Eyelet parameters
    private Long eyeletId;
    private Integer eyeletStepCm = 40;

    // Selected operation IDs
    private List<Long> operationIds;
}
