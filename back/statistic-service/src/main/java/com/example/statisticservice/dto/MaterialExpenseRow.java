package com.example.statisticservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialExpenseRow {
    private String materialName;
    private BigDecimal netQuantity;
    private BigDecimal consumptionWithPriceplus;
    private BigDecimal cost;
    private BigDecimal consumptionFromOrderPositions;
}
