package com.example.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEstimateDTO {
    private Long orderItemId;
    private Long productId;
    private List<OrderItemMaterialDTO> materials;
    private List<OrderItemOperationDTO> operations;
    private BigDecimal totalMaterials;
    private BigDecimal totalOperations;
    private BigDecimal grandTotal;
}
