package com.github.pgrest.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PgRestClient {
    private final PgRestProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private java.util.function.UnaryOperator<Map<String,Object>> claimsHandler;
    private Supplier<String> authTokenSupplier;

    public PgRestClient(PgRestProperties properties, HttpClient httpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public <T> List<T> list(String resource, PgQueryBuilder builder, Class<T> type) {
        String url = properties.getBaseUrl() + "/" + resource + builder.build();
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .GET();
        addAuth(rb);
        HttpRequest request = rb.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> PageResult<T> page(String resource, PgQueryBuilder builder, int page, int size, Class<T> type) {
        int offset = (page - 1) * size;
        PgQueryBuilder qb = builder.copy().limit(size).offset(offset);
        String url = properties.getBaseUrl() + "/" + resource + qb.build();
        HttpRequest.Builder rb2 = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .GET();
        addAuth(rb2);
        HttpRequest request = rb2.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            List<T> list = objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
            String contentRange = response.headers().firstValue("Content-Range").orElse("items */0");
            long total = parseTotal(contentRange);
            return new PageResult<>(page, size, total, list);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> get(String resource, PgQueryBuilder builder) {
        String url = properties.getBaseUrl() + "/" + resource + builder.build();
        HttpRequest.Builder rb3 = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .GET();
        addAuth(rb3);
        HttpRequest request = rb3.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>(){});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T getById(String resource, Object id, Class<T> type) {
        return getById(resource, "id", id, new PgQueryBuilder(), type);
    }

    public <T> T getById(String resource, String idColumn, Object id, PgQueryBuilder builder, Class<T> type) {
        PgQueryBuilder qb = builder.copy().eq(idColumn, id).limit(1);
        String url = properties.getBaseUrl() + "/" + resource + qb.build();
        HttpRequest.Builder rb4 = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .GET();
        addAuth(rb4);
        HttpRequest request = rb4.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            List<T> list = objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
            return list == null || list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long parseTotal(String contentRange) {
        int slash = contentRange.lastIndexOf('/');
        if (slash >= 0) {
            String totalStr = contentRange.substring(slash + 1).trim();
            try {
                return Long.parseLong(totalStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

  

    public String issueJwt(Map<String,Object> claims) {
        Map<String,Object> payload = new java.util.HashMap<>();
        if (claims != null) payload.putAll(claims);
        if (claimsHandler != null) payload = claimsHandler.apply(payload);
        else payload = java.util.function.UnaryOperator.<Map<String,Object>>identity()
                .andThen(c -> { c.put("user", "test"); return c; })
                .apply(payload);
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) secret = properties.getJwtSecret();
        if (secret == null || secret.isBlank()) throw new IllegalStateException("jwt-secret is missing");
        secret = secret.trim();
        byte[] key = secret.startsWith("@") ? java.util.Base64.getDecoder().decode(secret.substring(1)) : secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) throw new IllegalStateException("jwt-secret must be at least 256 bits");
        long nowSec = java.time.Instant.now().getEpochSecond();
        long expSec = nowSec + Math.max(1, properties.getJwtTtlSeconds());
        payload.put("iat", nowSec);
        payload.put("exp", expSec);
        String role = properties.getDbRole();
        if (role != null && !role.isBlank()) payload.put("role", role);
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String sigB64 = enc.encodeToString(sig);
        return signingInput + "." + sigB64;
    }

    public void setClaimsHandler(java.util.function.UnaryOperator<Map<String,Object>> claimsHandler) {
        this.claimsHandler = claimsHandler;
    }

    public void setAuthTokenSupplier(Supplier<String> supplier) {
        this.authTokenSupplier = supplier;
    }

    public void setStaticAuthToken(String token) {
        this.authTokenSupplier = () -> token;
    }

    public <T> List<T> insert(String resource, Object payload, Class<T> type) {
        String url = properties.getBaseUrl() + "/" + resource;
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder rb5 = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation,count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            addAuth(rb5);
            HttpRequest request = rb5.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> insert(String resource, Object payload, PgQueryBuilder builder, Class<T> type) {
        String url = properties.getBaseUrl() + "/" + resource + builder.build();
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder rb6 = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation,count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            addAuth(rb6);
            HttpRequest request = rb6.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T rpcForObject(String function, Object payload, Class<T> type) {
        String url = properties.getBaseUrl() + "/rpc/" + function;
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder rb7 = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            addAuth(rb7);
            HttpRequest request = rb7.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T rpcForObject(String function, Object payload, PgQueryBuilder builder, Class<T> type) {
        String url = properties.getBaseUrl() + "/rpc/" + function + builder.build();
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder rb8 = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            addAuth(rb8);
            HttpRequest request = rb8.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> rpcForList(String function, Object payload, Class<T> type) {
        String url = properties.getBaseUrl() + "/rpc/" + function;
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder rb9 = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            addAuth(rb9);
            HttpRequest request = rb9.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> rpcForList(String function, Object payload, PgQueryBuilder builder, Class<T> type) {
        String url = properties.getBaseUrl() + "/rpc/" + function + builder.build();
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder rb10 = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            addAuth(rb10);
            HttpRequest request = rb10.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> update(String resource, PgQueryBuilder builder, Object payload, Class<T> type) {
        String url = properties.getBaseUrl() + "/" + resource + builder.build();
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder rb11 = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation,count=exact")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            addAuth(rb11);
            HttpRequest request = rb11.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int delete(String resource, PgQueryBuilder builder) {
        String url = properties.getBaseUrl() + "/" + resource + builder.build();
        HttpRequest.Builder rb12 = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .DELETE();
        addAuth(rb12);
        HttpRequest request = rb12.build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String contentRange = response.headers().firstValue("Content-Range").orElse("items */0");
            long total = parseTotal(contentRange);
            return (int) total;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addAuth(HttpRequest.Builder rb) {
        if (!properties.isAuthEnabled()) return;
        String token = null;
        if (authTokenSupplier != null) {
            token = authTokenSupplier.get();
        } else {
            String s = properties.getSecret();
            String js = properties.getJwtSecret();
            if ((s == null || s.isBlank()) && (js == null || js.isBlank())) return;
            token = issueJwt(null);
        }
        if (token != null && !token.isBlank()) {
            rb.header("Authorization", "Bearer " + token);
        }
    }
}
