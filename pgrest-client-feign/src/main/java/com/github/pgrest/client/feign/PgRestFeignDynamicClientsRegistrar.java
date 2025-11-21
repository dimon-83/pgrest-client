package com.github.pgrest.client.feign;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;

public class PgRestFeignDynamicClientsRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) { this.environment = environment; }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        PgRestFeignProperties props = Binder.get(environment).bind("pgrest.feign", Bindable.of(PgRestFeignProperties.class)).orElseGet(PgRestFeignProperties::new);
        if (props.getClients() == null || props.getClients().isEmpty()) return;
        props.getClients().forEach((key, client) -> {
            String serviceName = client.getServiceName();
            if (serviceName == null || serviceName.isBlank()) return;
            boolean direct = "direct".equalsIgnoreCase(client.getMode());
            Class<?> type = direct ? com.github.pgrest.client.feign.PgRestDirectFeignClient.class : com.github.pgrest.client.feign.PgRestFeignClient.class;
            String beanName = (direct ? "pgrestDirectFeignClient." : "pgrestFeignClient.") + serviceName;

            BeanDefinitionBuilder b = BeanDefinitionBuilder.genericBeanDefinition(FeignClientFactoryBean.class);
            b.addPropertyValue("name", serviceName);
            b.addPropertyValue("contextId", serviceName);
            b.addPropertyValue("type", type);
            if (client.getUrl() != null && !client.getUrl().isBlank()) b.addPropertyValue("url", client.getUrl());
            if (client.getPathPrefix() != null && !client.getPathPrefix().isBlank()) b.addPropertyValue("path", client.getPathPrefix());
            if (client.getDecode404() != null) b.addPropertyValue("decode404", client.getDecode404());
            b.addPropertyValue("refreshable", false);
            b.addPropertyValue("fallback", null);
            b.addPropertyValue("fallbackFactory", null);
            b.addPropertyValue("configuration", new Class<?>[]{PgRestFeignConfig.class});
            registry.registerBeanDefinition(beanName, b.getBeanDefinition());
        });
    }
}