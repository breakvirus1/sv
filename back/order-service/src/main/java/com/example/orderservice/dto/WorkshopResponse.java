package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkshopResponse {
    private Long id;
    private String name;
    private Integer sortOrder;
    private List<Long> operationIds;
}
