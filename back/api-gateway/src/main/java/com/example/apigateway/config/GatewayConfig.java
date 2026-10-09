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
                .uri("lb://order-service")
            )
            .route("workshop-service", r -> r
                .path("/api/v1/workshops/**")
                .uri("lb://workshop-service")
            )
            .route("client-service", r -> r
                .path("/api/v1/clients/**", "/api/v1/companies/**")
                .uri("lb://client-service")
            )
            .route("employee-service", r -> r
                .path("/api/v1/employees/**")
                .uri("lb://employee-service")
            )
            .route("material-service", r -> r
                .path("/api/v1/materials/**")
                .uri("lb://material-service")
            )
            .route("product-service", r -> r
                .path("/api/v1/products/**")
                .uri("lb://product-service")
            )
            .route("calculator-service", r -> r
                .path("/api/v1/calculations/**")
                .uri("lb://calculator-service")
            )
            // Admin endpoints
            .route("admin-client-service", r -> r
                .path("/api/v1/admin/clients/**")
                .uri("lb://client-service")
            )
            .route("admin-employee-service", r -> r
                .path("/api/v1/admin/employees/**")
                .uri("lb://employee-service")
            )
             .route("admin-material-operation-groups", r -> r
                  .path("/api/v1/admin/materials/*/operation-groups/**")
                  .uri("lb://calculator-service")
              )
              .route("admin-material-with-operations", r -> r
                  .path("/api/v1/admin/materials-with-operations")
                  .uri("lb://calculator-service")
              )
               .route("admin-material-service", r -> r
                   .path("/api/v1/admin/materials/**")
                   .uri("lb://material-service")
               )
            .route("admin-calculator-service", r -> r
                .path("/api/v1/admin/operations/**", "/api/v1/admin/operation-groups/**")
                .uri("lb://calculator-service")
            )
            .route("admin-order-service", r -> r
                .path("/api/v1/admin/orders/**")
                .uri("lb://order-service")
            )
            .route("admin-product-service", r -> r
                .path("/api/v1/admin/products/**")
                .uri("lb://product-service")
            )
            .route("admin-generate-data-service", r -> r
                  .path("/api/v1/admin/generate/**")
                  .uri("lb://generate-data-service")
              )
             .route("comment-service", r -> r
                  .path("/api/v1/comments/**", "/api/v1/comment-replies/**", "/api/v1/notifications/**", "/api/v1/images/**")
                  .uri("lb://comment-service")
              )
             .route("statistic-service", r -> r
                  .path("/api/v1/admin/statistics/**")
                  .uri("lb://statistic-service")
              )
            .route("file-service", r -> r
                .path("/api/files/**")
                .uri("lb://file-service")
            )
            .build();
        log.info("Custom routes configured for all services via service discovery");
        return routes;
    }
}
