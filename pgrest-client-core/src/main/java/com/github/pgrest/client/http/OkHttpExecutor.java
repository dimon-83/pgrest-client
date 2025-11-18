package com.github.pgrest.client.http;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.nio.charset.StandardCharsets;

public class OkHttpExecutor implements HttpExecutor {
    private final OkHttpClient client;
    public OkHttpExecutor(OkHttpClient client) { this.client = client; }
    @Override
    public HttpResponseData execute(HttpRequestData req) {
        try {
            Request.Builder rb = new Request.Builder().url(req.getUrl());
            for (var e : req.getHeaders().entrySet()) rb.addHeader(e.getKey(), e.getValue());
            String m = req.getMethod();
            String body = req.getBody();
            RequestBody requestBody = body == null ? RequestBody.create(new byte[0]) : RequestBody.create(body.getBytes(StandardCharsets.UTF_8), MediaType.parse(req.getHeaders().getOrDefault("Content-Type", "application/json")));
            if ("GET".equalsIgnoreCase(m)) rb.get();
            else if ("DELETE".equalsIgnoreCase(m)) rb.delete();
            else if ("PATCH".equalsIgnoreCase(m)) rb.patch(requestBody);
            else rb.method(m.toUpperCase(), requestBody);
            try (Response resp = client.newCall(rb.build()).execute()) {
                HttpResponseData out = new HttpResponseData();
                out.setStatus(resp.code());
                out.setBody(resp.body() == null ? null : resp.body().string());
                resp.headers().toMultimap().forEach((k, v) -> out.getHeaders().put(k, v == null || v.isEmpty() ? null : v.get(0)));
                return out;
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}