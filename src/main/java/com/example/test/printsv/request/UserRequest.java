package com.example.test.printsv.request;


import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

import com.example.test.printsv.enums.Role;
import lombok.*;

import java.util.Set;


@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Component
public class UserRequest {
    private Long id;
    private String userName;



    private Set<Role> roles;

}
