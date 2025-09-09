package com.example.test.printsv.response;




import java.util.List;



import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class CustomerResponse {
    private Long id;

    @NonNull    
    private String name;

    private String phone;


}