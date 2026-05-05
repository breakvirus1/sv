package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO для создания операции в позиции заказа (материала).
 * Ссылается на шаблон MaterialOperation и содержит параметры.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderOperationCreateRequest {
    /** ID шаблона операции материала */
    private Long materialOperationId;

    /** Количество операции (например, площадь для печати, погонные метры) */
    private BigDecimal quantity = BigDecimal.ONE;

    /** Коэффициент отходов (может переопределять основной у материала) */
    private BigDecimal wasteCoefficient;

    /** Дополнительные параметры операции в формате ключ-значение */
    private Map<String, Object> parameters;

    /** Дополнительные материалы, используемые в операции (материалId -> кол-во) */
    private Map<Long, BigDecimal> additionalMaterials;
}
