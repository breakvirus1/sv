package com.example.generatedataservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "clients", schema = "svschema")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Client extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private ClientType type = ClientType.PRIVATE;

    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String inn;

    @Column(length = 255)
    private String address;

    @Column(name = "priceplus", precision = 10, scale = 2)
    private BigDecimal priceplus;
}
