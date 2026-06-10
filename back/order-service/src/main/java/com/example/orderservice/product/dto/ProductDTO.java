package com.example.orderservice.product.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String article;
    private String description;
    private BigDecimal width;
    private BigDecimal height;
    private String unit;
    private BigDecimal basePrice;
    private String category;
    private Boolean isActive;
    private String formulaJson;
    private List<ProductMaterialDTO> materials;
    private List<ProductOperationDTO> operations;
}
