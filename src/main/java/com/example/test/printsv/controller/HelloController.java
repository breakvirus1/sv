package com.example.test.printsv.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        String name =  SecurityContextHolder.getContext().getAuthentication().getName();
        return "Hello from the secured backend, "+name;
    }
}
