package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO для этапа производства.
 * Связывает заказ с конкретным цехом и описывает ход выполнения.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStageResponse {
    /** ID этапа */
    private Long id;
    /** Цех, выполняющий этап */
    private WorkshopResponse workshop;
    /** Ждать завершения предыдущих цехов? */
    private Boolean waitPrevious;
    /** Планируемый срок выполнения этапа */
    private LocalDate dueDate;
    /** Примечание к этапу */
    private String note;
    /** Статус: TODO, IN_PROGRESS, DONE */
    private String status;
    /** Исходные файлы для этапа (пути или JSON с метаданными) */
    private String sourceFiles;
}
