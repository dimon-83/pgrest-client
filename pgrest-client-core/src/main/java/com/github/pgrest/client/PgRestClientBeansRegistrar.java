package com.github.pgrest.client;

import com.github.pgrest.client.http.JdkHttpExecutor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

public class PgRestClientBeansRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) { this.environment = environment; }

    @Override
    public void registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        PgRestCoreProperties props = Binder.get(environment).bind("pgrest", Bindable.of(PgRestCoreProperties.class)).orElseGet(PgRestCoreProperties::new);
        Map<String, PgRestCoreProperties.DataSource> dsMap = props.getDatasources();
        if (dsMap == null || dsMap.isEmpty()) return;
        dsMap.forEach((name, ds) -> {
            BeanDefinitionBuilder cfgDef = BeanDefinitionBuilder.genericBeanDefinition(PgClientConfig.class);
            cfgDef.addPropertyValue("baseUrl", ds.getBaseUrl());
            cfgDef.addPropertyValue("dbRole", ds.getDbRole());
            cfgDef.addPropertyValue("jwtSecret", ds.getJwtSecret());
            cfgDef.addPropertyValue("secret", ds.getSecret());
            cfgDef.addPropertyValue("jwtTtlSeconds", ds.getJwtTtlSeconds());
            cfgDef.addPropertyValue("authEnabled", ds.isAuthEnabled());
            cfgDef.addPropertyValue("jwtIssuer", ds.getJwtIssuer());
            cfgDef.addPropertyValue("jwtAudience", ds.getJwtAudience());
            cfgDef.addPropertyValue("defaultUser", ds.getDefaultUser());
            cfgDef.addPropertyValue("addNbf", ds.isAddNbf());
            cfgDef.addPropertyValue("addJti", ds.isAddJti());
            registry.registerBeanDefinition("pgrestConfig." + name, cfgDef.getBeanDefinition());

            BeanDefinitionBuilder httpClientDef = BeanDefinitionBuilder.genericBeanDefinition(HttpClient.class, () -> HttpClient.newBuilder().connectTimeout(Duration.ofMillis(ds.getConnectTimeoutMillis())).build());
            registry.registerBeanDefinition("pgrestHttpClient." + name, httpClientDef.getBeanDefinition());

            BeanDefinitionBuilder execDef = BeanDefinitionBuilder.genericBeanDefinition(JdkHttpExecutor.class);
            execDef.addConstructorArgReference("pgrestHttpClient." + name);
            registry.registerBeanDefinition("pgrestExecutor." + name, execDef.getBeanDefinition());

            BeanDefinitionBuilder clientDef = BeanDefinitionBuilder.genericBeanDefinition(PgRestClient.class);
            clientDef.addConstructorArgReference("pgrestConfig." + name);
            clientDef.addConstructorArgReference("pgrestExecutor." + name);
            clientDef.addConstructorArgReference("pgRestObjectMapper");
            registry.registerBeanDefinition("pgrestClient." + name, clientDef.getBeanDefinition());
        });
    }
}