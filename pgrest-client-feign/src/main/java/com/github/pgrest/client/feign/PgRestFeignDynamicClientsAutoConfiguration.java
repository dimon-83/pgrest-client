package com.github.pgrest.client.feign;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(PgRestFeignProperties.class)
@ConditionalOnProperty(prefix = "pgrest.feign", name = "enabled", havingValue = "true")
@Import(PgRestFeignDynamicClientsRegistrar.class)
public class PgRestFeignDynamicClientsAutoConfiguration {
}