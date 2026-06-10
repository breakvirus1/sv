package com.example.calculatorservice.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CalculationResponseDto {
    private Long id;
    private String materialName;
    private BigDecimal widthM;
    private BigDecimal heightM;
    private BigDecimal totalPrice;
    private List<OperationResultDto> operations;
    private LocalDateTime createdAt;

    // Eyelet breakdown (optional)
    private EyeletResultDto eyelet;

    /** Computed eyelet hardware cost (= quantity * pricePerPiece) */
    private BigDecimal eyeletCost;
}
