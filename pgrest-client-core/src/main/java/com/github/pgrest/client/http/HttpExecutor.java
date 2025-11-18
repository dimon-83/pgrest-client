package com.github.pgrest.client.http;

public interface HttpExecutor {
    HttpResponseData execute(HttpRequestData request);
}