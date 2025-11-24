package com.github.pgrest.client;

import java.util.Map;

public interface PayloadFieldFilter {
    Map<String, Object> apply(String resource, Object source, Map<String, Object> payload);
}