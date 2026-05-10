package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request для операции в позиции заказа.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderOperationRequest {
    /** ID операции из calculator-service */
    private Long operationId;
}