package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class PgFromBehaviorTest {
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
    public void preferAppliedAcrossOperations() {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        PgPrefer prefer = PgPrefer.create().handlingStrict().countExact();

        client.from("items").prefer(prefer).list(Map.class);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));

        client.from("items").prefer(prefer).page(1, 10, Map.class);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));

        client.from("items").prefer(prefer).insert(Map.of("id", 1), Map.class);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));

        client.from("items").prefer(prefer).update(Map.of("name", "n"), Map.class);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));

        client.from("items").prefer(prefer).delete();
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));
    }

    @Test
    public void onConflictAddsQueryParam() {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        client.from("people").onConflict("email").insert(Map.of("email", "a@b"), Map.class);
        Assertions.assertTrue(ex.last.getUrl().contains("on_conflict=email"));
    }
}

