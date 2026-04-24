package com.example.zakazservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = "com.example.zakazservice.repository")
@EntityScan(basePackages = "com.example.common.entity")
public class ZakazServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZakazServiceApplication.class, args);
    }
}
