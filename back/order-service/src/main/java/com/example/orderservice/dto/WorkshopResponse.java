package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO для цеха (мастерской).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkshopResponse {
    /** ID цеха */
    private Long id;
    /** Наименование цеха */
    private String name;
    /** Порядок сортировки (для отображения в последовательности) */
    private Integer sortOrder;
}
