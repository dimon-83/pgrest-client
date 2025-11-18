package io.github.dimon83.examples.pgrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.pgrest.client.PageResult;
import com.github.pgrest.client.PgQueryBuilder;
import com.github.pgrest.client.PgRestClient;
import com.github.pgrest.client.PgClientConfig;
import com.github.pgrest.client.http.JdkHttpExecutor;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class DirectExampleMain {
    public static void main(String[] args) {
        PgClientConfig cfg = new PgClientConfig();
        cfg.setBaseUrl("http://10.38.245.92:3007");
        cfg.setSecret("reallyreallyreallyreallyverysafe");
        cfg.setJwtTtlSeconds(3600);
        cfg.setDbRole("api_user");
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        PgRestClient client = new PgRestClient(cfg, new JdkHttpExecutor(http), mapper);

        PgQueryBuilder qb = new PgQueryBuilder().select("id,park_code,equip_name,tenant_id").orderDesc("id").limit(5);
        List<Map> rows = client.list("gate_info", qb, Map.class);
        System.out.println("list size=" + rows.size());

        PageResult<Map> page = client.page("gate_info", new PgQueryBuilder().orderAsc("id"), 1, 10, Map.class);
        System.out.println("page total=" + page.getTotal());
        try {
            List<Map> ins = client.insert("gate_info", Map.of("equip_name", "demo"), Map.class);
            System.out.println("inserted=" + ins.size());
            if (ins == null || ins.isEmpty() || ins.get(0) == null || ins.get(0).get("id") == null) {
                System.out.println("skip update/delete: insert empty");
                return;
            }
            Object id = ins.get(0).get("id");
            List<Map> upd = client.update("gate_info", new PgQueryBuilder().eq("id", id), Map.of("equip_name", "demo2"), Map.class);
            System.out.println("updated=" + upd.size());
            int del = client.delete("gate_info", new PgQueryBuilder().eq("id", id));
            System.out.println("deleted=" + del);
        } catch (Exception e) {
            System.out.println("write failed: " + e.getMessage());
        }
    }
}