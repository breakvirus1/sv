package com.example.calculatorservice.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BANNER")
public class Banner extends Material {
    // No additional fields
}
