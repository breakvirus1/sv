package com.example.test.printsv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String phone;

    @OneToMany(mappedBy = "customerOfZakaz", cascade = CascadeType.ALL)
    private List<Zakaz> zakazList = new ArrayList<>();
}