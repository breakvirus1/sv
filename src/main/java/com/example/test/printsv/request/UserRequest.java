package com.example.test.printsv.request;


import com.example.test.printsv.enums.Role;
import lombok.*;


@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class UserRequest {
    @NonNull
    private Role role;
    @NonNull
    private String name;
    @NonNull
    private String password;

}
