package com.github.pgrest.client;

public class PgClientConfig {
    private String baseUrl;
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