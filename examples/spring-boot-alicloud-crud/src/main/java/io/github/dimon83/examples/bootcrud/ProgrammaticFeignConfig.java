package io.github.dimon83.examples.bootcrud;

import feign.Feign;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ProgrammaticFeignConfig {
    @Bean
    public RequestInterceptor usersHeadersInterceptor() {
        return template -> {
            template.header("Accept", "application/json");
            template.header("Prefer", "count=exact");
        };
    }

    @Bean
    public Encoder usersFeignEncoder(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        return new SpringEncoder(() -> new HttpMessageConverters(converter));
    }

    @Bean
    public Decoder usersFeignDecoder(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        return new SpringDecoder(() -> new HttpMessageConverters(converter));
    }

    
}