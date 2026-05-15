package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request для создания позиции заказа в виде материала.
 * Используется при создании заказа для указания материала, его количества и срока готовности.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterialCreateRequest {
    /** ID записи материала в заказе (null при создании) */
    private Long id;
    /** ID материала из справочника */
    private Long materialId;
    /** Ширина в метрах (для расчета площади) */
    private BigDecimal widthM;
    /** Высота в метрах (для расчета площади) */
    private BigDecimal heightM;
    /** Список операций для позиции */
    private List<OrderOperationRequest> operations;
    /** Срок готовности позиции (опционально) */
    private LocalDate readyDate;
    /** ID люверса (опционально, если операция "Установка люверсов") */
    private Long eyeletId;
  /** Шаг установки люверсов в см (опционально) */
  private Integer eyeletStepCm;
  /** Припуск по горизонтали в миллиметрах (опционально, для подворотов) */
  private BigDecimal podvorotMmHorizontal;
  /** Припуск по вертикали в миллиметрах (опционально, для подворотов) */
  private BigDecimal podvorotMmVertical;
  /** Количество подворотов на сторону (опционально, по умолчанию 2) */
  private Integer podvorotCountPerSide;
}
