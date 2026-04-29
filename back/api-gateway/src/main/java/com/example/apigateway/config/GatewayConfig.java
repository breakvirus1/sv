package com.example.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayConfig.class);

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Configuring custom routes for API Gateway");
        RouteLocator routes = builder.routes()
            .route("order-service", r -> r
                .path("/api/v1/orders/**")
                .uri("http://order-service:8081")
            )
            .route("client-service", r -> r
                .path("/api/v1/clients/**")
                .uri("http://client-service:8082")
            )
            .route("employee-service", r -> r
                .path("/api/v1/employees/**")
                .uri("http://employee-service:8083")
            )
            .route("material-service", r -> r
                .path("/api/v1/materials/**")
                .uri("http://material-service:8084")
            )
            // Admin endpoints
            .route("admin-client-service", r -> r
                .path("/api/v1/admin/clients/**")
                .uri("http://client-service:8082")
            )
            .route("admin-employee-service", r -> r
                .path("/api/v1/admin/employees/**")
                .uri("http://employee-service:8083")
            )
            .route("admin-material-service", r -> r
                .path("/api/v1/admin/materials/**")
                .uri("http://material-service:8084")
            )
            .route("admin-order-service", r -> r
                .path("/api/v1/admin/orders/**")
                .uri("http://order-service:8081")
            )
            .build();
        log.info("Custom routes configured: order-service, client-service, employee-service, material-service, and admin endpoints");
        return routes;
    }
}
