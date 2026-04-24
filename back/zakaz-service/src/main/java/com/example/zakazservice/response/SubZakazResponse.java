package com.example.zakazservice.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubZakazResponse {
    private Long id;
    private String materialName;
    private Double width;
    private Double height;
    private Double cena;
    private Double price;
}
