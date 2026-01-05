package com.github.pgrest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pgrest.client.http.HttpExecutor;
import com.github.pgrest.client.http.HttpRequestData;
import com.github.pgrest.client.http.HttpResponseData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class PgRestClientPartialUpdateTest {

    static class TestEntity {
        private String id;
        private String name;
        private String description;

        public TestEntity(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    static class StubExecutor implements HttpExecutor {
        HttpRequestData last;
        @Override
        public HttpResponseData execute(HttpRequestData request) {
            this.last = request;
            HttpResponseData resp = new HttpResponseData();
            resp.setStatus(201);
            resp.setBody("[]");
            return resp;
        }
    }

    @Test
    public void testUpsertMergeWithColumnsParameter() {
        StubExecutor executor = new StubExecutor();
        PgClientConfig config = new PgClientConfig();
        config.setBaseUrl("http://localhost:3000");
        PgRestClient client = new PgRestClient(config, executor, new ObjectMapper());

        TestEntity entity = new TestEntity("1", "Updated Name", "Should be ignored by server");
        
        // Use columns parameter to tell server to only insert/update id and name
        client.upsertMerge("test_resource", entity, new PgQueryBuilder().raw("on_conflict", "id").columns("id", "name"), TestEntity.class);

        Assertions.assertNotNull(executor.last);
        String url = executor.last.getUrl();
        
        // Verify URL contains columns parameter
        // The implementation encodes the comma, so we expect id%2Cname
        Assertions.assertTrue(url.contains("columns=id%2Cname") || url.contains("columns=id,name"), "URL should contain columns parameter: " + url);
        
        // Verify body still contains all fields (server-side filtering)
        String body = executor.last.getBody();
        Assertions.assertTrue(body.contains("Should be ignored by server"), "Body should contain all fields as filtering is server-side");
    }

    @Test
    public void testUpsertMergeWithPayloadFilter() {
        StubExecutor executor = new StubExecutor();
        PgClientConfig config = new PgClientConfig();
        config.setBaseUrl("http://localhost:3000");
        PgRestClient client = new PgRestClient(config, executor, new ObjectMapper());

        TestEntity entity = new TestEntity("1", "Updated Name", "Should be removed");
        
        // Use filter to remove description from payload
        PayloadFieldFilter filter = (res, src, payload) -> {
            if (payload instanceof Map) {
                ((Map<?, ?>) payload).remove("description");
            }
            return payload;
        };

        client.upsertMerge("test_resource", entity, new PgQueryBuilder().raw("on_conflict", "id"), filter, TestEntity.class);

        Assertions.assertNotNull(executor.last);
        String body = executor.last.getBody();
        
        // Verify body only contains id and name
        Assertions.assertTrue(body.contains("Updated Name"));
        Assertions.assertFalse(body.contains("Should be removed"), "Body should NOT contain filtered field");
    }
}
