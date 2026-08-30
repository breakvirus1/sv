package com.example.statisticservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticOrderItemResponse {
    private Long id;
    private Long orderItemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal cost;
    private LocalDate readyDate;
    private List<MaterialConsumptionResponse> materials;
}
