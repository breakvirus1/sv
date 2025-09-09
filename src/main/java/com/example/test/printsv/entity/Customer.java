package com.example.test.printsv.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public void addZakaz(Zakaz zakaz){
        zakazList.add(zakaz);
    }

    public void removeZakaz(Zakaz zakaz){
        zakazList.remove(zakaz);
        zakaz.setCustomerOfZakaz(null);
    }


}