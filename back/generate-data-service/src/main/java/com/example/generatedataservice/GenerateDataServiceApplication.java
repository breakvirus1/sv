package com.example.generatedataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GenerateDataServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GenerateDataServiceApplication.class, args);
    }
}
