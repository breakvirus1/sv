package com.example.orderservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemOperationDTO {
    private Long id;
    private String name;
    private BigDecimal pricePerUnit;
    private String normTime;
    private BigDecimal quantity;
    private BigDecimal cost;
}
