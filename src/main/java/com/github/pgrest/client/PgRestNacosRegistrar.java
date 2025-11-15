package com.github.pgrest.client;

import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;

import java.net.URI;
import java.util.Map;
import java.util.Properties;

@ConditionalOnProperty(prefix = "pgrest", name = "registerToNacos", havingValue = "true", matchIfMissing = true)
public class PgRestNacosRegistrar implements SmartLifecycle {
    private final PgRestProperties properties;
    private final NacosServiceManager serviceManager;
    private final NacosDiscoveryProperties discoveryProperties;
    private volatile boolean running;

    public PgRestNacosRegistrar(PgRestProperties properties, NacosServiceManager serviceManager, NacosDiscoveryProperties discoveryProperties) {
        this.properties = properties;
        this.serviceManager = serviceManager;
        this.discoveryProperties = discoveryProperties;
    }

    @Override
    public void start() {
        if (properties.getBaseUrl() == null || properties.getServiceName() == null) return;
        try {
            Properties nacosProps = discoveryProperties.getNacosProperties();
            NamingService namingService = serviceManager.getNamingService(nacosProps);
            URI uri = URI.create(properties.getBaseUrl());
            Instance instance = new Instance();
            instance.setIp(uri.getHost());
            int port = uri.getPort() > 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
            instance.setPort(port);
            instance.setHealthy(true);
            instance.setEphemeral(true);
            Map<String, String> md = properties.getMetadata();
            if (md != null) instance.setMetadata(md);
            String group = properties.getGroup() != null ? properties.getGroup() : discoveryProperties.getGroup();
            namingService.registerInstance(properties.getServiceName(), group, instance);
            running = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (!running) return;
        try {
            Properties nacosProps = discoveryProperties.getNacosProperties();
            NamingService namingService = serviceManager.getNamingService(nacosProps);
            URI uri = URI.create(properties.getBaseUrl());
            String ip = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
            String group = properties.getGroup() != null ? properties.getGroup() : discoveryProperties.getGroup();
            namingService.deregisterInstance(properties.getServiceName(), group, ip, port);
            running = false;
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}