package com.example.test.printsv.controller;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.security.JwtTokenUtil;
import com.example.test.printsv.security.LoginResponse;
import com.example.test.printsv.service.UserService;

import lombok.AllArgsConstructor;

@RestController

@AllArgsConstructor
@RequestMapping("/user")
public class UserController {

    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private JwtTokenUtil jwtTokenUtil;

    

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        User existingUser = userService.getUserByName(user.getName());
        if (existingUser != null) {
            return "Имя или id занято";
        }
        userService.saveUser(user);
        return "Юзер зареган";
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody User user) {
        User existingUser = userService.getUserByName(user.getName());

        if (existingUser != null && passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            String token = jwtTokenUtil.generateToken(user.getName());
            return new LoginResponse(token, "Успешно залогинился");
        } else {
            return new LoginResponse(null, "fail гдето");
        }
    }
}