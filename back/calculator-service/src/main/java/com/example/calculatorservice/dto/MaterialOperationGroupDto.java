package com.example.calculatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialOperationGroupDto {
    private Long id;
    private Long materialId;
    private OperationGroupDto group;
    private OperationDto operation;
}
