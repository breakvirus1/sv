package com.example.test.printsv.response;




import com.example.test.printsv.entity.Customer;
import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.entity.User;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ZakazResponse {
    private SubZakaz subZakaz;
    private Customer customerOfZakaz;
    private User userOfZakaz;
    private Integer sum;
    
}
