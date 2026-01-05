package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PgRestClientDefaultFilterTest {

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
    public void testDefaultAnnotationFilter() {
        StubExecutor executor = new StubExecutor();
        PgClientConfig config = new PgClientConfig();
        config.setBaseUrl("http://localhost:3000");
        PgRestClient client = new PgRestClient(config, executor, new ObjectMapper());

        TestEntity entity = new TestEntity("test-name", "should-be-ignored");
        
        // Call standard insert without explicit filter
        client.insert("test_resource", entity, TestEntity.class);

        Assertions.assertNotNull(executor.last);
        String body = executor.last.getBody();
        
        // Verify 'name' is present
        Assertions.assertTrue(body.contains("test-name"), "Body should contain name");
        
        // Verify 'ignored' is NOT present
        Assertions.assertFalse(body.contains("should-be-ignored"), "Body should NOT contain ignored field value");
        Assertions.assertFalse(body.contains("ignored"), "Body should NOT contain ignored field key");
    }
}
