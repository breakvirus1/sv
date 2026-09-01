package com.example.statisticservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialExpenseRow {
    private String type;
    private String name;
    private String materialName;
    private String unit;
    private BigDecimal netQuantity;
    private BigDecimal quantityWithWaste;
    private BigDecimal pieces;
    private BigDecimal linearMeters;
    private BigDecimal squareMeters;
    private BigDecimal eyeletPieces;
    private BigDecimal consumptionWithPriceplus;
    private BigDecimal cost;
    private BigDecimal consumptionFromOrderPositions;
    private Long operationCount;
    private BigDecimal operationsTotalCost;
}
