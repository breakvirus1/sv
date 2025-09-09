package com.example.test.printsv.response;

import lombok.Data;

@Data

public class ZakazListResponse {
    private Long id;
    private Integer sum;
    private String userOfZakazId;
    private String customerNameOfZakaz;

}