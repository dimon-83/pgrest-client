package com.github.pgrest.client.http;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequestData {
    private String url;
    private String method;
    private String body;
    private Map<String, String> headers = new LinkedHashMap<>();

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Map<String, String> getHeaders() { return headers; }
}