package com.example.materialservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для динамического параметра операции.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationParameterDto {
    private Long id;

    private String paramKey;

    private String displayName;

    private String type;

    private String unit;

    private String defaultValue;

    private Boolean required = false;

    private Integer sortOrder = 0;
}
