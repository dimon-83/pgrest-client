package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class PgRestClientHeaderTest {
    static class CapturingExecutor implements HttpExecutor {
        HttpRequestData last;
        @Override
        public HttpResponseData execute(HttpRequestData request) {
            last = request;
            HttpResponseData resp = new HttpResponseData();
            resp.setStatus(200);
            resp.setBody("[]");
            return resp;
        }
    }

    @Test
    public void insertWithPrefer() {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl("http://localhost:3000");
        cfg.setAuthEnabled(false);
        CapturingExecutor ex = new CapturingExecutor();
        PgRestClient client = new PgRestClient(cfg, ex, new ObjectMapper());
        PgPrefer prefer = PgPrefer.create().returnHeadersOnly().countExact();
        client.insert("projects", Map.of("id", 1), prefer, Map.class);
        String h = ex.last.getHeaders().get("Prefer");
        Assertions.assertEquals(prefer.toHeaderValue(), h);
    }

    @Test
    public void upsertMergeSetsResolution() {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl("http://localhost:3000");
        cfg.setAuthEnabled(false);
        CapturingExecutor ex = new CapturingExecutor();
        PgRestClient client = new PgRestClient(cfg, ex, new ObjectMapper());
        PgQueryBuilder qb = new PgQueryBuilder().raw("on_conflict", "email");
        client.upsertMerge("people", Map.of("email", "x@y"), qb, Map.class);
        String h = ex.last.getHeaders().get("Prefer");
        Assertions.assertTrue(h.contains("resolution=merge-duplicates"));
        Assertions.assertTrue(ex.last.getUrl().contains("on_conflict=email"));
    }

    @Test
    public void upsertIgnoreSetsResolution() {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl("http://localhost:3000");
        cfg.setAuthEnabled(false);
        CapturingExecutor ex = new CapturingExecutor();
        PgRestClient client = new PgRestClient(cfg, ex, new ObjectMapper());
        PgQueryBuilder qb = new PgQueryBuilder();
        client.upsertIgnore("orders", Map.of("order_no", "A1"), qb, Map.class);
        String h = ex.last.getHeaders().get("Prefer");
        Assertions.assertTrue(h.contains("resolution=ignore-duplicates"));
    }
}

