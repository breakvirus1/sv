package com.example.test.printsv.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZakazResponse {
    private Long id;
    private Integer sum;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
}