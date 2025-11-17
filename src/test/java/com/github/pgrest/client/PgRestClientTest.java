package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import static org.junit.jupiter.api.Assertions.*;

public class PgRestClientTest {
    private HttpServer server;
    private int port;

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/users", this::handleUsers);
        server.createContext("/rpc/echo", this::handleRpcEcho);
        server.createContext("/rpc/top_orders", this::handleRpcTopOrders);
        server.start();
    }

    @AfterEach
    void teardown() {
        if (server != null) server.stop(0);
    }

    private void handleRpcEcho(HttpExchange ex) throws IOException {
        Map<String,Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("user_name", "alice");
        byte[] json = new ObjectMapper().writeValueAsBytes(row);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, json.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(json); }
    }

    private void handleRpcTopOrders(HttpExchange ex) throws IOException {
        List<Map<String,Object>> body = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Map<String,Object> row = new HashMap<>();
            row.put("id", i + 1);
            row.put("user_name", "user" + (i + 1));
            body.add(row);
        }
        byte[] json = new ObjectMapper().writeValueAsBytes(body);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, json.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(json); }
    }

    private void handleUsers(HttpExchange ex) throws IOException {
        URI uri = ex.getRequestURI();
        String query = uri.getRawQuery() == null ? "" : uri.getRawQuery();
        String method = ex.getRequestMethod();

        List<Map<String,Object>> body = new ArrayList<>();
        String contentRange = "items */0";

        if ("GET".equalsIgnoreCase(method)) {
            int limit = getInt(query, "limit", 1);
            int offset = getInt(query, "offset", 0);
            for (int i = 0; i < limit; i++) {
                int id = offset + i + 1;
                Map<String,Object> row = new HashMap<>();
                row.put("id", id);
                row.put("user_name", "user" + id);
                body.add(row);
            }
            int end = offset + limit - 1;
            contentRange = "items " + offset + "-" + end + "/42";
        } else if ("POST".equalsIgnoreCase(method)) {
            Map<String,Object> row = new HashMap<>();
            row.put("id", 1);
            row.put("user_name", "alice");
            body.add(row);
            contentRange = "items */1";
        } else if ("PATCH".equalsIgnoreCase(method)) {
            Map<String,Object> row = new HashMap<>();
            row.put("id", 1);
            row.put("user_name", "alice");
            row.put("status", "active");
            body.add(row);
            contentRange = "items */1";
        } else if ("DELETE".equalsIgnoreCase(method)) {
            contentRange = "items */3";
        }

        byte[] json = new ObjectMapper().writeValueAsBytes(body);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.getResponseHeaders().add("Content-Range", contentRange);
        ex.sendResponseHeaders(200, json.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(json);
        }
    }

    private int getInt(String query, String key, int def) {
        for (String p : query.split("&")) {
            int i = p.indexOf('=');
            if (i > 0) {
                String k = p.substring(0, i);
                String v = p.substring(i + 1);
                if (k.equals(key)) {
                    try { return Integer.parseInt(v); } catch (Exception ignored) {}
                }
            }
        }
        return def;
    }

    private PgRestClient newClient() {
        PgRestProperties props = new PgRestProperties();
        props.setBaseUrl("http://127.0.0.1:" + port);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        return new PgRestClient(props, httpClient, mapper);
    }

    public static class UserVO {
        public Long id;
        public String userName;
    }

    @Test
    void testList() {
        PgRestClient client = newClient();
        List<UserVO> users = client.list("users", new PgQueryBuilder().limit(2), UserVO.class);
        assertEquals(2, users.size());
        assertEquals("user1", users.get(0).userName);
    }

    @Test
    void testPage() {
        PgRestClient client = newClient();
        PageResult<UserVO> page = client.page("users", new PgQueryBuilder().orderAsc("id"), 2, 3, UserVO.class);
        assertEquals(2, page.getPage());
        assertEquals(3, page.getSize());
        assertEquals(42, page.getTotal());
        assertEquals(3, page.getRecords().size());
        assertEquals("user4", page.getRecords().get(0).userName);
    }

    @Test
    void testGetById() {
        PgRestClient client = newClient();
        UserVO one = client.getById("users", 1, UserVO.class);
        assertNotNull(one);
        assertEquals(1L, one.id);
        assertEquals("user1", one.userName);
    }

    @Test
    void testRpcForObject() {
        PgRestClient client = newClient();
        UserVO vo = client.rpcForObject("echo", Map.of("any", "param"), UserVO.class);
        assertNotNull(vo);
        assertEquals(1L, vo.id);
        assertEquals("alice", vo.userName);
    }

    @Test
    void testRpcForList() {
        PgRestClient client = newClient();
        List<UserVO> list = client.rpcForList("top_orders", Map.of("limit_arg", 2), UserVO.class);
        assertEquals(2, list.size());
        assertEquals("user1", list.get(0).userName);
    }

    @Test
    void testIssueJwtDefault() throws Exception {
        PgRestProperties props = new PgRestProperties();
        props.setJwtSecret("this_is_a_very_long_32_bytes_secret_key__");
        props.setJwtTtlSeconds(60);
        props.setDbRole("api_user");
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        PgRestClient client = new PgRestClient(props, httpClient, mapper);
        String jwt = client.issueJwt(null);
        assertNotNull(jwt);
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length);
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Map<?,?> payload = new ObjectMapper().readValue(payloadJson, Map.class);
       
        assertEquals("test", payload.get("user"));
        assertEquals("api_user", payload.get("role"));
    }

    @Test
    void testIssueJwtHandlerMapping() throws Exception {
        PgRestProperties props = new PgRestProperties();
        props.setJwtSecret("this_is_a_very_long_32_bytes_secret_key__");
        props.setJwtTtlSeconds(60);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        PgRestClient client = new PgRestClient(props, httpClient, mapper);
        client.setClaimsHandler(c -> {
            Object t = c.remove("tenant");
            if (t != null) c.put("tenant_id", t);
            Object d = c.remove("dept");
            if (d != null) c.put("dept_id", d);
            c.remove("userName");
            return c;
        });
        Map<String,Object> claims = new HashMap<>();
        claims.put("tenant", "foo");
        claims.put("dept", "bar");
        claims.put("userName", "dexter");
        String jwt = client.issueJwt(claims);
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
        Map<?,?> payload = new ObjectMapper().readValue(payloadJson, Map.class);
        assertEquals(false,payload.containsKey("userName"));
        assertEquals("foo", payload.get("tenant_id"));
        assertEquals("bar", payload.get("dept_id"));
    }

    @Test
    void testIssueJwtCustomClaims() throws Exception {
        PgRestProperties props = new PgRestProperties();
        props.setSecret("@" + java.util.Base64.getEncoder().encodeToString("this_is_a_very_long_32_bytes_secret_key__".getBytes(StandardCharsets.UTF_8)));
        props.setJwtTtlSeconds(120);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        PgRestClient client = new PgRestClient(props, httpClient, mapper);
        client.setClaimsHandler(c -> { c.put("scope", "admin"); c.put("username", "bob"); return c; });
        Map<String,Object> claims = new HashMap<>();
        claims.put("user_id", "u-001");
        claims.put("username", "alice");
        claims.put("tenant_id", "t-01");
        claims.put("dept_id", "d-02");
        String jwt = client.issueJwt(claims);
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
        Map<?,?> payload = new ObjectMapper().readValue(payloadJson, Map.class);
        assertEquals("bob", payload.get("username"));
        assertEquals("u-001", payload.get("user_id"));
        assertEquals("t-01", payload.get("tenant_id"));
        assertEquals("d-02", payload.get("dept_id"));
        assertEquals("admin", payload.get("scope"));
    }
}