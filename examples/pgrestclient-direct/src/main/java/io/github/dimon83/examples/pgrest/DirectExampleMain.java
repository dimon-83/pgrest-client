package io.github.dimon83.examples.pgrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        cfg.setBaseUrl("http://10.38.245.103:3007");
        cfg.setSecret("reallyreallyreallyreallyverysafe");
        cfg.setJwtTtlSeconds(3600);
        cfg.setDbRole("api_user");
        ObjectMapper mapper = new ObjectMapper();
        
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.registerModule(new JavaTimeModule());
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        PgRestClient client = new PgRestClient(cfg, new JdkHttpExecutor(http), mapper);

        PgQueryBuilder qb = new PgQueryBuilder().select("id,park_code,equip_name,create_time").orderDesc("create_time").limit(5);
        List<GateInfo> rows = client.list("gate_info", qb, GateInfo.class);
        System.out.println("list size=" + rows.size());

        // //insert a new gate info
        // GateInfo newGate = new GateInfo();
        // newGate.setId(1991394183331446784L);
        // newGate.setParkCode("PARK001");
        // newGate.setEquipName("test_equip");
        // newGate.setEquipCode("Code_0001");
        // newGate.setEquipType("0");
        // newGate.setCreateBy("admin");
        // newGate.setCreateTime(java.time.LocalDateTime.now());
        // newGate.setUpdateBy("admin");
        // newGate.setUpdateTime(java.time.LocalDateTime.now());
        // newGate.setDelFlag("0");
        // newGate.setTenantId(0L);
        // List<GateInfo> ins = client.insert("gate_info", newGate, GateInfo.class);
        // System.out.println("inserted=" + ins.size());

        // PageResult<Map> page = client.page("users", new PgQueryBuilder().orderAsc("id"), 1, 10, Map.class);
        // System.out.println("page total=" + page.getTotal());

        // PgQueryBuilder aggByStatus = new PgQueryBuilder().select("status,id.count()").orderAsc("status");
        // List<Map> statusStats = client.list("users", aggByStatus, Map.class);
        // System.out.println("status counts=" + statusStats);

        // PgQueryBuilder dailyStatsQb = new PgQueryBuilder().select("created_at::date,id.count()").orderAsc("created_at::date");
        // List<Map> dailyStats = client.list("users", dailyStatsQb, Map.class);
        // System.out.println("daily new users=" + dailyStats);
        // try {
        //     List<Map> ins = client.insert("users", Map.of("user_name", "alice"), Map.class);
        //     System.out.println("inserted=" + ins.size());
        //     if (ins == null || ins.isEmpty() || ins.get(0) == null || ins.get(0).get("id") == null) {
        //         System.out.println("skip update/delete: insert empty");
        //         return;
        //     }
        //     Object id = ins.get(0).get("id");
        //     List<Map> upd = client.update("users", new PgQueryBuilder().eq("id", id), Map.of("status", "active"), Map.class);
        //     System.out.println("updated=" + upd.size());
        //     int del = client.delete("users", new PgQueryBuilder().eq("id", id));
        //     System.out.println("deleted=" + del);
        // } catch (Exception e) {
        //     System.out.println("write failed: " + e.getMessage());
        // }
    }
}