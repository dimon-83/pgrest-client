package com.github.pgrest.client.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.PgQueryBuilder;
import com.github.pgrest.client.PgRestClient;
import com.github.pgrest.client.PgRestProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PgRestFeignIT {
    static HttpServer postgrest;
    static int postgrestPort;

    @LocalServerPort
    int gatewayPort;

    @Autowired
    PgRestGatewayFeignClient gatewayFeign;

    @BeforeAll
    static void startPostgrest() throws IOException {
        postgrest = HttpServer.create(new InetSocketAddress(0), 0);
        postgrestPort = postgrest.getAddress().getPort();
        postgrest.createContext("/users", PgRestFeignIT::handleUsers);
        postgrest.start();
    }

    @AfterAll
    static void stopPostgrest() {
        if (postgrest != null) postgrest.stop(0);
    }

    static void handleUsers(HttpExchange ex) throws IOException {
        URI uri = ex.getRequestURI();
        String query = uri.getRawQuery() == null ? "" : uri.getRawQuery();
        String method = ex.getRequestMethod();
        List<Map<String,Object>> body = new ArrayList<>();
        String contentRange = "items */0";
        if ("GET".equalsIgnoreCase(method)) {
            int limit = getInt(query, "limit", 2);
            int offset = getInt(query, "offset", 0);
            for (int i = 0; i < limit; i++) {
                int id = offset + i + 1;
                Map<String,Object> row = new HashMap<>();
                row.put("id", id);
                row.put("user_name", "user" + id);
                body.add(row);
            }
            int end = offset + limit - 1;
            contentRange = "items " + offset + "-" + end + "/100";
        }
        byte[] json = new ObjectMapper().writeValueAsBytes(body);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.getResponseHeaders().add("Content-Range", contentRange);
        ex.sendResponseHeaders(200, json.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(json); }
    }

    static int getInt(String query, String key, int def) {
        for (String p : query.split("&")) {
            int i = p.indexOf('=');
            if (i > 0) {
                String k = p.substring(0, i);
                String v = p.substring(i + 1);
                if (k.equals(key)) { try { return Integer.parseInt(v); } catch (Exception ignored) {} }
            }
        }
        return def;
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("pgrest.base-url", () -> "http://127.0.0.1:" + postgrestPort);
        r.add("pgrest.gateway.enabled", () -> "true");
        r.add("pgrest.gateway-service-name", () -> "test-gateway");
        r.add("spring.application.name", () -> "pgrest-feign-it");
    }

    @TestConfiguration
    static class LBConfig {
        @Bean
        @Primary
        ServiceInstanceListSupplier gatewaySupplier(@LocalServerPort int port) {
            ServiceInstance inst = new DefaultServiceInstance("test-gateway-1", "test-gateway", "127.0.0.1", port, false);
            return new StaticSupplier("test-gateway", List.of(inst));
        }
    }

    static class StaticSupplier implements ServiceInstanceListSupplier {
        private final String serviceId;
        private final List<ServiceInstance> instances;
        StaticSupplier(String serviceId, List<ServiceInstance> instances) { this.serviceId = serviceId; this.instances = instances; }
        @Override public String getServiceId() { return serviceId; }
        @Override public Flux<List<ServiceInstance>> get() { return Flux.just(instances); }
    }

    @Test
    void testGatewayList() {
        var resp = gatewayFeign.list("users", Map.of("limit","2","offset","0"));
        assertNotNull(resp.getBody());
        assertEquals(2, resp.getBody().size());
        assertEquals("user1", resp.getBody().get(0).get("user_name"));
    }

    @Test
    void testGatewayPageHeaders() {
        var resp = gatewayFeign.list("users", Map.of("order","id.asc","limit","3","offset","3"), "3-5", "items");
        assertEquals(3, Objects.requireNonNull(resp.getBody()).size());
        String cr = resp.getHeaders().getFirst("Content-Range");
        assertEquals("items 3-5/100", cr);
    }
}