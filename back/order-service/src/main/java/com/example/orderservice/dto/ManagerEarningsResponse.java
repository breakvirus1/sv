package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для статистики заработка менеджера.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManagerEarningsResponse {
    /** ID менеджера */
    private Long managerId;
    /** ФИО менеджера */
    private String managerName;
    /** Процент заработка менеджера */
    private BigDecimal managerCashPercent;
    /** Сумма заработка с заказов со статусом READY (уже начислено) */
    private BigDecimal readyEarnings;
    /** Сумма заработка с заказов со статусом IN_PROGRESS (потенциальный заработок) */
    private BigDecimal inProgressEarnings;
    /** Количество заказов со статусом READY */
    private int readyOrdersCount;
    /** Количество заказов со статусом IN_PROGRESS */
    private int inProgressOrdersCount;
}
