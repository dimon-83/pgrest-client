package com.github.pgrest.client.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.pgrest.client.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PgRestTypedClientTest {
    static class UserVO {
        public Long id;
        public String userName;
        public String status;
    }

    static class StubGateway implements PgRestGatewayFeignClient {
        String lastRange;
        Map<String,String> lastQuery;
        @Override
        public ResponseEntity<List<Map<String, Object>>> list(String resource, Map<String, String> query) {
            return list(resource, query, null, "items");
        }
        @Override
        public ResponseEntity<List<Map<String, Object>>> list(String resource, Map<String, String> query, String range, String rangeUnit) {
            lastRange = range;
            lastQuery = query;
            int limit = Integer.parseInt(query.getOrDefault("limit", "10"));
            int offset = Integer.parseInt(query.getOrDefault("offset", "0"));
            List<Map<String,Object>> body = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                int id = offset + i + 1;
                Map<String,Object> row = new HashMap<>();
                row.put("id", id);
                row.put("user_name", "user" + id);
                body.add(row);
            }
            int end = offset + limit - 1;
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Range", "items " + offset + "-" + end + "/42");
            return ResponseEntity.ok().headers(h).body(body);
        }
        @Override
        public ResponseEntity<List<Map<String, Object>>> insert(String resource, Object payload, Map<String, String> query) {
            Map<String,Object> row = new HashMap<>();
            row.put("id", 1);
            row.put("user_name", "alice");
            return ResponseEntity.ok(List.of(row));
        }
        @Override
        public ResponseEntity<List<Map<String, Object>>> update(String resource, Object payload, Map<String, String> query) {
            Map<String,Object> row = new HashMap<>();
            row.put("id", 1);
            row.put("user_name", "alice");
            row.put("status", "active");
            return ResponseEntity.ok(List.of(row));
        }
        @Override
        public ResponseEntity<Integer> delete(String resource, Map<String, String> query) {
            return ResponseEntity.ok(3);
        }
    }

    static class StubDirect implements PgRestFeignClient {
        String lastRange;
        Map<String,String> lastQuery;
        @Override
        public ResponseEntity<List<Map<String, Object>>> list(String resource, Map<String, String> query) {
            int limit = Integer.parseInt(query.getOrDefault("limit", "10"));
            int offset = Integer.parseInt(query.getOrDefault("offset", "0"));
            List<Map<String,Object>> body = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                int id = offset + i + 1;
                Map<String,Object> row = new HashMap<>();
                row.put("id", id);
                row.put("user_name", "user" + id);
                body.add(row);
            }
            return ResponseEntity.ok(body);
        }
        @Override
        public ResponseEntity<List<Map<String, Object>>> list(String resource, Map<String, String> query, String range, String rangeUnit) {
            lastRange = range;
            lastQuery = query;
            int limit = Integer.parseInt(query.getOrDefault("limit", "10"));
            int offset = Integer.parseInt(query.getOrDefault("offset", "0"));
            List<Map<String,Object>> body = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                int id = offset + i + 1;
                Map<String,Object> row = new HashMap<>();
                row.put("id", id);
                row.put("user_name", "user" + id);
                body.add(row);
            }
            int end = offset + limit - 1;
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Range", "items " + offset + "-" + end + "/50");
            return ResponseEntity.ok().headers(h).body(body);
        }
        @Override
        public ResponseEntity<List<Map<String, Object>>> insert(String resource, Object payload, Map<String, String> query) {
            Map<String,Object> row = new HashMap<>();
            row.put("id", 2);
            row.put("user_name", "bob");
            return ResponseEntity.ok(List.of(row));
        }
        @Override
        public ResponseEntity<List<Map<String, Object>>> update(String resource, Object payload, Map<String, String> query) {
            Map<String,Object> row = new HashMap<>();
            row.put("id", 2);
            row.put("user_name", "bob");
            row.put("status", "active");
            return ResponseEntity.ok(List.of(row));
        }
        @Override
        public ResponseEntity<Void> delete(String resource, Map<String, String> query) {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Range", "items */5");
            return ResponseEntity.ok().headers(h).build();
        }
    }

    private ObjectMapper mapper() {
        ObjectMapper m = new ObjectMapper();
        m.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return m;
    }

    @Test
    void testListGateway() {
        StubGateway gw = new StubGateway();
        PgRestTypedClient typed = new PgRestTypedClient(gw, null, mapper());
        List<UserVO> users = typed.list("users", Map.of("limit","2","offset","0"), UserVO.class);
        assertEquals(2, users.size());
        assertEquals("user1", users.get(0).userName);
    }

    @Test
    void testInsertUpdateGateway() {
        StubGateway gw = new StubGateway();
        PgRestTypedClient typed = new PgRestTypedClient(gw, null, mapper());
        List<UserVO> ins = typed.insert("users", Map.of("user_name","alice"), Map.of(), UserVO.class);
        assertEquals(1, ins.size());
        assertEquals("alice", ins.get(0).userName);
        List<UserVO> upd = typed.update("users", Map.of("status","active"), Map.of("id","eq.1"), UserVO.class);
        assertEquals("active", upd.get(0).status);
    }

    @Test
    void testDeleteGateway() {
        StubGateway gw = new StubGateway();
        PgRestTypedClient typed = new PgRestTypedClient(gw, null, mapper());
        int n = typed.delete("users", Map.of("id","eq.1"));
        assertEquals(3, n);
    }

    @Test
    void testDeleteDirect() {
        StubDirect direct = new StubDirect();
        PgRestTypedClient typed = new PgRestTypedClient(null, direct, mapper());
        int n = typed.delete("users", Map.of("id","eq.1"));
        assertEquals(5, n);
    }

    @Test
    void testPageGateway() {
        StubGateway gw = new StubGateway();
        PgRestTypedClient typed = new PgRestTypedClient(gw, null, mapper());
        PageResult<UserVO> page = typed.page("users", Map.of("order","id.asc"), 2, 3, UserVO.class);
        assertEquals(2, page.getPage());
        assertEquals(3, page.getSize());
        assertEquals(42, page.getTotal());
        assertEquals("3-5", gw.lastRange == null ? null : gw.lastRange);
        assertEquals("3", gw.lastQuery.get("limit"));
        assertEquals("3", String.valueOf(page.getRecords().size()));
    }

    @Test
    void testPageDirect() {
        StubDirect direct = new StubDirect();
        PgRestTypedClient typed = new PgRestTypedClient(null, direct, mapper());
        PageResult<UserVO> page = typed.page("users", Map.of(), 2, 2, UserVO.class);
        assertEquals(50, page.getTotal());
        assertEquals("2-3", direct.lastRange);
        assertEquals("2", direct.lastQuery.get("limit"));
    }
}