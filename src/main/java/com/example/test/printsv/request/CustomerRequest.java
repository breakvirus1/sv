package com.example.test.printsv.request;




import com.example.test.printsv.entity.Zakaz;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class CustomerRequest {
    

    @NonNull    
    private String name;
    private String phone;
}