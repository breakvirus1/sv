package com.example.materialservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO для расчёта количества операции.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationCalculateRequest {
    private BigDecimal width;
    private BigDecimal height;
    private Integer itemCount;
    private Map<String, Object> parameters;
}
