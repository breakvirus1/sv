package com.example.employeeservice.service;

import com.example.employeeservice.dto.KeycloakUserCreateRequest;
import com.example.employeeservice.exception.BadRequestException;
import com.example.employeeservice.exception.ConflictException;
import com.example.employeeservice.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Создание учётных записей в Keycloak через Admin REST API.
 * Роль назначается отдельным запросом: Keycloak игнорирует realmRoles при создании пользователя.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private final KeycloakAdminTokenProvider tokenProvider;

    @Value("${keycloak.admin.server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.admin.realm:print-sv}")
    private String keycloakRealm;

    /** Создаёт пользователя с ролью и атрибутом company_id. Возвращает id нового пользователя. */
    public String createUser(KeycloakUserCreateRequest request, Long companyId) {
        RestTemplate restTemplate = new RestTemplate();
        String adminToken = tokenProvider.obtainToken(restTemplate);
        HttpHeaders headers = authHeaders(adminToken);
        String usersUrl = adminUrl("/users");

        if (!findUsers(restTemplate, headers, request.getUsername()).isEmpty()) {
            throw new ConflictException("Пользователь с таким логином уже существует в Keycloak");
        }

        String userId;
        try {
            HttpEntity<Map<String, Object>> createRequest =
                    new HttpEntity<>(buildUserPayload(request, companyId), headers);
            var response = restTemplate.postForEntity(usersUrl, createRequest, String.class);
            userId = extractUserId(response.getHeaders().getLocation());
        } catch (HttpStatusCodeException e) {
            log.error("Keycloak rejected user creation for {}: {} {}",
                    request.getUsername(), e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 409 || e.getResponseBodyAsString().contains("exists")) {
                throw new ConflictException("Пользователь с таким логином уже существует в Keycloak");
            }
            throw new BadRequestException("Keycloak отклонил создание пользователя: "
                    + e.getResponseBodyAsString());
        }

        if (userId == null) {
            throw new BadRequestException("Keycloak не вернул id созданного пользователя");
        }

        assignRealmRole(restTemplate, headers, userId, normalizeRole(request.getRole()));
        log.info("Created Keycloak user {} (id={}) with role {}", request.getUsername(), userId,
                normalizeRole(request.getRole()));
        return userId;
    }

    /**
     * Сбрасывает пароль существующего пользователя Keycloak.
     * Значение текущего пароля недоступно по определению — Admin API отдаёт только параметры хеша,
     * поэтому реализовано именно изменение (reset), а не просмотр.
     */
    public void updatePassword(String username, String password, boolean temporary) {
        RestTemplate restTemplate = new RestTemplate();
        String adminToken = tokenProvider.obtainToken(restTemplate);
        HttpHeaders headers = authHeaders(adminToken);

        List<Map<String, Object>> users = findUsers(restTemplate, headers, username);
        if (users.isEmpty()) {
            throw new NotFoundException("Пользователь " + username + " не найден в Keycloak");
        }
        Object userId = users.get(0).get("id");
        if (userId == null) {
            throw new BadRequestException("Keycloak не вернул id пользователя " + username);
        }

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", temporary);

        try {
            restTemplate.put(adminUrl("/users/" + userId + "/reset-password"),
                    new HttpEntity<>(credential, headers));
        } catch (HttpStatusCodeException e) {
            // тело и значение пароля в лог не выписываем
            log.error("Keycloak rejected password update for {}: {}", username, e.getStatusCode());
            if (e.getStatusCode().value() == 404) {
                throw new NotFoundException("Пользователь " + username + " не найден в Keycloak");
            }
            throw new BadRequestException("Keycloak отклонил смену пароля: " + e.getResponseBodyAsString());
        }
        log.info("Password updated for Keycloak user {}", username);
    }

    /**
     * Обновляет профиль пользователя Keycloak (логин, ФИО, email) по текущему логину.
     * Если пользователь в Keycloak не найден — только логируем предупреждение (сотрудник мог быть
     * создан только в основной базе), чтобы синхронизация не ломала локальные правки.
     */
    public void updateUser(String currentUsername, String newUsername, String fullName, String email) {
        RestTemplate restTemplate = new RestTemplate();
        String adminToken = tokenProvider.obtainToken(restTemplate);
        HttpHeaders headers = authHeaders(adminToken);

        List<Map<String, Object>> users = findUsers(restTemplate, headers, currentUsername);
        if (users.isEmpty()) {
            log.warn("Keycloak user {} not found for update, skipping Keycloak sync", currentUsername);
            return;
        }
        Object userId = users.get(0).get("id");
        if (userId == null) {
            throw new BadRequestException("Keycloak не вернул id пользователя " + currentUsername);
        }

        Map<String, Object> payload = new HashMap<>();
        if (newUsername != null && !newUsername.isBlank()) {
            payload.put("username", newUsername);
        }
        String[] nameParts = splitFullName(fullName);
        payload.put("firstName", nameParts[0]);
        payload.put("lastName", nameParts[1]);
        if (email != null) {
            payload.put("email", email);
            // Email считается подтверждённым: Keycloak не требует подтверждение при обновлении
            payload.put("emailVerified", true);
        }

        try {
            restTemplate.put(adminUrl("/users/" + userId), new HttpEntity<>(payload, headers));
        } catch (HttpStatusCodeException e) {
            log.error("Keycloak rejected user update for {}: {}", currentUsername, e.getStatusCode());
            if (e.getStatusCode().value() == 404) {
                throw new NotFoundException("Пользователь " + currentUsername + " не найден в Keycloak");
            }
            if (e.getStatusCode().value() == 409) {
                throw new ConflictException("Пользователь с таким логином уже существует в Keycloak");
            }
            throw new BadRequestException("Keycloak отклонил обновление пользователя: " + e.getResponseBodyAsString());
        }
        log.info("Updated Keycloak user {} -> {}", currentUsername, newUsername);
    }

    /**
     * Удаляет пользователя Keycloak по логину. Отсутствие пользователя не считается ошибкой
     * ( Keycloak и основная база могли разойтись ранее ) — операция идемпотентна.
     */
    public void deleteUser(String username) {
        RestTemplate restTemplate = new RestTemplate();
        String adminToken = tokenProvider.obtainToken(restTemplate);
        HttpHeaders headers = authHeaders(adminToken);

        List<Map<String, Object>> users = findUsers(restTemplate, headers, username);
        if (users.isEmpty()) {
            log.warn("Keycloak user {} not found for deletion, skipping Keycloak sync", username);
            return;
        }
        Object userId = users.get(0).get("id");
        if (userId == null) {
            throw new BadRequestException("Keycloak не вернул id пользователя " + username);
        }

        try {
            restTemplate.exchange(adminUrl("/users/" + userId), HttpMethod.DELETE,
                    new HttpEntity<>(headers), Void.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                log.warn("Keycloak user {} already absent during deletion", username);
                return;
            }
            log.error("Keycloak rejected user deletion for {}: {}", username, e.getStatusCode());
            throw new BadRequestException("Keycloak отклонил удаление пользователя: " + e.getResponseBodyAsString());
        }
        log.info("Deleted Keycloak user {}", username);
    }

    private Map<String, Object> buildUserPayload(KeycloakUserCreateRequest request, Long companyId) {
        String[] nameParts = splitFullName(request.getFullName());

        Map<String, Object> passwordCredential = new HashMap<>();
        passwordCredential.put("type", "password");
        passwordCredential.put("value", request.getPassword());
        passwordCredential.put("temporary", false);

        // Email необязателен: если не задан — генерируем произвольный, чтобы пользователь
        // гарантированно мог войти без верификации email.
        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            String localPart = request.getUsername() == null ? "user" : request.getUsername().replaceAll("[^a-zA-Z0-9._-]", "");
            if (localPart.isBlank()) localPart = "user";
            email = localPart + "@noemail.local";
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", request.getUsername());
        payload.put("firstName", nameParts[0]);
        payload.put("lastName", nameParts[1]);
        payload.put("email", email);
        payload.put("emailVerified", true);
        payload.put("enabled", true);
        payload.put("credentials", List.of(passwordCredential));
        if (companyId != null) {
            payload.put("attributes", Map.of("company_id", List.of(String.valueOf(companyId))));
        }
        return payload;
    }

    private String[] splitFullName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        String firstName = parts[0];
        String lastName = parts.length > 1
                ? String.join(" ", List.of(parts).subList(1, parts.length))
                : firstName;
        return new String[]{firstName, lastName};
    }

    private List<Map<String, Object>> findUsers(RestTemplate restTemplate, HttpHeaders headers, String username) {
        try {
            var response = restTemplate.exchange(
                    adminUrl("/users?username=" + uriEncode(username) + "&exact=true"),
                    HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("Cannot check existing Keycloak user {}: {}", username, e.getStatusCode());
            return List.of();
        }
    }

    private void assignRealmRole(RestTemplate restTemplate, HttpHeaders headers, String userId, String roleName) {
        Map<String, Object> role;
        try {
            var roleResponse = restTemplate.exchange(
                    adminUrl("/roles/" + uriEncode(roleName)),
                    HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            role = roleResponse.getBody();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                throw new BadRequestException("Роль " + roleName + " не найдена в Keycloak");
            }
            throw new BadRequestException("Не удалось получить роль " + roleName + ": " + e.getStatusCode());
        }

        if (role == null || role.get("id") == null) {
            throw new BadRequestException("Роль " + roleName + " не найдена в Keycloak");
        }

        try {
            restTemplate.postForEntity(adminUrl("/users/" + userId + "/role-mappings/realm"),
                    new HttpEntity<>(List.of(role), headers), String.class);
        } catch (HttpStatusCodeException e) {
            log.error("Cannot assign role {} to user {}: {} {}",
                    roleName, userId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("Не удалось назначить роль " + roleName);
        }
    }

    /** Keycloak хранит роли без префикса ROLE_, фронт присылает с префиксом. */
    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private String adminUrl(String path) {
        return keycloakServerUrl + "/admin/realms/" + keycloakRealm + path;
    }

    private String uriEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Из Location-заголовка ответа Keycloak: .../users/{id}. */
    private String extractUserId(java.net.URI location) {
        if (location == null) return null;
        String path = location.getPath();
        int idx = path.lastIndexOf('/');
        String id = idx >= 0 ? path.substring(idx + 1) : path;
        return id.isBlank() ? null : id;
    }
}
