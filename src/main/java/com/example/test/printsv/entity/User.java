package com.example.test.printsv.entity;

import jakarta.persistence.*;

import com.example.test.printsv.enums.Role;
import lombok.Data;


@Entity
@Table(name = "users")
@Data

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String password;

}
