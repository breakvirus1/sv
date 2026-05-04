package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO для создания/обновления этапа производства.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStageRequest {
    /** ID цеха */
    private Long workshopId;
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
