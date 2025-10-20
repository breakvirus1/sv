package com.example.test.printsv.response;

import com.example.test.printsv.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ZakazResponse {
    private Long id;
    private Integer sum;
    private User userId;
    private LocalDateTime createdAt;
    // private Long customerOfZakazId

}