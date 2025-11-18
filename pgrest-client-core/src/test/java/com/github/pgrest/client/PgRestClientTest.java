package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.pgrest.client.http.JdkHttpExecutor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PgRestClientTest {
    HttpServer server;
    int port;

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
    void tearDown() {
        server.stop(0);
    }

    private void handleUsers(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        if ("GET".equals(method)) {
            List<Map<String,Object>> body = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Map<String,Object> row = new HashMap<>();
                row.put("id", i + 1);
                row.put("user_name", "user" + (i + 1));
                body.add(row);
            }
            byte[] json = new ObjectMapper().writeValueAsBytes(body);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.getResponseHeaders().add("Content-Range", "items 0-2/3");
            ex.sendResponseHeaders(200, json.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(json); }
        } else if ("POST".equals(method)) {
            byte[] resp = "[]".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(201, resp.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(resp); }
        } else if ("PATCH".equals(method)) {
            byte[] resp = "[]".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, resp.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(resp); }
        } else if ("DELETE".equals(method)) {
            ex.getResponseHeaders().add("Content-Range", "items 0-0/1");
            ex.sendResponseHeaders(204, -1);
        } else {
            ex.sendResponseHeaders(405, -1);
        }
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

    private PgRestClient newClient() {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl("http://127.0.0.1:" + port);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        return new PgRestClient(cfg, new JdkHttpExecutor(httpClient), mapper);
    }

    static class UserVO { public Long id; public String userName; }

    @Test
    void testPage() {
        PgRestClient client = newClient();
        PageResult<UserVO> pr = client.page("users", new PgQueryBuilder().orderAsc("id"), 1, 3, UserVO.class);
        assertEquals(3, pr.getItems().size());
        assertEquals(3, pr.getTotal());
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
        PgClientConfig cfg = new PgClientConfig();
        cfg.setJwtSecret("this_is_a_very_long_32_bytes_secret_key__");
        cfg.setJwtTtlSeconds(60);
        cfg.setDbRole("api_user");
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        PgRestClient client = new PgRestClient(cfg, new JdkHttpExecutor(httpClient), mapper);
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
        PgClientConfig cfg = new PgClientConfig();
        cfg.setJwtSecret("this_is_a_very_long_32_bytes_secret_key__");
        cfg.setJwtTtlSeconds(60);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        PgRestClient client = new PgRestClient(cfg, new JdkHttpExecutor(httpClient), mapper);
        client.setClaimsHandler(c -> { Object t = c.remove("tenant"); if (t != null) c.put("tenant_id", t); Object d = c.remove("dept"); if (d != null) c.put("dept_id", d); return c; });
        Map<String,Object> claims = new HashMap<>();
        claims.put("tenant", "foo"); claims.put("dept", "bar"); claims.put("userName", "dexter");
        String jwt = client.issueJwt(claims);
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
        Map<?,?> payload = new ObjectMapper().readValue(payloadJson, Map.class);
        assertEquals("dexter", payload.get("userName"));
        assertEquals("foo", payload.get("tenant_id"));
        assertEquals("bar", payload.get("dept_id"));
    }
}