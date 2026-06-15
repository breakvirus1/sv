package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterialCreateRequest {
    private Long id;
    private Long materialId;
    /** Ширина в метрах */
    private BigDecimal widthM;
    /** Высота в метрах */
    private BigDecimal heightM;
    private List<OrderOperationRequest> operations;
    private LocalDate readyDate;
    private Long eyeletId;
    private Integer eyeletStepCm;
    private BigDecimal podvorotMmHorizontal;
    private BigDecimal podvorotMmVertical;
    private Integer podvorotCountPerSide;
}
