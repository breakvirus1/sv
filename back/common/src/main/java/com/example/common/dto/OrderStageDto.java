package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO для этапа производства.
 * Связывает заказ с конкретным цехом и описывает ход выполнения.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStageDto {
    /** ID этапа */
    private Long id;
    /** Цех, выполняющий этап */
    private WorkshopDto workshop;
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
