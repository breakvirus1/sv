package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZakazDto {
    private Long id;
    private Double sum;
    private LocalDateTime createdAt;
    private Long userId;
    private List<SubZakazDto> subZakazList;
}
