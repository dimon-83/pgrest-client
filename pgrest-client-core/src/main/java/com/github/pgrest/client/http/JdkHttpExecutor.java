package com.github.pgrest.client.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class JdkHttpExecutor implements HttpExecutor {
    private final HttpClient client;

    public JdkHttpExecutor(HttpClient client) { this.client = client; }

    @Override
    public HttpResponseData execute(HttpRequestData req) {
        try {
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(req.getUrl()));
            for (var e : req.getHeaders().entrySet()) rb.header(e.getKey(), e.getValue());
            String m = req.getMethod();
            String body = req.getBody();
            if ("GET".equalsIgnoreCase(m)) rb.GET();
            else if ("DELETE".equalsIgnoreCase(m)) rb.DELETE();
            else rb.method(m, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            HttpResponse<String> response = client.send(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            HttpResponseData out = new HttpResponseData();
            out.setStatus(response.statusCode());
            out.setBody(response.body());
            response.headers().map().forEach((k, v) -> out.getHeaders().put(k, v == null || v.isEmpty() ? null : v.get(0)));
            return out;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}