package com.example.test.printsv.response;



import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaterialResponse {
    private Long id;
    private String name;
    private Double price;
}
