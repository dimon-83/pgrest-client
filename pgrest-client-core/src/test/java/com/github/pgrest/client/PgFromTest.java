package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PgFromTest {

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
    public void testListChainBuildsUrl() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        List<Map> list = client.from("users").select("id,name").eq("role","user").orderAsc("id").groupBy("role").limit(5).list(Map.class);
        Assertions.assertNotNull(list);
        String url = exec.last.getUrl();
        Assertions.assertTrue(url.startsWith("http://localhost:3000/users?"));
        Assertions.assertTrue(url.contains("select=id%2Cname"));
        Assertions.assertTrue(url.contains("role=eq.user"));
        Assertions.assertTrue(url.contains("order=id.asc"));
        Assertions.assertTrue(url.contains("group=role"));
        Assertions.assertTrue(url.contains("limit=5"));
    }

    @Test
    public void testInsertWithFilter() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        Map<String,Object> payload = Map.of("name","alice","debug","x");
        List<Map> out = client.from("users").withFilter(new BlacklistPayloadFieldFilter(Set.of("debug"))).insert(payload, Map.class);
        Assertions.assertEquals(1, out.size());
        String body = exec.last.getBody();
        Assertions.assertTrue(body.contains("\"name\":"));
        Assertions.assertFalse(body.contains("\"debug\":"));
    }

    @Test
    public void testPreferHeaderAppliedOnListAndDelete() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        PgPrefer prefer = PgPrefer.create().countExact().timezone("UTC").returnRepresentation();

        client.from("users").prefer(prefer).select("id").list(Map.class);
        Assertions.assertEquals(prefer.toHeaderValue(), exec.last.getHeaders().get("Prefer"));

        client.from("users").prefer(prefer).eq("id",1).delete();
        Assertions.assertEquals(prefer.toHeaderValue(), exec.last.getHeaders().get("Prefer"));
    }

    @Test
    public void testOnConflictInInsertUrl() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        Map<String,Object> payload = Map.of("id",1,"name","x");
        client.from("users").onConflict("id","name").insert(payload, Map.class);
        String url = exec.last.getUrl();
        Assertions.assertTrue(url.contains("on_conflict=id%2Cname"));
    }

    @Test
    public void testSingleAddsLimitOne() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        client.from("users").eq("id", 1).single(Map.class);
        Assertions.assertTrue(exec.last.getUrl().contains("limit=1"));
    }

    @Test
    public void testOffsetAndInNullChecks() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        client.from("users").in("id", java.util.List.of(1,2)).isNull("deleted_at").notNull("created_at").offset(10).list(Map.class);
        String url = exec.last.getUrl();
        Assertions.assertTrue(url.contains("id=in.(1%2C2)"));
        Assertions.assertTrue(url.contains("deleted_at=is.null"));
        Assertions.assertTrue(url.contains("created_at=not.is.null"));
        Assertions.assertTrue(url.contains("offset=10"));
    }

    @Test
    public void testComparisonAndLikeOperators() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        client.from("users").ne("status","inactive").gt("score",80).gte("level",2).lt("age",50).lte("age",60).like("name","A%25").ilike("email","%@example.com").list(Map.class);
        String url = exec.last.getUrl();
        Assertions.assertTrue(url.contains("status=neq.inactive"));
        Assertions.assertTrue(url.contains("score=gt.80"));
        Assertions.assertTrue(url.contains("level=gte.2"));
        Assertions.assertTrue(url.contains("age=lt.50"));
        Assertions.assertTrue(url.contains("age=lte.60"));
        Assertions.assertTrue(url.contains("name=like.A%2525"));
        Assertions.assertTrue(url.contains("email=ilike.%25%40example.com"));
    }

    @Test
    public void testUpdateWhitelist() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        Map<String,Object> payload = Map.of("name","bob","extra","x");
        List<Map> out = client.from("users").eq("id",1).withFilter(new WhitelistPayloadFieldFilter(Set.of("name"))).update(payload, Map.class);
        Assertions.assertEquals(1, out.size());
        String body = exec.last.getBody();
        Assertions.assertTrue(body.contains("\"name\":"));
        Assertions.assertFalse(body.contains("\"extra\":"));
        Assertions.assertEquals("PATCH", exec.last.getMethod());
        Assertions.assertTrue(exec.last.getUrl().contains("id=eq.1"));
    }

    @Test
    public void testDeleteChain() {
        CapturingExecutor exec = new CapturingExecutor();
        PgRestClient client = newClient(exec);
        int count = client.from("users").eq("role","guest").delete();
        Assertions.assertEquals(0, count); // response stub returns 0 total
        Assertions.assertEquals("DELETE", exec.last.getMethod());
        Assertions.assertTrue(exec.last.getUrl().contains("role=eq.guest"));
    }
}
