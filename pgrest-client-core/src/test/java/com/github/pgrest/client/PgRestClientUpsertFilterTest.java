package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

public class PgRestClientUpsertFilterTest {

    static class TestEntity {
        private String name;
        @PgIgnore
        private String ignored;

        public TestEntity(String name, String ignored) {
            this.name = name;
            this.ignored = ignored;
        }

        public String getName() { return name; }
        public String getIgnored() { return ignored; }
    }

    static class StubExecutor implements HttpExecutor {
        HttpRequestData last;
        @Override
        public HttpResponseData execute(HttpRequestData request) {
            this.last = request;
            HttpResponseData resp = new HttpResponseData();
            resp.setStatus(201);
            resp.setBody("[]");
            // headers are already initialized
            return resp;
        }
    }

    @Test
    public void testUpsertMergeWithDefaultFilter() {
        StubExecutor executor = new StubExecutor();
        PgClientConfig config = new PgClientConfig();
        config.setBaseUrl("http://localhost:3000");
        PgRestClient client = new PgRestClient(config, executor, new ObjectMapper());

        TestEntity entity = new TestEntity("test-upsert", "should-be-ignored");
        
        client.upsertMerge("test_resource", entity, new PgQueryBuilder().raw("on_conflict", "id"), TestEntity.class);

        Assertions.assertNotNull(executor.last);
        String body = executor.last.getBody();
        
        // Verify 'name' is present
        Assertions.assertTrue(body.contains("test-upsert"), "Body should contain name");
        
        // Verify 'ignored' is NOT present
        Assertions.assertFalse(body.contains("should-be-ignored"), "Body should NOT contain ignored field value");
        Assertions.assertFalse(body.contains("ignored"), "Body should NOT contain ignored field key");
        
        // Verify Prefer header
        String prefer = executor.last.getHeaders().get("Prefer");
        Assertions.assertTrue(prefer.contains("resolution=merge-duplicates"));
    }

    @Test
    public void testUpsertMergeWithExplicitFilter() {
        StubExecutor executor = new StubExecutor();
        PgClientConfig config = new PgClientConfig();
        config.setBaseUrl("http://localhost:3000");
        PgRestClient client = new PgRestClient(config, executor, new ObjectMapper());

        TestEntity entity = new TestEntity("test-upsert-explicit", "should-be-ignored");
        
        // Use a custom filter that does NOT ignore anything (identity)
        PayloadFieldFilter identityFilter = (res, src, payload) -> payload;

        client.upsertMerge("test_resource", entity, new PgQueryBuilder().raw("on_conflict", "id"), identityFilter, TestEntity.class);

        Assertions.assertNotNull(executor.last);
        String body = executor.last.getBody();
        
        // Verify 'name' is present
        Assertions.assertTrue(body.contains("test-upsert-explicit"), "Body should contain name");
        
        // Verify 'ignored' IS present because we used identity filter
        Assertions.assertTrue(body.contains("should-be-ignored"), "Body should contain ignored field value with identity filter");
    }
}
