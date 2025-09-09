package com.example.test.printsv.response;

import com.example.test.printsv.enums.Role;
import lombok.*;

import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;

    private Set<Role> roles;

}
