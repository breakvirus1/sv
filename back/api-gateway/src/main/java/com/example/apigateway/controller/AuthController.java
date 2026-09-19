package com.example.apigateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final WebClient webClient;
    private final String keycloakIssuerUri;
    private final String clientId;
    private final String clientSecret;

    public AuthController(WebClient.Builder webClientBuilder,
                          @Value("${app.keycloak.issuer-uri:http://192.168.1.40:8080/realms/print-sv}") String keycloakIssuerUri,
                          @Value("${app.keycloak.client-id:frontend}") String clientId,
                          @Value("${app.keycloak.client-secret:}") String clientSecret) {
        this.webClient = webClientBuilder.build();
        this.keycloakIssuerUri = keycloakIssuerUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody Mono<Map<String, String>> formDataMono) {
        return formDataMono.flatMap(formData -> {
            String username = formData.get("username");
            String password = formData.get("password");

            if (username == null || password == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "username and password are required");
                return Mono.just(ResponseEntity.badRequest().body(error));
            }

            return callKeycloakTokenEndpoint(Map.of(
                    "grant_type", "password",
                    "client_id", clientId,
                    "username", username,
                    "password", password,
                    "client_secret", clientSecret
            ));
        });
    }

    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> refresh(@RequestBody Mono<Map<String, String>> formDataMono) {
        return formDataMono.flatMap(formData -> {
            String refreshToken = formData.get("refresh_token");

            if (refreshToken == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "refresh_token is required");
                return Mono.just(ResponseEntity.badRequest().body(error));
            }

            return callKeycloakTokenEndpoint(Map.of(
                    "grant_type", "refresh_token",
                    "client_id", clientId,
                    "refresh_token", refreshToken,
                    "client_secret", clientSecret
            ));
        });
    }

    @SuppressWarnings("unchecked")
    private Mono<ResponseEntity<Map<String, Object>>> callKeycloakTokenEndpoint(Map<String, String> params) {
        var body = BodyInserters.fromFormData("grant_type", params.get("grant_type"))
                .with("client_id", params.get("client_id"))
                .with("username", params.getOrDefault("username", ""))
                .with("password", params.getOrDefault("password", ""))
                .with("refresh_token", params.getOrDefault("refresh_token", ""));

        if (clientSecret != null && !clientSecret.isBlank()) {
            body = body.with("client_secret", clientSecret);
        }

        return webClient.post()
                .uri(keycloakIssuerUri + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                                .flatMap(errorBody ->
                                        Mono.error(new RuntimeException("Keycloak auth failed: " + errorBody))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(tokenResponse -> ResponseEntity.ok((Map<String, Object>) (Map<?, ?>) tokenResponse))
                .onErrorResume(e -> {
                    String message = e.getMessage();
                    Map<String, Object> error = new HashMap<>();
                    if (message != null && message.contains("invalid_grant")) {
                        error.put("error", "Invalid credentials or refresh token");
                        return Mono.just(ResponseEntity.<Map<String, Object>>status(401).body(error));
                    }
                    error.put("error", "Authentication service unavailable");
                    return Mono.just(ResponseEntity.<Map<String, Object>>status(502).body(error));
                });
    }
}
