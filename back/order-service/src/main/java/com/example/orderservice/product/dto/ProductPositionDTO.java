package com.example.orderservice.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Позиция изделия: один материал и список операций, применяемых к нему.
 * Сумма позиции единственная и рассчитывается на сервере.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPositionDTO {
    /** ID позиции (product_materials.id) */
    private Long id;
    private Long materialId;
    private String materialName;
    private String unit;
    private BigDecimal price;
    private BigDecimal quantity;
    /** Единственный коэффициент количества материала */
    private BigDecimal wasteCoefficient;
    private String quantityFormula;
    private Integer sortOrder;
    /** Единственная сумма позиции: материал + операции */
    private BigDecimal positionSum;
    private List<ProductOperationDTO> operations = new ArrayList<>();
}
