package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderManagerEarningsResponse {
    private Long orderId;
    private String orderNumber;
    private Long managerId;
    private String managerName;
    private BigDecimal managerCashPercent;
    private BigDecimal totalWithPriceplus;
    private BigDecimal managerEarnings;
}
