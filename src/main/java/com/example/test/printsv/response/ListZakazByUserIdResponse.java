package com.example.test.printsv.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListZakazByUserIdResponse {
    private Long userId;
    private String username;
    private List<ZakazResponse> zakazList;
    private LocalDateTime createdAt;
    private Integer sum;
    private Long id;
}
