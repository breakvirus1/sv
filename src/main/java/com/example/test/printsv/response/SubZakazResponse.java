package com.example.test.printsv.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubZakazResponse {
    private Long id;
    private String materialName;
    private Double width;
    private Double height;
    private Double cena;
    private Double total;
}