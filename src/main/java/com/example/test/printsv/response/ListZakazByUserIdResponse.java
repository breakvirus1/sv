package com.example.test.printsv.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.test.printsv.entity.Zakaz;

import lombok.*;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ListZakazByUserIdResponse {

    private Long id;
    private String username;
    private List<Zakaz> zakazList;

}
