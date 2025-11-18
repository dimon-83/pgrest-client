package com.github.pgrest.client;

public class PgClientConfig {
    private String baseUrl;
    private String dbRole;
    private String jwtSecret;
    private String secret;
    private int jwtTtlSeconds = 3600;
    private boolean authEnabled = true;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
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