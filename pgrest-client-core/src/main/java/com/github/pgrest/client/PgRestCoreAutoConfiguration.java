package com.github.pgrest.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.pgrest.client.http.JdkHttpExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.net.http.HttpClient;
import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(PgRestCoreProperties.class)
@Import(PgRestClientBeansRegistrar.class)
public class PgRestCoreAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper pgRestObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean(name = "pgRestClient")
    @ConditionalOnProperty(prefix = "pgrest", name = "baseUrl")
    public PgRestClient pgRestClient(PgRestCoreProperties properties, ObjectMapper pgRestObjectMapper) {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl(properties.getBaseUrl());
        cfg.setDbRole(properties.getDbRole());
        cfg.setJwtSecret(properties.getJwtSecret());
        cfg.setSecret(properties.getSecret());
        cfg.setJwtTtlSeconds(properties.getJwtTtlSeconds());
        cfg.setAuthEnabled(properties.isAuthEnabled());
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis())).build();
        return new PgRestClient(cfg, new JdkHttpExecutor(httpClient), pgRestObjectMapper);
    }
}