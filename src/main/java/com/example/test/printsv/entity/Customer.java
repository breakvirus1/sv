package com.example.test.printsv.entity;


import java.util.List;

import jakarta.persistence.*;

import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;
    
    @JoinColumn(name = "zakaz_id")
    @ManyToOne
    private List<Zakaz> listOfZakaz;
}