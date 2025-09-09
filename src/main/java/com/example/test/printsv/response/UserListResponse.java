package com.example.test.printsv.response;


import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.enums.Role;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Builder
@Data
@Getter
@Setter
@NoArgsConstructor
public class UserListResponse {


    private Role role;
    private String name;
    private List<Zakaz> listOfZakaz;

}
