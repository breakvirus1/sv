package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для цеха (мастерской).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkshopDto {
    /** ID цеха */
    private Long id;
    /** Наименование цеха */
    private String name;
    /** Порядок сортировки (для отображения в последовательности) */
    private Integer sortOrder;
}
