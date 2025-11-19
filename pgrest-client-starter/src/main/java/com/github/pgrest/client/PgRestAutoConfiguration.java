package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.pgrest.client.gateway.PgRestController;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.JdkHttpExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.net.http.HttpClient;
import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(PgRestProperties.class)
public class PgRestAutoConfiguration {
    @Bean
    public ObjectMapper pgRestObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

    @Bean
    public HttpClient pgRestHttpClient(PgRestProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                .build();
    }

    @Bean
    public HttpExecutor pgRestHttpExecutor(HttpClient pgRestHttpClient) {
        return new JdkHttpExecutor(pgRestHttpClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "pgrest", name = "baseUrl")
    public PgRestClient pgRestClient(PgRestProperties properties, HttpExecutor pgRestHttpExecutor, ObjectMapper pgRestObjectMapper) {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl(properties.getBaseUrl());
        cfg.setDbRole(properties.getDbRole());
        cfg.setJwtSecret(properties.getJwtSecret());
        cfg.setSecret(properties.getSecret());
        cfg.setJwtTtlSeconds(properties.getJwtTtlSeconds());
        cfg.setAuthEnabled(properties.isAuthEnabled());
        return new PgRestClient(cfg, pgRestHttpExecutor, pgRestObjectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "pgrest.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PgRestController PgRestController(PgRestClient client) {
        return new PgRestController(client);
    }
}