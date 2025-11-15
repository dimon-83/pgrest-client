package com.github.pgrest.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties(prefix = "pgrest")
public class PgRestProperties {
    private String baseUrl;
    private String serviceName;
    private String group;
    private boolean registerToNacos = true;
    private Map<String, String> metadata;
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 10000;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public boolean isRegisterToNacos() { return registerToNacos; }
    public void setRegisterToNacos(boolean registerToNacos) { this.registerToNacos = registerToNacos; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
    public void setReadTimeoutMillis(int readTimeoutMillis) { this.readTimeoutMillis = readTimeoutMillis; }
}