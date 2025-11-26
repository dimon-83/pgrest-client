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

public class PgFnTest {

    static class CapturingExecutor implements HttpExecutor {
        HttpRequestData last;
        @Override public HttpResponseData execute(HttpRequestData request) {
            last = request;
            String body = request.getBody();
            String resp = body == null || body.isBlank() ? "[]" : ("[" + body + "]");
            HttpResponseData d = new HttpResponseData();
            d.setStatus(200);
            d.setBody(resp);
            return d;
        }
    }

    private PgRestClient newClient(CapturingExecutor exec) {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl("http://localhost:3000");
        cfg.setAuthEnabled(false);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.registerModule(new JavaTimeModule());
        return new PgRestClient(cfg, exec, mapper);
    }

    @Test
    public void testRpcChainBuildsUrl() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        Map<String,Object> criteria = Map.of("is_active", true);
        List<Map> list = client.fn("search_users").params(criteria).select("id,name,email,created_at").eq("is_active", true).orderDesc("created_at").limit(50).list(Map.class);
        Assertions.assertNotNull(list);
        String url = exec.last.getUrl();
        Assertions.assertTrue(url.startsWith("http://localhost:3000/rpc/search_users?"));
        Assertions.assertTrue(url.contains("select=id%2Cname%2Cemail%2Ccreated_at"));
        Assertions.assertTrue(url.contains("is_active=eq.true"));
        Assertions.assertTrue(url.contains("order=created_at.desc"));
        Assertions.assertTrue(url.contains("limit=50"));
        Assertions.assertEquals("POST", exec.last.getMethod());
    }

    @Test
    public void testRpcVoidInvoke() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        Map<String,Object> payload = Map.of("payload", Map.of("N","sensor_001","V","12.34","T","2025-11-25T10:00:00Z","Q","1","Err",""));
        client.fn("ai_insert_ia_node_value").params(payload).invokeVoid();
        Assertions.assertEquals("POST", exec.last.getMethod());
        Assertions.assertTrue(exec.last.getUrl().endsWith("/rpc/ai_insert_ia_node_value"));
        Assertions.assertTrue(exec.last.getBody().contains("sensor_001"));
    }

    @Test
    public void testRpcProfileHeaderApplied() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        client.setProfile("ia_csc").fn("upsert_ia_node_value").params(Map.of("payload", Map.of("N","n"))).list(Map.class);
        String profile = exec.last.getHeaders().get("Content-Profile");
        Assertions.assertEquals("ia_csc", profile);
    }
}
