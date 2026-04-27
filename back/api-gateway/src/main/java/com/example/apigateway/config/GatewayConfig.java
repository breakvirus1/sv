package com.example.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("order-service", r -> r
                .path("/api/v1/orders/**")
                .uri("http://localhost:8081")
            )
            .route("client-service", r -> r
                .path("/api/v1/clients/**")
                .uri("http://localhost:8082")
            )
            .build();
    }
}
