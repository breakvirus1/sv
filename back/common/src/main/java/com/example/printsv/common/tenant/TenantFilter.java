package com.example.printsv.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Извлекает company_id из JWT и устанавливает в TenantContext для маршрутизации DataSource.
 */
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) auth.getPrincipal();
                Long companyId = extractCompanyId(jwt);
                if (companyId != null) {
                    TenantContext.setTenantId(companyId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Long extractCompanyId(Jwt jwt) {
        Object claim = jwt.getClaim("company_id");
        if (claim instanceof Number) {
            return ((Number) claim).longValue();
        }
        if (claim instanceof String) {
            try {
                return Long.parseLong((String) claim);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
