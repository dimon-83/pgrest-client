package io.github.dimon83.examples.pgrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.pgrest.client.PageResult;
import com.github.pgrest.client.PgQueryBuilder;
import com.github.pgrest.client.PgRestClient;
import com.github.pgrest.client.PgRestProperties;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class DirectExampleMain {
    public static void main(String[] args) {
        PgRestProperties props = new PgRestProperties();
        props.setBaseUrl("http://127.0.0.1:3000");
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        PgRestClient client = new PgRestClient(props, http, mapper);

        PgQueryBuilder qb = new PgQueryBuilder().select("id,user_name,created_at").orderDesc("created_at").limit(5);
        List<Map> rows = client.list("users", qb, Map.class);
        System.out.println("list size=" + rows.size());

        PageResult<Map> page = client.page("users", new PgQueryBuilder().orderAsc("id"), 1, 10, Map.class);
        System.out.println("page total=" + page.getTotal());

        List<Map> ins = client.insert("users", Map.of("user_name", "alice"), Map.class);
        System.out.println("inserted=" + ins.size());

        List<Map> upd = client.update("users", new PgQueryBuilder().eq("id", ins.get(0).get("id")), Map.of("status", "active"), Map.class);
        System.out.println("updated=" + upd.size());

        int del = client.delete("users", new PgQueryBuilder().eq("id", ins.get(0).get("id")));
        System.out.println("deleted=" + del);
    }
}