package com.example.calculatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialOperationGroupUpdateRequest {
    private Long materialId;
    private List<Long> operationIds;
}
