package com.example.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final WebClient webClient;
    private final String tokenBaseUrl;
    private final String clientId;
    private final String clientSecret;

    public AuthController(WebClient.Builder webClientBuilder,
                          @Value("${app.keycloak.token-base-url:http://keycloak:8080/realms/print-sv}") String tokenBaseUrl,
                          @Value("${app.keycloak.client-id:frontend}") String clientId,
                          @Value("${app.keycloak.client-secret:}") String clientSecret) {
        this.webClient = webClientBuilder
                .baseUrl(tokenBaseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.tokenBaseUrl = tokenBaseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        log.info("AuthController initialized with token-base-url: {}, client-id: {}", tokenBaseUrl, clientId);
    }

    @RequestMapping(method = RequestMethod.OPTIONS, value = {"/login", "/refresh"})
    public Mono<ResponseEntity<Void>> options() {
        return Mono.just(ResponseEntity.ok().build());
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> login(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String username = formData.getFirst("username");
                    String password = formData.getFirst("password");

                    if (username == null || username.isBlank() || password == null || password.isBlank()) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "username and password are required");
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    }

                    log.debug("Login attempt for user: {}", username);
                    return callKeycloakTokenEndpoint(Map.of(
                            "grant_type", "password",
                            "client_id", clientId,
                            "username", username,
                            "password", password
                    ));
                })
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(e -> handleTimeout(e, "login"));
    }

    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> refresh(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String refreshToken = formData.getFirst("refresh_token");

                    if (refreshToken == null || refreshToken.isBlank()) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "refresh_token is required");
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    }

                    log.debug("Token refresh attempt");
                    return callKeycloakTokenEndpoint(Map.of(
                            "grant_type", "refresh_token",
                            "client_id", clientId,
                            "refresh_token", refreshToken
                    ));
                })
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(e -> handleTimeout(e, "refresh"));
    }

    private Mono<ResponseEntity<Map<String, Object>>> callKeycloakTokenEndpoint(Map<String, String> params) {
        var body = BodyInserters.fromFormData("grant_type", params.get("grant_type"))
                .with("client_id", params.get("client_id"));

        if (params.containsKey("username")) {
            body = body.with("username", params.get("username"));
        }
        if (params.containsKey("password")) {
            body = body.with("password", params.get("password"));
        }
        if (params.containsKey("refresh_token")) {
            body = body.with("refresh_token", params.get("refresh_token"));
        }
        if (clientSecret != null && !clientSecret.isBlank()) {
            body = body.with("client_secret", clientSecret);
        }

        return webClient.post()
                .uri("/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        response -> response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                                .flatMap(errorBody -> {
                                    String errorMsg = extractErrorMessage(errorBody);
                                    int statusCode = response.statusCode().value();
                                    log.warn("Keycloak returned error: {} - {}", statusCode, errorMsg);
                                    return Mono.error(new KeycloakAuthException(errorMsg, statusCode));
                                }))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(tokenResponse -> {
                    log.debug("Keycloak token response received successfully");
                    return ResponseEntity.ok(tokenResponse);
                })
                .onErrorResume(e -> {
                    log.error("Keycloak authentication error: {}", e.getMessage(), e);
                    if (e instanceof KeycloakAuthException kae) {
                        Map<String, Object> error = new HashMap<>();
                        if (kae.getStatusCode() == 400 || kae.getStatusCode() == 401) {
                            error.put("error", "Invalid credentials or refresh token");
                            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error));
                        }
                        error.put("error", "Authentication service error: " + kae.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error));
                    }
                    if (e instanceof WebClientRequestException wcre) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "Cannot connect to authentication service: " + wcre.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error));
                    }
                    if (e instanceof WebClientResponseException wcre) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "Authentication service returned: " + wcre.getStatusCode() + " " + wcre.getResponseBodyAsString());
                        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error));
                    }
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Authentication service unavailable: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
                });
    }

    private String extractErrorMessage(Map<String, Object> errorBody) {
        if (errorBody == null) return "Unknown error";
        Object error = errorBody.get("error");
        Object errorDescription = errorBody.get("error_description");
        if (errorDescription != null) {
            return error + ": " + errorDescription;
        }
        return error != null ? error.toString() : "Unknown error";
    }

    private Mono<ResponseEntity<Map<String, Object>>> handleTimeout(Throwable e, String operation) {
        log.error("Timeout during {}: {}", operation, e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Authentication service timeout during " + operation);
        return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error));
    }

    private static class KeycloakAuthException extends RuntimeException {
        private final int statusCode;

        KeycloakAuthException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        int getStatusCode() {
            return statusCode;
        }
    }
}
