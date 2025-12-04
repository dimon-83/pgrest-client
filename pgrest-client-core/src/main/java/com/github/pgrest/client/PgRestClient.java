package com.github.pgrest.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PgRestClient {
    private final PgClientConfig config;
    private final HttpExecutor httpExecutor;
    private final ObjectMapper objectMapper;
    private volatile java.util.function.UnaryOperator<Map<String,Object>> claimsHandler;
    private volatile Supplier<String> authTokenSupplier;
    private volatile String readProfile;
    private volatile String writeProfile;

    public PgRestClient(PgClientConfig config, HttpExecutor httpExecutor, ObjectMapper objectMapper) {
        this.config = config;
        this.httpExecutor = httpExecutor;
        this.objectMapper = objectMapper;
    }

    public <T> List<T> list(String resource, PgQueryBuilder builder, Class<T> type) {
        String url = config.getBaseUrl() + "/" + resource + builder.build();
        HttpRequestData req = new HttpRequestData();
        req.setUrl(url);
        req.setMethod("GET");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Prefer", "count=exact");
        applyProfile(req, false);
        addAuth(req);
        try {
            HttpResponseData response = httpExecutor.execute(req);
            ensure2xx(response);
            return readList(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> PageResult<T> page(String resource, PgQueryBuilder builder, int page, int size, Class<T> type) {
        int offset = (page - 1) * size;
        PgQueryBuilder qb = builder.copy().limit(size).offset(offset);
        String url = config.getBaseUrl() + "/" + resource + qb.build();
        HttpRequestData req = new HttpRequestData();
        req.setUrl(url);
        req.setMethod("GET");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Prefer", "count=exact");
        applyProfile(req, false);
        addAuth(req);
        try {
            HttpResponseData response = httpExecutor.execute(req);
            ensure2xx(response);
            List<T> list = readList(response.getBody(), type);
            String contentRange = getHeader(response.getHeaders(), "Content-Range", "items */0");
            long total = parseTotal(contentRange);
            return new PageResult<>(page, size, total, list);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public Map<String, Object> get(String resource, PgQueryBuilder builder) {
        String url = config.getBaseUrl() + "/" + resource + builder.build();
        HttpRequestData req = new HttpRequestData();
        req.setUrl(url);
        req.setMethod("GET");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Prefer", "count=exact");
        applyProfile(req, false);
        addAuth(req);
        try {
            HttpResponseData response = httpExecutor.execute(req);
            return objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>(){});
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> T getById(String resource, Object id, Class<T> type) { return getById(resource, "id", id, new PgQueryBuilder(), type); }

    public <T> T getById(String resource, String idColumn, Object id, PgQueryBuilder builder, Class<T> type) {
        PgQueryBuilder qb = builder.copy().eq(idColumn, id).limit(1);
        String url = config.getBaseUrl() + "/" + resource + qb.build();
        HttpRequestData req = new HttpRequestData();
        req.setUrl(url);
        req.setMethod("GET");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Prefer", "count=exact");
        applyProfile(req, false);
        addAuth(req);
        try {
            HttpResponseData response = httpExecutor.execute(req);
            ensure2xx(response);
            List<T> list = readList(response.getBody(), type);
            return list == null || list.isEmpty() ? null : list.get(0);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private long parseTotal(String contentRange) {
        int slash = contentRange.lastIndexOf('/');
        if (slash >= 0) {
            String totalStr = contentRange.substring(slash + 1).trim();
            try { return Long.parseLong(totalStr); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public String issueJwt(Map<String,Object> claims) {
        Map<String,Object> payload = new java.util.HashMap<>();
        if (claims != null) payload.putAll(claims);
        if (claimsHandler != null) payload = claimsHandler.apply(payload);
        else {
            String du = config.getDefaultUser();
            if (du != null && !du.isBlank()) payload.put("user", du);
            else payload = java.util.function.UnaryOperator.<Map<String,Object>>identity().andThen(c -> { c.put("user", "test"); return c; }).apply(payload);
        }
        String secret = config.getSecret();
        if (secret == null || secret.isBlank()) secret = config.getJwtSecret();
        if (secret == null || secret.isBlank()) throw new IllegalStateException("jwt-secret is missing");
        secret = secret.trim();
        byte[] key = secret.startsWith("@") ? java.util.Base64.getDecoder().decode(secret.substring(1)) : secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) throw new IllegalStateException("jwt-secret must be at least 256 bits");
        long nowSec = java.time.Instant.now().getEpochSecond();
        long skew = Math.max(0, config.getJwtClockSkewSeconds());
        long expSec = nowSec + Math.max(1, config.getJwtTtlSeconds());
        if (config.isAddIat()) payload.put("iat", nowSec - skew);
        payload.put("exp", expSec);
        if (config.isAddNbf()) payload.put("nbf", nowSec - skew);
        if (config.isAddJti()) payload.put("jti", java.util.UUID.randomUUID().toString());
        String role = config.getDbRole();
        if (role != null && !role.isBlank()) payload.put("role", role);
        String iss = config.getJwtIssuer();
        if (iss != null && !iss.isBlank()) payload.put("iss", iss);
        String aud = config.getJwtAudience();
        if (aud != null && !aud.isBlank()) payload.put("aud", aud);
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson;
        try { payloadJson = objectMapper.writeValueAsString(payload); } catch (Exception e) { throw new RuntimeException(e); }
        java.util.Base64.Encoder enc = java.util.Base64.getUrlEncoder().withoutPadding();
        String headerB64 = enc.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = enc.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = headerB64 + "." + payloadB64;
        byte[] sig;
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec sk = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256");
            mac.init(sk);
            sig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new RuntimeException(e); }
        String sigB64 = enc.encodeToString(sig);
        return signingInput + "." + sigB64;
    }

    public void setClaimsHandler(java.util.function.UnaryOperator<Map<String,Object>> claimsHandler) { this.claimsHandler = claimsHandler; }
    public void setAuthTokenSupplier(Supplier<String> supplier) { this.authTokenSupplier = supplier; }
    public void setStaticAuthToken(String token) { this.authTokenSupplier = () -> token; }

    public PgFrom from(String resource) { return new PgFrom(this, resource); }
    public PgFn fn(String function) { return new PgFn(this, function); }
    public PgFn rpc(String function) { return new PgFn(this, function); }
    public PgRestClient setReadProfile(String schema) { this.readProfile = schema; return this; }
    public PgRestClient setWriteProfile(String schema) { this.writeProfile = schema; return this; }
    public PgRestClient setProfile(String schema) { this.readProfile = schema; this.writeProfile = schema; return this; }

    public <T> List<T> insert(String resource, Object payload, Class<T> type) {
        String url = config.getBaseUrl() + "/" + resource;
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
        req.setMethod("POST");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Prefer", "return=representation,count=exact");
        req.setBody(json);
        applyProfile(req, true);
        addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            ensure2xx(response);
            return readList(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> List<T> insert(String resource, Object payload, PgQueryBuilder builder, Class<T> type) {
        String url = config.getBaseUrl() + "/" + resource + builder.build();
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
        req.setMethod("POST");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Prefer", "return=representation,count=exact");
        req.setBody(json);
        applyProfile(req, true);
        addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            ensure2xx(response);
            return readList(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> List<T> insert(String resource, Object payload, PayloadFieldFilter filter, Class<T> type) {
        String url = config.getBaseUrl() + "/" + resource;
        try {
            Map<String,Object> map = payload instanceof Map ? (Map<String,Object>) payload : objectMapper.convertValue(payload, new TypeReference<Map<String,Object>>(){});
            Map<String,Object> filtered = filter == null ? map : filter.apply(resource, payload, map);
            String json = objectMapper.writeValueAsString(filtered);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
        req.setMethod("POST");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Prefer", "return=representation,count=exact");
        req.setBody(json);
        applyProfile(req, true);
        addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            ensure2xx(response);
            return readList(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> List<T> insert(String resource, Object payload, PgQueryBuilder builder, PayloadFieldFilter filter, Class<T> type) {
        String url = config.getBaseUrl() + "/" + resource + builder.build();
        try {
            Map<String,Object> map = payload instanceof Map ? (Map<String,Object>) payload : objectMapper.convertValue(payload, new TypeReference<Map<String,Object>>(){});
            Map<String,Object> filtered = filter == null ? map : filter.apply(resource, payload, map);
            String json = objectMapper.writeValueAsString(filtered);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
        req.setMethod("POST");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Prefer", "return=representation,count=exact");
        req.setBody(json);
        applyProfile(req, true);
        addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            ensure2xx(response);
            return readList(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> T rpcForObject(String function, Object payload, Class<T> type) {
        String url = config.getBaseUrl() + "/rpc/" + function;
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
        req.setMethod("POST");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Prefer", "count=exact");
        req.setBody(json);
        applyProfile(req, true);
        addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            return objectMapper.readValue(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> T rpcForObject(String function, Object payload, PgQueryBuilder builder, Class<T> type) {
        String url = config.getBaseUrl() + "/rpc/" + function + builder.build();
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
        req.setMethod("POST");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Prefer", "count=exact");
        req.setBody(json);
        applyProfile(req, true);
        addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            return objectMapper.readValue(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> List<T> rpcForList(String function, Object payload, Class<T> type) {
        String url = config.getBaseUrl() + "/rpc/" + function;
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
        req.setMethod("POST");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Prefer", "count=exact");
        req.setBody(json);
        applyProfile(req, true);
        addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            ensure2xx(response);
            return readList(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> List<T> rpcForList(String function, Object payload, PgQueryBuilder builder, Class<T> type) {
        String url = config.getBaseUrl() + "/rpc/" + function + builder.build();
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
            req.setMethod("POST");
            req.getHeaders().put("Accept", "application/json");
            req.getHeaders().put("Content-Type", "application/json");
            req.getHeaders().put("Prefer", "count=exact");
            req.setBody(json);
            applyProfile(req, true);
            addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            ensure2xx(response);
            return readList(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> List<T> update(String resource, PgQueryBuilder builder, Object payload, Class<T> type) {
        String url = config.getBaseUrl() + "/" + resource + builder.build();
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
        req.setMethod("PATCH");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Prefer", "return=representation,count=exact");
        req.setBody(json);
        applyProfile(req, true);
        addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            return readList(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public <T> List<T> update(String resource, PgQueryBuilder builder, Object payload, PayloadFieldFilter filter, Class<T> type) {
        String url = config.getBaseUrl() + "/" + resource + builder.build();
        try {
            Map<String,Object> map = payload instanceof Map ? (Map<String,Object>) payload : objectMapper.convertValue(payload, new TypeReference<Map<String,Object>>(){});
            Map<String,Object> filtered = filter == null ? map : filter.apply(resource, payload, map);
            String json = objectMapper.writeValueAsString(filtered);
            HttpRequestData req = new HttpRequestData();
            req.setUrl(url);
        req.setMethod("PATCH");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Prefer", "return=representation,count=exact");
        req.setBody(json);
        applyProfile(req, true);
        addAuth(req);
            HttpResponseData response = httpExecutor.execute(req);
            return readList(response.getBody(), type);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public int delete(String resource, PgQueryBuilder builder) {
        String url = config.getBaseUrl() + "/" + resource + builder.build();
        HttpRequestData req = new HttpRequestData();
        req.setUrl(url);
        req.setMethod("DELETE");
        req.getHeaders().put("Accept", "application/json");
        req.getHeaders().put("Prefer", "count=exact");
        applyProfile(req, true);
        addAuth(req);
        try {
            HttpResponseData response = httpExecutor.execute(req);
            String contentRange = getHeader(response.getHeaders(), "Content-Range", "items */0");
            long total = parseTotal(contentRange);
            return (int) total;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void addAuth(HttpRequestData req) {
        if (!config.isAuthEnabled()) return;
        String token = null;
        if (authTokenSupplier != null) token = authTokenSupplier.get();
        else {
            String s = config.getSecret();
            String js = config.getJwtSecret();
            if ((s == null || s.isBlank()) && (js == null || js.isBlank())) return;
            token = issueJwt(null);
        }
        if (token != null && !token.isBlank()) req.getHeaders().put("Authorization", "Bearer " + token);
    }

    private void applyProfile(HttpRequestData req, boolean write) {
        String p = write ? writeProfile : readProfile;
        if (p == null || p.isBlank()) return;
        req.getHeaders().put(write ? "Content-Profile" : "Accept-Profile", p);
    }

    private String getHeader(Map<String,String> headers, String name, String def) {
        if (headers == null) return def;
        for (Map.Entry<String,String> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) return e.getValue() == null ? def : e.getValue();
        }
        return def;
    }

    private void ensure2xx(HttpResponseData response) {
        int s = response.getStatus();
        if (s < 200 || s >= 300) throw new RuntimeException("HTTP " + s + ": " + (response.getBody() == null ? "" : response.getBody()));
    }

    private <T> List<T> readList(String body, Class<T> type) {
        String s = body == null ? "" : body.trim();
        if (s.isEmpty()) return java.util.Collections.emptyList();
        if (s.startsWith("[")) {
            try { return objectMapper.readValue(body, objectMapper.getTypeFactory().constructCollectionType(List.class, type)); } catch (Exception e) { throw new RuntimeException(e); }
        }
        if (s.startsWith("{")) {
            try {
                T obj = objectMapper.readValue(body, type);
                java.util.List<T> l = new java.util.ArrayList<>(1);
                l.add(obj);
                return l;
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        return java.util.Collections.emptyList();
    }
}
