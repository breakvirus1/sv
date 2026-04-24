package com.example.zakazservice.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZakazResponse {
    private Long id;
    private Double sum;
    private LocalDateTime createdAt;
}
