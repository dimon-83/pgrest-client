package com.github.pgrest.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "pgrest")
public class PgRestCoreProperties {
    private String baseUrl;
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 10000;
    private String dbRole;
    private String jwtSecret;
    private String secret;
    private int jwtTtlSeconds = 3600;
    private boolean authEnabled = true;
    private Map<String, DataSource> datasources = new LinkedHashMap<>();

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
    public void setReadTimeoutMillis(int readTimeoutMillis) { this.readTimeoutMillis = readTimeoutMillis; }
    public String getDbRole() { return dbRole; }
    public void setDbRole(String dbRole) { this.dbRole = dbRole; }
    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public int getJwtTtlSeconds() { return jwtTtlSeconds; }
    public void setJwtTtlSeconds(int jwtTtlSeconds) { this.jwtTtlSeconds = jwtTtlSeconds; }
    public boolean isAuthEnabled() { return authEnabled; }
    public void setAuthEnabled(boolean authEnabled) { this.authEnabled = authEnabled; }
    public Map<String, DataSource> getDatasources() { return datasources; }
    public void setDatasources(Map<String, DataSource> datasources) { this.datasources = datasources; }

    public static class DataSource {
        private String baseUrl;
        private int connectTimeoutMillis = 5000;
        private int readTimeoutMillis = 10000;
        private String dbRole;
        private String jwtSecret;
        private String secret;
        private int jwtTtlSeconds = 3600;
        private boolean authEnabled = true;
        private String jwtIssuer;
        private String jwtAudience;
        private String defaultUser;
        private boolean addNbf;
        private boolean addJti;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
        public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
        public int getReadTimeoutMillis() { return readTimeoutMillis; }
        public void setReadTimeoutMillis(int readTimeoutMillis) { this.readTimeoutMillis = readTimeoutMillis; }
        public String getDbRole() { return dbRole; }
        public void setDbRole(String dbRole) { this.dbRole = dbRole; }
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public int getJwtTtlSeconds() { return jwtTtlSeconds; }
        public void setJwtTtlSeconds(int jwtTtlSeconds) { this.jwtTtlSeconds = jwtTtlSeconds; }
        public boolean isAuthEnabled() { return authEnabled; }
        public void setAuthEnabled(boolean authEnabled) { this.authEnabled = authEnabled; }
        public String getJwtIssuer() { return jwtIssuer; }
        public void setJwtIssuer(String jwtIssuer) { this.jwtIssuer = jwtIssuer; }
        public String getJwtAudience() { return jwtAudience; }
        public void setJwtAudience(String jwtAudience) { this.jwtAudience = jwtAudience; }
        public String getDefaultUser() { return defaultUser; }
        public void setDefaultUser(String defaultUser) { this.defaultUser = defaultUser; }
        public boolean isAddNbf() { return addNbf; }
        public void setAddNbf(boolean addNbf) { this.addNbf = addNbf; }
        public boolean isAddJti() { return addJti; }
        public void setAddJti(boolean addJti) { this.addJti = addJti; }
    }
}