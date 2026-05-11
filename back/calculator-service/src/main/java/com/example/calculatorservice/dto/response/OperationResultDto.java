package com.example.calculatorservice.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO for operation breakdown in calculation response.
 */
@Data
public class OperationResultDto {
    private Long operationId;
    private String operationName;
    private BigDecimal quantity;
    private String unit; // м², п.м., шт
    private BigDecimal pricePerUnit;
    private BigDecimal subtotal;
}
