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

public class PgRestClient {
    private final PgRestProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PgRestClient(PgRestProperties properties, HttpClient httpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public <T> List<T> list(String resource, PgQueryBuilder builder, Class<T> type) {
        String url = properties.getBaseUrl() + "/" + resource + builder.build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .GET()
                .build();
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
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .GET()
                .build();
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
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .GET()
                .build();
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
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .GET()
                .build();
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

    public <T> List<T> insert(String resource, Object payload, Class<T> type) {
        String url = properties.getBaseUrl() + "/" + resource;
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation,count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
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
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation,count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
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
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
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
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
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
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
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
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "count=exact")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
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
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation,count=exact")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readValue(response.body(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int delete(String resource, PgQueryBuilder builder) {
        String url = properties.getBaseUrl() + "/" + resource + builder.build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Prefer", "count=exact")
                .DELETE()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String contentRange = response.headers().firstValue("Content-Range").orElse("items */0");
            long total = parseTotal(contentRange);
            return (int) total;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
