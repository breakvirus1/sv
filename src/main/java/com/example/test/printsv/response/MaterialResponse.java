package com.example.test.printsv.response;



import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaterialResponse {
    private String materialName;
    private Double price;
}
