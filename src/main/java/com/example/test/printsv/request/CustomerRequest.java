package com.example.test.printsv.request;




import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class CustomerRequest {
    

    @NonNull    
    private String email;
    private String phone;
}