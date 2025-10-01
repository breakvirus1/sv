package com.example.test.printsv.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data

public class ZakazResponse {
    private Long id;
    private Integer sum;
    private Long userOfZakazId;
    private Long customerOfZakazId;

}