package com.example.orderservice.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateRequest {
    private Long productId;
    private Long orderItemId;
}
