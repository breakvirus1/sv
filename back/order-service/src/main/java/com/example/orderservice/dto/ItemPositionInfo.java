package com.example.orderservice.dto;

import com.example.materialservice.dto.MaterialResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO с информацией об одной позиции заказа (одна строка таблицы позиций).
 * Используется для передачи фронтенду полей для заполнения формы, включая defaults из базы.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemPositionInfo {
    /**
     * ID строки материала в заказе (null для новой позиции)
     */
    private Long id;

    /**
     * Ссылка на материал из справочника (с подтянутыми defaultWidthMm/defaultHeightMm из БД).
     * Поле может быть null если materialId не существует в справочнике.
     */
    private MaterialResponse material;

    /**
     * Ширина в миллиметрах, подтянутая из БД (из заказа или из defaultWidthMm материала).
     * Если в заказе ширина не задана, возвращает defaultWidthMm справочника.
     */
    private BigDecimal widthMm;

    /**
     * Высота в миллиметрах, подтянутая из БД (из заказа или из defaultHeightMm материала).
     * Если в заказе высота не задана, возвращает defaultHeightMm справочника.
     */
    private BigDecimal heightMm;

    private List<OperationInfo> operations;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OperationInfo {
        private Long operationId;
        private String operationName;
        private BigDecimal widthM;
        private BigDecimal heightM;
    }
}
