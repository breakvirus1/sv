package com.example.test.printsv.config;

import com.example.test.printsv.service.UserService;
import com.example.test.printsv.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@Lazy
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
    @Autowired
    private final JwtUtil jwtUtil;
    @Autowired
    private final UserService userDetailsService;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, @Lazy UserService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostConstruct
    public void init() {

    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String path = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");

        logger.debug("JwtFilter: Обработка пути '{}', заголовок Auth: {}", path, authHeader != null ? "присутствует" : "отсутствует");

        // Пропуск для permitAll (/auth/**)
        if (path.startsWith("/auth/")) {
            logger.debug("Пропуск JWT для permitAll: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Для защищённых путей (/hello и т.д.)
        String username = null;
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
                logger.debug("Извлечён username из токена: {}", username);
            } catch (Exception e) {
                logger.error("Ошибка извлечения username из токена: {}", e.getMessage());
            }
        } else {
            logger.debug("Нет Bearer-токена для пути: {}", path);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                            null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Установлен SecurityContext для: {}", username);
                } else {
                    logger.warn("Токен невалиден для: {}", username);
                }
            } catch (Exception e) {
                logger.error("Ошибка загрузки UserDetails для {}: {}", username, e.getMessage());
            }
        }

        logger.debug("JwtFilter завершён для '{}', текущий auth: {}", path,
                SecurityContextHolder.getContext().getAuthentication() != null ? "установлен" : "анонимный");
        filterChain.doFilter(request, response);
    }


//    @Override
//    protected void doFilterInternal(@NonNull HttpServletRequest request,
//                                    @NonNull HttpServletResponse response,
//                                    @NonNull FilterChain filterChain)
//            throws ServletException, IOException {
//
//        final String authHeader = request.getHeader("Authorization");
//
//        String username = null;
//        String token = null;
//
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            token = authHeader.substring(7);
//            username = jwtUtil.extractUsername(token);
//        }
//
//        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//            if (jwtUtil.validateToken(token, userDetails)) {
//                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
//                        null, userDetails.getAuthorities());
//                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                SecurityContextHolder.getContext().setAuthentication(authToken);
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }

    @PreDestroy
    public void destroy() {

    }
}
