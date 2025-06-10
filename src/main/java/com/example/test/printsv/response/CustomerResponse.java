package com.example.test.printsv.response;




import java.util.List;

import com.example.test.printsv.entity.Zakaz;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class CustomerResponse {
    

    @NonNull    
    private String name;

    private String phone;
    private Zakaz listOfZakaz;

}