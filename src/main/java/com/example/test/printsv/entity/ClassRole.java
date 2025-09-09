package com.example.test.printsv.entity;

import com.example.test.printsv.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "roles")
public class ClassRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role name;

    public ClassRole() {

    }

    public ClassRole(Role name) {
        this.name = name;
    }
}
