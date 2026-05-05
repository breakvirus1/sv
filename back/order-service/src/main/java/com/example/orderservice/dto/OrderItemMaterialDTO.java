package com.example.orderservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemMaterialDTO {
    private Long id;
    private Long materialId;
    private String materialName;
    private BigDecimal quantity;
    private BigDecimal wasteCoefficient;
    private BigDecimal cost;
    private BigDecimal price;
    private String unit;
}
