package com.example.orderservice.dto;

import com.example.orderservice.product.dto.ProductMaterialDTO;
import com.example.orderservice.product.dto.ProductOperationDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductEstimateDTO {
    private Long productId;
    private List<ProductMaterialDTO> materials;
    private List<ProductOperationDTO> operations;
    private BigDecimal totalMaterials;
    private BigDecimal totalOperations;
    private BigDecimal grandTotal;
}
