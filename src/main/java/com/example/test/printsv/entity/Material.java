package com.example.test.printsv.entity;

import jakarta.persistence.*;

import lombok.*;
@Entity
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "material")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Double price;
    @Column(nullable = false, name="name")
    private String name;
}
