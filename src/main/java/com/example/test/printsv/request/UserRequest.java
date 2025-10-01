package com.example.test.printsv.request;



import org.springframework.stereotype.Component;


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
    private Set<String> roles;

}
