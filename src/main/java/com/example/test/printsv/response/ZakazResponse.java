package com.example.test.printsv.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ZakazResponse {
    private Long id;
    private Integer sum;
    private Long userId;
    private Long customerId;
    private List<SubZakazResponse> subZakaz;
}