package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class PgRestClientPreferCoverageTest {
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
    public void getAndGetByIdPrefer() {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        PgPrefer prefer = PgPrefer.create().countExact().returnHeadersOnly();

        ex.response.setBody("{}");
        client.get("projects", new PgQueryBuilder(), prefer);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));

        ex.response.setBody("[{\"id\":1}]");
        client.getById("projects", "id", 1, new PgQueryBuilder(), prefer, Map.class);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));
    }

    @Test
    public void pageParsesTotal() {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        ex.response.getHeaders().put("Content-Range", "items */42");
        PageResult<Map> pr = client.page("items", new PgQueryBuilder(), 2, 10, Map.class);
        Assertions.assertEquals(42, pr.getTotal());
        Assertions.assertEquals(2, pr.getPage());
        Assertions.assertEquals(10, pr.getSize());
    }

    @Test
    public void deletePreferAndTotal() {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        PgPrefer prefer = PgPrefer.create().countExact().handlingLenient();
        ex.response.getHeaders().put("Content-Range", "items */5");
        int n = client.delete("items", new PgQueryBuilder(), prefer);
        Assertions.assertEquals(5, n);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));
    }

    @Test
    public void rpcPreferObjectAndList() {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        PgPrefer prefer = PgPrefer.create().countExact().timezone("America/Los_Angeles");

        ex.response.setBody("{\"ok\":true}");
        Map obj = client.rpcForObject("do", Map.of("a",1), prefer, Map.class);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));
        Assertions.assertEquals(true, obj.get("ok"));

        ex.response.setBody("[{\"id\":1}]\n");
        java.util.List<Map> list = client.rpcForList("list", Map.of("a",1), prefer, Map.class);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));
        Assertions.assertFalse(list.isEmpty());
    }

    @Test
    public void insertUpdateWithFilterAndPrefer() throws Exception {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        PgPrefer prefer = PgPrefer.create().returnRepresentation().countExact().missingDefault();

        PayloadFieldFilter filter = new WhitelistPayloadFieldFilter(java.util.Set.of("id"));
        client.insert("projects", Map.of("id",1,"name","x"), new PgQueryBuilder(), filter, prefer, Map.class);
        String ibody = ex.last.getBody();
        Map<String,Object> imap = new ObjectMapper().readValue(ibody, Map.class);
        Assertions.assertTrue(imap.containsKey("id") && imap.size() == 1);
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));

        PayloadFieldFilter bl = new BlacklistPayloadFieldFilter(java.util.Set.of("secret"));
        client.update("secrets", new PgQueryBuilder(), Map.of("secret","s","name","n"), bl, prefer, Map.class);
        String ubody = ex.last.getBody();
        Map<String,Object> umap = new ObjectMapper().readValue(ubody, Map.class);
        Assertions.assertFalse(umap.containsKey("secret"));
        Assertions.assertEquals(prefer.toHeaderValue(), ex.last.getHeaders().get("Prefer"));
    }

    @Test
    public void ensure2xxThrowsOnError() {
        StubExecutor ex = new StubExecutor();
        PgRestClient client = newClient(ex);
        ex.response.setStatus(400);
        ex.response.setBody("{\\\"error\\\":\\\"bad\\\"}");
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> client.list("items", new PgQueryBuilder(), Map.class));
        Assertions.assertTrue(thrown.getMessage().contains("HTTP 400"));
    }
}
