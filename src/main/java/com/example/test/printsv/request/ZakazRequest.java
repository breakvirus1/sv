package com.example.test.printsv.request;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
public class ZakazRequest {
    private Integer sum;
    // private Long userOfZakazId;
    // private Long customerOfZakazId;
}