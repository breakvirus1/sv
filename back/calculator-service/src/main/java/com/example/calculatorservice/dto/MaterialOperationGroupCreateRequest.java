package com.example.calculatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialOperationGroupCreateRequest {
    private Long materialId;
    private Long operationGroupId;
    private Long operationId;
}
