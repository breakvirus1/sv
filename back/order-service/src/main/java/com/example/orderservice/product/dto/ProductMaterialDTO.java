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
    private String quantityFormula;  // e.g. "width * height * 1.1"
    private BigDecimal quantity;     // base quantity (if formula is null)
    private BigDecimal wasteCoefficient;
    private Integer sortOrder;
    private BigDecimal price;
    private String unit;
}
