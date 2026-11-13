package com.example.apigateway.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Реактивный фильтр для извлечения company_id из JWT и добавления его в заголовки для downstream сервисов.
 * Работает в WebFlux контексте (Spring Cloud Gateway).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class ReactiveTenantFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .map(auth -> (Jwt) auth.getPrincipal())
                .map(this::extractCompanyId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(companyId -> {
                    exchange.getRequest().mutate()
                            .header("X-Company-Id", companyId.toString())
                            .build();
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private Optional<Long> extractCompanyId(Jwt jwt) {
        Object claim = jwt.getClaim("company_id");
        if (claim instanceof Number) {
            return Optional.of(((Number) claim).longValue());
        }
        if (claim instanceof String) {
            try {
                return Optional.of(Long.parseLong((String) claim));
            } catch (NumberFormatException ignored) {}
        }
        return Optional.empty();
    }
}