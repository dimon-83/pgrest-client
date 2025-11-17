package com.github.pgrest.client;

/*
 * Configuration categories:
 * - pgrest: client settings for PostgREST (baseUrl, dbRole, jwtSecret)
 * - nacos: service registration/discovery settings (serviceName, group, registerToNacos, metadata)
 * - http timeouts: HTTP client connect/read timeouts (connectTimeoutMillis, readTimeoutMillis)
 */

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties(prefix = "pgrest")
public class PgRestProperties {
    /** pgrest: Base URL of the PostgREST service, e.g. http://127.0.0.1:3000 */
    private String baseUrl;
    /** nacos: Service name used for registry/discovery */
    private String serviceName;
    /** nacos: Service group for registry isolation and routing */
    private String group;
    /** nacos: Whether to register to Nacos (default true) */
    private boolean registerToNacos = true;
    /** nacos: Metadata key/value pairs published to the registry */
    private Map<String, String> metadata;
    /** http: Connection timeout in milliseconds */
    private int connectTimeoutMillis = 5000;
    /** http: Read timeout in milliseconds */
    private int readTimeoutMillis = 10000;

    /** pgrest: Database role to embed in JWT 'role' claim (formerly db-anon-role). */
    private String dbRole;

    /** pgrest: Secret used to sign/verify JWT (maps to jwt-secret). Inject via external config/env; do not hard-code. */
    private String jwtSecret;
    /** pgrest: Legacy alias for jwt secret if used elsewhere */
    private String secret;
    /** pgrest: JWT TTL seconds */
    private int jwtTtlSeconds = 3600;
    /** pgrest: Enable auto Authorization header injection */
    private boolean authEnabled = true;

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