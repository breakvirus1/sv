package com.example.test.printsv.request;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaterialRequest {
    @NonNull        
    private String name;
}
