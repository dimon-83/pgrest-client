package com.github.pgrest.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

public class PgRestClientBeansRegistrarTest {
    @Test
    public void registersBeansForDatasources() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("pgrest.datasources.main.base-url", "http://localhost:3000");
        env.setProperty("pgrest.datasources.main.db-role", "api_user");
        env.setProperty("pgrest.datasources.main.jwt-ttl-seconds", "3600");
        env.setProperty("pgrest.datasources.main.auth-enabled", "true");
        env.setProperty("pgrest.datasources.main.connect-timeout-millis", "5000");

        PgRestClientBeansRegistrar registrar = new PgRestClientBeansRegistrar();
        registrar.setEnvironment(env);
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(PgRestClientBeansRegistrarTest.class), factory);

        Assertions.assertTrue(factory.containsBeanDefinition("pgrestConfig.main"));
        Assertions.assertTrue(factory.containsBeanDefinition("pgrestHttpClient.main"));
        Assertions.assertTrue(factory.containsBeanDefinition("pgrestExecutor.main"));
        Assertions.assertTrue(factory.containsBeanDefinition("pgrestClient.main"));
    }
}
