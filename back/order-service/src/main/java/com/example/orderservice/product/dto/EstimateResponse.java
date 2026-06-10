package com.example.orderservice.product.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class EstimateResponse {
    private BigDecimal totalMaterials;
    private BigDecimal totalOperations;
    private BigDecimal grandTotal;
    private List<ProductMaterialDTO> materials;
    private List<ProductOperationDTO> operations;
}
