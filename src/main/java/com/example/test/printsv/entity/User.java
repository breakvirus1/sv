package com.example.test.printsv.entity;

import jakarta.persistence.*;

import java.util.List;

import com.example.test.printsv.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String password;
    @ManyToOne
    private List<Zakaz> listOfZakaz;

}
