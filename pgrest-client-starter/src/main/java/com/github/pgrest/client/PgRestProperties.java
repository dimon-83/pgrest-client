package com.github.pgrest.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pgrest")
public class PgRestProperties {
    private String baseUrl;
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 10000;
    private String dbRole;
    private String jwtSecret;
    private String secret;
    private int jwtTtlSeconds = 3600;
    private boolean authEnabled = true;

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
}