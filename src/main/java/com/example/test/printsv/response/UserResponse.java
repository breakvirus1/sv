package com.example.test.printsv.response;


import java.util.List;

import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.enums.Role;

import lombok.*;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class UserResponse {


    
    private Role role;
    private String name;
    private String password;
    private List<Zakaz> listOfZakaz;

}
