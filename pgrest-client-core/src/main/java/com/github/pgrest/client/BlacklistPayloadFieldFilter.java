package com.github.pgrest.client;

import java.util.Map;
import java.util.Set;

public class BlacklistPayloadFieldFilter implements PayloadFieldFilter {
    private final Set<String> blacklist;

    public BlacklistPayloadFieldFilter(Set<String> blacklist) { this.blacklist = blacklist == null ? java.util.Collections.emptySet() : blacklist; }

    @Override
    public Map<String, Object> apply(String resource, Object source, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty() || blacklist.isEmpty()) return payload;
        java.util.Map<String,Object> out = new java.util.HashMap<>(payload.size());
        for (Map.Entry<String,Object> e : payload.entrySet()) {
            if (!blacklist.contains(e.getKey())) out.put(e.getKey(), e.getValue());
        }
        return out;
    }
}