package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubZakazDto {
    private Long id;
    private Long materialId;
    private String materialName;
    private Double width;
    private Double height;
    private Double cena;
    private Double total;
    private Long zakazId;
}
