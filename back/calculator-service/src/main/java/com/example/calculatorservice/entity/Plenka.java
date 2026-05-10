package com.example.calculatorservice.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PLENKA")
public class Plenka extends Material {
    // No additional fields
}
