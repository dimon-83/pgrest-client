package com.github.pgrest.client.feign;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "pgrest.feign")
public class PgRestFeignProperties {
    private boolean enabled;
    private Map<String, Client> clients = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<String, Client> getClients() { return clients; }
    public void setClients(Map<String, Client> clients) { this.clients = clients; }

    public static class Client {
        private String serviceName;
        private String mode = "gateway"; // gateway | direct
        private String url;
        private String pathPrefix;
        private Boolean decode404;

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getPathPrefix() { return pathPrefix; }
        public void setPathPrefix(String pathPrefix) { this.pathPrefix = pathPrefix; }
        public Boolean getDecode404() { return decode404; }
        public void setDecode404(Boolean decode404) { this.decode404 = decode404; }
    }
}