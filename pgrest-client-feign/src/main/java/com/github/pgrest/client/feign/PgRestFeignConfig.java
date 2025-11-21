package com.github.pgrest.client.feign;

import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

@Configuration
public class PgRestFeignConfig {
    @Bean
    public RequestInterceptor pgRestHeadersInterceptor() {
        return template -> {
            template.header("Accept", "application/json");
            template.header("Prefer", "count=exact");
        };
    }

    @Bean
    public ObjectMapper pgRestFeignObjectMapper(@org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper existing) {
        if (existing != null) return existing;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

    @Bean
    public Encoder feignEncoder(ObjectMapper pgRestFeignObjectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(pgRestFeignObjectMapper);
        return new SpringEncoder(() -> new HttpMessageConverters(converter));
    }

    @Bean
    public Decoder feignDecoder(ObjectMapper pgRestFeignObjectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(pgRestFeignObjectMapper);
        return new SpringDecoder(() -> new HttpMessageConverters(converter));
    }
}