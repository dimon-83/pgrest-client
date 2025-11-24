package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PgRestClientFilterTest {

    private static class StubHttpExecutor implements HttpExecutor {
        @Override
        public HttpResponseData execute(HttpRequestData request) {
            String body = request.getBody();
            String resp;
            if (body == null || body.isBlank()) resp = "[]";
            else {
                String s = body.trim();
                resp = s.startsWith("[") ? s : ("[" + s + "]");
            }
            HttpResponseData d = new HttpResponseData();
            d.setStatus(200);
            d.setBody(resp);
            return d;
        }
    }

    private PgRestClient newClient() {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl("http://localhost:3000");
        cfg.setAuthEnabled(false);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.registerModule(new JavaTimeModule());
        return new PgRestClient(cfg, new StubHttpExecutor(), mapper);
    }

    @Test
    public void testBlacklistFilterOnMap() {
        PgRestClient client = newClient();
        Map<String,Object> payload = new java.util.HashMap<>();
        payload.put("name", "alice");
        payload.put("extra", "x");
        List<Map> out = client.insert("users", payload, new BlacklistPayloadFieldFilter(Set.of("extra")), Map.class);
        Assertions.assertEquals(1, out.size());
        Map<String,Object> row = out.get(0);
        Assertions.assertEquals("alice", row.get("name"));
        Assertions.assertFalse(row.containsKey("extra"));
    }

    @Test
    public void testWhitelistFilterOnMap() {
        PgRestClient client = newClient();
        Map<String,Object> payload = new java.util.HashMap<>();
        payload.put("name", "bob");
        payload.put("role", "user");
        payload.put("debug", true);
        List<Map> out = client.insert("users", payload, new WhitelistPayloadFieldFilter(Set.of("name","role")), Map.class);
        Assertions.assertEquals(1, out.size());
        Map<String,Object> row = out.get(0);
        Assertions.assertEquals("bob", row.get("name"));
        Assertions.assertEquals("user", row.get("role"));
        Assertions.assertFalse(row.containsKey("debug"));
    }

    public static class BeanPayload {
        @PgIgnore private String debug;
        @PgInclude private String firstName;
        private String lastName;
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getDebug() { return debug; }
        public void setDebug(String debug) { this.debug = debug; }
    }

    @Test
    public void testAnnotationFilterOnBeanSnakeCase() {
        PgRestClient client = newClient();
        BeanPayload p = new BeanPayload();
        p.setFirstName("charlie");
        p.setLastName("brown");
        p.setDebug("trace");
        List<Map> out = client.insert("users", p, new AnnotationPayloadFieldFilter(), Map.class);
        Assertions.assertEquals(1, out.size());
        Map<String,Object> row = out.get(0);
        Assertions.assertEquals("charlie", row.get("first_name"));
        Assertions.assertFalse(row.containsKey("last_name"));
        Assertions.assertFalse(row.containsKey("debug"));
    }

    @Test
    public void testUpdateWithFilter() {
        PgRestClient client = newClient();
        PgQueryBuilder qb = new PgQueryBuilder().eq("id", 1);
        Map<String,Object> payload = java.util.Map.of("name","dora","extra","z");
        List<Map> out = client.update("users", qb, payload, new BlacklistPayloadFieldFilter(Set.of("extra")), Map.class);
        Assertions.assertEquals(1, out.size());
        Map<String,Object> row = out.get(0);
        Assertions.assertEquals("dora", row.get("name"));
        Assertions.assertFalse(row.containsKey("extra"));
    }
}