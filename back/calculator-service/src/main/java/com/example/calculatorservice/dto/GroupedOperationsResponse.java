package com.example.calculatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupedOperationsResponse {
    private List<GroupDto> groups;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroupDto {
        private Long id;
        private String name;
        private List<OperationDto> operations;
    }
}
