package com.example.materialservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EntityScan("com.example.common.entity")
@EnableJpaAuditing
public class MaterialServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaterialServiceApplication.class, args);
    }
}
