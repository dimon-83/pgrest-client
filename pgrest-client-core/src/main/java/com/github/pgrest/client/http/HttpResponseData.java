package com.github.pgrest.client.http;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponseData {
    private int status;
    private String body;
    private Map<String, String> headers = new LinkedHashMap<>();

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Map<String, String> getHeaders() { return headers; }
}