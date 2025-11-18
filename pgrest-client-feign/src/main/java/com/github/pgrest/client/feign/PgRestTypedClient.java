package com.github.pgrest.client.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.PageResult;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;

public class PgRestTypedClient {
    private final PgRestFeignClient gateway;
    private final PgRestDirectFeignClient direct;
    private final ObjectMapper objectMapper;

    public PgRestTypedClient(PgRestFeignClient gateway, PgRestDirectFeignClient direct, ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.direct = direct;
        this.objectMapper = objectMapper;
    }

    private boolean useGateway() { return gateway != null; }

    public <T> List<T> list(String resource, Map<String,String> query, Class<T> type) {
        ResponseEntity<List<Map<String,Object>>> resp = useGateway() ? gateway.list(resource, query) : direct.list(resource, query);
        List<Map<String,Object>> body = resp.getBody();
        return objectMapper.convertValue(body, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    public <T> List<T> insert(String resource, Object payload, Map<String,String> query, Class<T> type) {
        ResponseEntity<List<Map<String,Object>>> resp = useGateway() ? gateway.insert(resource, payload, query) : direct.insert(resource, payload, query);
        List<Map<String,Object>> body = resp.getBody();
        return objectMapper.convertValue(body, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    public <T> List<T> update(String resource, Object payload, Map<String,String> query, Class<T> type) {
        ResponseEntity<List<Map<String,Object>>> resp = useGateway() ? gateway.update(resource, payload, query) : direct.update(resource, payload, query);
        List<Map<String,Object>> body = resp.getBody();
        return objectMapper.convertValue(body, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    public int delete(String resource, Map<String,String> query) {
        if (useGateway()) {
            ResponseEntity<Integer> resp = gateway.delete(resource, query);
            Integer v = resp.getBody();
            return v == null ? 0 : v;
        } else {
            ResponseEntity<Void> resp = direct.delete(resource, query);
            String cr = resp.getHeaders().getFirst("Content-Range");
            if (cr == null) return 0;
            int i = cr.lastIndexOf('/');
            if (i >= 0) {
                try { return Integer.parseInt(cr.substring(i + 1).trim()); } catch (Exception ignored) {}
            }
            return 0;
        }
    }

    public <T> PageResult<T> page(String resource, Map<String,String> query, int page, int size, Class<T> type) {
        if (useGateway()) {
            java.util.Map<String,String> q = new java.util.HashMap<>(query == null ? java.util.Collections.emptyMap() : query);
            q.remove("page");
            q.remove("size");
            ResponseEntity<com.github.pgrest.client.PageResult<Map<String,Object>>> resp = gateway.page(resource, q, page, size);
            com.github.pgrest.client.PageResult<Map<String,Object>> pr = resp.getBody();
            java.util.List<Map<String,Object>> items = pr == null ? java.util.Collections.emptyList() : pr.getItems();
            java.util.List<T> list = objectMapper.convertValue(items, objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, type));
            long total = pr == null ? 0 : pr.getTotal();
            return new com.github.pgrest.client.PageResult<>(page, size, total, list);
        } else {
            int offset = (page - 1) * size;
            java.util.Map<String,String> q = new java.util.HashMap<>(query == null ? java.util.Collections.emptyMap() : query);
            q.put("limit", String.valueOf(size));
            q.put("offset", String.valueOf(offset));
            String range = offset + "-" + (offset + size - 1);
            ResponseEntity<java.util.List<java.util.Map<String,Object>>> resp = direct.list(resource, q, range, "items");
            java.util.List<java.util.Map<String,Object>> body = resp.getBody();
            java.util.List<T> list = objectMapper.convertValue(body, objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, type));
            String contentRange = resp.getHeaders().getFirst("Content-Range");
            long total = 0;
            if (contentRange != null) {
                int slash = contentRange.lastIndexOf('/');
                if (slash >= 0) {
                    try { total = Long.parseLong(contentRange.substring(slash + 1).trim()); } catch (Exception ignored) {}
                }
            }
            return new com.github.pgrest.client.PageResult<>(page, size, total, list);
        }
    }
}