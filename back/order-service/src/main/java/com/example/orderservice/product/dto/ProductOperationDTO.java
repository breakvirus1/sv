package com.example.orderservice.product.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductOperationDTO {
    private Long id;
    private String name;
    private BigDecimal pricePerUnit;
    private String normTime;
    private String unit;
    private Integer sortOrder;
}
