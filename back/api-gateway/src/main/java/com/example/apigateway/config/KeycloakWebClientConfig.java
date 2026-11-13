package com.example.apigateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * WebClient для обмена токенами с Keycloak.
 *
 * <p>Штатный WebClient.Builder из Spring Boot использует пул с maxIdleTime/maxLifeTime
 * короче, чем keepAlive-таймаут Keycloak (по умолчанию 300 с). После простоя шлюз берёт
 * из пула соединение, которое Keycloak уже закрыл, и запрос висит до срабатывания
 * общего timeout (ClosedChannelException). Явный пул с коротким maxIdleTime и фоновой
 * эвикцией убирает гонку, а responseTimeout задаёт верхнюю границу ожидания ответа.
 *
 * <p>Важно: НЕ добавляем сюда ReadTimeoutHandler/WriteTimeoutHandler — на простаивающем
 * соединении из пула они срабатывают сами по себе (ReadTimeoutException) и рвут запрос,
 * который только что был взят из пула.
 */
@Configuration
public class KeycloakWebClientConfig {

    @Bean
    public WebClient keycloakWebClient(WebClient.Builder builder) {
        ConnectionProvider provider = ConnectionProvider.builder("keycloak-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .evictInBackground(Duration.ofSeconds(10))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(20));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}