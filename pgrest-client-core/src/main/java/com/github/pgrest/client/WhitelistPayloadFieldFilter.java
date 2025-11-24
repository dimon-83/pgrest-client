package com.github.pgrest.client;

import java.util.Map;
import java.util.Set;

public class WhitelistPayloadFieldFilter implements PayloadFieldFilter {
    private final Set<String> whitelist;

    public WhitelistPayloadFieldFilter(Set<String> whitelist) { this.whitelist = whitelist == null ? java.util.Collections.emptySet() : whitelist; }

    @Override
    public Map<String, Object> apply(String resource, Object source, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty() || whitelist.isEmpty()) return payload;
        java.util.Map<String,Object> out = new java.util.HashMap<>(Math.min(payload.size(), whitelist.size()));
        for (String k : whitelist) {
            if (payload.containsKey(k)) out.put(k, payload.get(k));
        }
        return out;
    }
}