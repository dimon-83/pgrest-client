package com.github.pgrest.client.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class JdkHttpExecutorTest {
    private HttpResponse<String> mockResponse(int status, String body, Map<String, List<String>> headers) {
        HttpResponse<String> resp = Mockito.mock(HttpResponse.class);
        Mockito.when(resp.statusCode()).thenReturn(status);
        Mockito.when(resp.body()).thenReturn(body);
        HttpHeaders hs = Mockito.mock(HttpHeaders.class);
        Mockito.when(hs.map()).thenReturn(headers);
        Mockito.when(resp.headers()).thenReturn(hs);
        return resp;
    }

    @Test
    public void executesGetWithHeaders() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        Mockito.when(client.send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
                .thenAnswer(inv -> mockResponse(200, "ok", Map.of("content-type", List.of("application/json"))));

        JdkHttpExecutor exec = new JdkHttpExecutor(client);
        HttpRequestData req = new HttpRequestData();
        req.setUrl("http://localhost/test");
        req.setMethod("GET");
        req.getHeaders().put("Accept", "application/json");

        HttpResponseData out = exec.execute(req);
        Assertions.assertEquals(200, out.getStatus());
        Assertions.assertEquals("ok", out.getBody());
        Assertions.assertEquals("application/json", out.getHeaders().get("content-type"));
    }

    @Test
    public void executesPostUtf8BodyAndPatch() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        Mockito.when(client.send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
                .thenAnswer(inv -> mockResponse(201, "créé", Map.of("x-id", List.of("1"))));

        JdkHttpExecutor exec = new JdkHttpExecutor(client);
        HttpRequestData req = new HttpRequestData();
        req.setUrl("http://localhost/items");
        req.setMethod("PATCH");
        req.setBody("名称=例子");
        req.getHeaders().put("Content-Type", "application/json; charset=UTF-8");

        HttpResponseData out = exec.execute(req);
        Assertions.assertEquals(201, out.getStatus());
        Assertions.assertEquals("créé", out.getBody());
        Assertions.assertEquals("1", out.getHeaders().get("x-id"));
        Assertions.assertEquals(StandardCharsets.UTF_8, StandardCharsets.UTF_8);
    }

    @Test
    public void mapsEmptyHeaderListToNull() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        Mockito.when(client.send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
                .thenAnswer(inv -> mockResponse(200, "", Map.of("x-empty", List.of())));

        JdkHttpExecutor exec = new JdkHttpExecutor(client);
        HttpRequestData req = new HttpRequestData();
        req.setUrl("http://localhost/headers");
        req.setMethod("GET");

        HttpResponseData out = exec.execute(req);
        Assertions.assertTrue(out.getHeaders().containsKey("x-empty"));
        Assertions.assertNull(out.getHeaders().get("x-empty"));
    }

    @Test
    public void wrapsSendExceptionAsRuntime() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        Mockito.when(client.send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("network error"));

        JdkHttpExecutor exec = new JdkHttpExecutor(client);
        HttpRequestData req = new HttpRequestData();
        req.setUrl("http://localhost/error");
        req.setMethod("GET");

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> exec.execute(req));
        Assertions.assertTrue(thrown.getCause() instanceof IOException);
        Assertions.assertTrue(thrown.getMessage() != null);
    }
}

