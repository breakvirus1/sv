package com.example.test.printsv.request;

import lombok.*;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MaterialRequest {
    @NonNull        
    private String name;
    
}
