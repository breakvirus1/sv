package com.example.apigateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Конвертер для извлечения ролей из Keycloak JWT токена.
 * Используется в ReactiveJwtAuthenticationConverter для получения GrantedAuthority.
 * Извлекает роли из:
 * - realm_access.roles
 * - resource_access.print-sv-client.roles
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, Flux<GrantedAuthority>> {

    @Override
    public Flux<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Извлекаем роли из realm_access
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList()));
        }

        // Извлекаем роли из resource_access (если есть)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.get("print-sv-client") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> clientRoles = (Map<String, Object>) resourceAccess.get("print-sv-client");
            if (clientRoles.get("roles") instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> clientRoleList = (List<String>) clientRoles.get("roles");
                authorities.addAll(clientRoleList.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList()));
            }
        }

        return Flux.fromIterable(authorities);
    }
}