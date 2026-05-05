package com.example.orderservice.product.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductMaterialDTO {
    private Long id;
    private Long materialId;
    private String materialName;
    private BigDecimal quantity;
    private BigDecimal wasteCoefficient;
    private Integer sortOrder;
    private BigDecimal price;
    private String unit;
}
