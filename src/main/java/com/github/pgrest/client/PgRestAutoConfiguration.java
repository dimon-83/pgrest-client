package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.github.pgrest.client.gateway.PgRestGatewayController;
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
    @ConditionalOnProperty(prefix = "pgrest", name = "baseUrl")
    public PgRestClient pgRestClient(PgRestProperties properties, HttpClient pgRestHttpClient, ObjectMapper pgRestObjectMapper) {
        return new PgRestClient(properties, pgRestHttpClient, pgRestObjectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "pgrest", name = "registerToNacos", havingValue = "true", matchIfMissing = true)
    public PgRestNacosRegistrar pgRestNacosRegistrar(PgRestProperties properties, NacosServiceManager nacosServiceManager, NacosDiscoveryProperties discoveryProperties) {
        return new PgRestNacosRegistrar(properties, nacosServiceManager, discoveryProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "pgrest.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PgRestGatewayController pgRestGatewayController(PgRestClient client) {
        return new PgRestGatewayController(client);
    }
}