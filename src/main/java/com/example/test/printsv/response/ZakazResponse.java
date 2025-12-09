package com.example.test.printsv.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.test.printsv.entity.SubZakaz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZakazResponse {
    private Long id;
    private Integer sum;
    private LocalDateTime createdAt;
    // private List<SubZakaz> subZakazList;
}