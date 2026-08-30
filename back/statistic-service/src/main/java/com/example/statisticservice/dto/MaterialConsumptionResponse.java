package com.example.statisticservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialConsumptionResponse {
    private Long id;
    private Long materialId;
    private String materialName;
    private String materialUnit;
    private BigDecimal materialPrice;
    private BigDecimal wasteCoefficient;
    private BigDecimal quantity;
    private BigDecimal widthM;
    private BigDecimal heightM;
    private BigDecimal netQuantity;
    private BigDecimal cost;
    private BigDecimal costPriceplus;
    private BigDecimal eyeletCost;
    private BigDecimal orderClientPriceplus;
    private BigDecimal consumptionWithPriceplus;
    private LocalDateTime syncedAt;
}
