package com.example.generatedataservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employees", schema = "ordschema")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Employee extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "username", length = 100, unique = true)
    private String username;

    @Column(name = "position", length = 100)
    private String position;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "workshop_id")
    private Long workshopId;
}
