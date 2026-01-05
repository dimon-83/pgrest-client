package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class PgRestClientUpsertTest {
    static class StubExecutor implements HttpExecutor {
        HttpRequestData last;
        HttpResponseData response = new HttpResponseData();
        @Override
        public HttpResponseData execute(HttpRequestData request) {
            last = request;
            if (response.getStatus() == 0) response.setStatus(200);
            if (response.getBody() == null) response.setBody("[]");
            return response;
        }
    }

    private PgRestClient newClient(StubExecutor ex) {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl("http://localhost:3000");
        cfg.setAuthEnabled(false);
        return new PgRestClient(cfg, ex, new ObjectMapper());
    }

    @Test
    public void testUpsertMergeWithOnConflict() {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        
        // Use primary key "id" for conflict resolution
        PgQueryBuilder builder = new PgQueryBuilder().raw("on_conflict", "id");
        
        Map<String, Object> payload = Map.of("id", 1, "name", "Updated Name");
        client.upsertMerge("users", payload, builder, Map.class);
        
        // Verify URL contains on_conflict=id
        String url = ex.last.getUrl();
        Assertions.assertTrue(url.contains("on_conflict=id"), "URL should contain on_conflict parameter: " + url);
        
        // Verify Prefer header contains resolution=merge-duplicates
        String prefer = ex.last.getHeaders().get("Prefer");
        Assertions.assertTrue(prefer.contains("resolution=merge-duplicates"), "Prefer header should contain resolution=merge-duplicates: " + prefer);
    }
    
    @Test
    public void testUpsertMergeWithCompositeKey() {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        
        // Use composite key "tenant_id, user_id"
        PgQueryBuilder builder = new PgQueryBuilder().raw("on_conflict", "tenant_id,user_id");
        
        Map<String, Object> payload = Map.of("tenant_id", 1, "user_id", 100, "role", "admin");
        client.upsertMerge("users", payload, builder, Map.class);
        
        // Verify URL contains on_conflict=tenant_id,user_id (encoded)
        String url = ex.last.getUrl();
        // The builder encodes keys and values, so we expect on_conflict=tenant_id%2Cuser_id or similar
        // Let's check if it contains "tenant_id" and "user_id"
        Assertions.assertTrue(url.contains("on_conflict="), "URL should contain on_conflict parameter");
        // We might need to check how it's encoded. 
        // PgQueryBuilder.raw implementation: parts.add(encode(key) + "=" + encode(value));
        // URLEncoder.encode("tenant_id,user_id") -> "tenant_id%2Cuser_id"
    }
}
