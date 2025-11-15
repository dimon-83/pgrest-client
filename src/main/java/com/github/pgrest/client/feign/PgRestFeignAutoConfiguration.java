package com.github.pgrest.client.feign;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfiguration
@ConditionalOnClass(EnableFeignClients.class)
@EnableFeignClients(basePackageClasses = PgRestFeignClient.class)
public class PgRestFeignAutoConfiguration {
    @Bean
    public PgRestTypedClient pgRestTypedClient(@Autowired(required = false) PgRestGatewayFeignClient gateway,
                                               @Autowired(required = false) PgRestFeignClient direct,
                                               ObjectMapper pgRestFeignObjectMapper) {
        return new PgRestTypedClient(gateway, direct, pgRestFeignObjectMapper);
    }
}