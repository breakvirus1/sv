package com.example.apigateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Конвертер JWT токена в аутентификационный токен для reactive WebFlux.
 * Извлекает роли из Keycloak JWT токена:
 * - realm_access.roles
 * - resource_access.print-sv-client.roles
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();

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

        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }
}