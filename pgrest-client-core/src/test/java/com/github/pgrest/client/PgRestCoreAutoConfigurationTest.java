package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PgRestCoreAutoConfigurationTest {
    @Test
    public void objectMapperConfigured() {
        PgRestCoreAutoConfiguration conf = new PgRestCoreAutoConfiguration();
        ObjectMapper mapper = conf.pgRestObjectMapper();
        Assertions.assertNotNull(mapper);
        Assertions.assertEquals(PropertyNamingStrategies.SNAKE_CASE, mapper.getPropertyNamingStrategy());
    }

    @Test
    public void pgRestClientBeanCreated() {
        PgRestCoreAutoConfiguration conf = new PgRestCoreAutoConfiguration();
        PgRestCoreProperties props = new PgRestCoreProperties();
        props.setBaseUrl("http://localhost:3000");
        props.setDbRole("api_user");
        props.setConnectTimeoutMillis(2000);

        PgRestClient client = conf.pgRestClient(props, conf.pgRestObjectMapper());
        Assertions.assertNotNull(client);
    }
}

