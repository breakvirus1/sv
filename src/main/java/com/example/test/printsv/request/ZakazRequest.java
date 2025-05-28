package com.example.test.printsv.request;

import java.util.List;

import com.example.test.printsv.entity.*;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ZakazRequest {
    private List<SubZakaz> subZakazList;
    private Customer customer;
    private User user;
    private Integer sum;
    
    
}
