package com.example.test.printsv.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

import com.example.test.printsv.entity.Role;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private Set<String> roles;
}
