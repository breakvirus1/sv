package com.example.employeeservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Выдаёт токен с правами на Keycloak Admin REST API.
 * Права на /admin/realms есть только у bootstrap-администратора master realm:
 * service-account клиента realm-management ролей не имеет и получает 403.
 */
@Component
@Slf4j
public class KeycloakAdminTokenProvider {

    @Value("${keycloak.admin.server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm:print-sv}")
    private String keycloakRealm;

    @Value("${keycloak.admin.client-id:order-service}")
    private String keycloakClientId;

    @Value("${keycloak.admin.client-secret:order-service-secret}")
    private String keycloakClientSecret;

    @Value("${keycloak.admin.master-username:}")
    private String keycloakMasterUsername;

    @Value("${keycloak.admin.master-password:}")
    private String keycloakMasterPassword;

    public String obtainToken() {
        return obtainToken(new RestTemplate());
    }

    public String obtainToken(RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 1. bootstrap-админ master realm (учётные данные берутся из окружения, не хардкод)
        if (!keycloakMasterUsername.isBlank() && !keycloakMasterPassword.isBlank()) {
            try {
                String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";
                var tokenRequest = new HttpEntity<>(
                        "grant_type=password&client_id=admin-cli&username=" + keycloakMasterUsernameвцы
            }
        }
        // 2. service-account клиента в рабочем realm
        String tokenUrl = keycloakServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
        var tokenRequest = new HttpEntity<>(
                "grant_type=client_credentials&client_id=" + keycloakClientId + "&client_secret=" + keycloakClientSecret,
                headers);
        var tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
        String accessToken = tokenResponse.getBody() == null
                ? null : (String) tokenResponse.getBody().get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException(
                    "Keycloak не выдал токен для Admin REST API: задайте KEYCLOAK_ADMIN_MASTER_USERNAME/PASSWORD");
        }
        return accessToken;
    }
}
