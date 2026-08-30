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
public class StatisticOrderResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private LocalDate orderDate;
    private Long clientId;
    private String clientName;
    private BigDecimal totalAmount;
    private BigDecimal priceplus;
    private BigDecimal totalWithPriceplus;
    private String status;
    private String productionStage;
    private Long managerId;
    private String managerName;
    private LocalDateTime syncedAt;
    private List<StatisticOrderItemResponse> items;
}
