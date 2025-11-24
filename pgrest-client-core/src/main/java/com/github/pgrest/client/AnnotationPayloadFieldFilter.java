package com.github.pgrest.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotationPayloadFieldFilter implements PayloadFieldFilter {
    @Override
    public Map<String, Object> apply(String resource, Object source, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty() || source == null) return payload;
        if (source instanceof Map) return payload;
        Class<?> c = source.getClass();
        Set<String> include = new HashSet<>();
        Set<String> ignore = new HashSet<>();

        for (Field f : c.getDeclaredFields()) {
            String name = f.getName();
            String snake = toSnake(name);
            if (f.isAnnotationPresent(PgIgnore.class)) { ignore.add(name); ignore.add(snake); }
            if (f.isAnnotationPresent(PgInclude.class)) { include.add(name); include.add(snake); }
        }
        for (Method m : c.getDeclaredMethods()) {
            String name = propertyNameFromGetter(m);
            if (name == null) continue;
            String snake = toSnake(name);
            if (m.isAnnotationPresent(PgIgnore.class)) { ignore.add(name); ignore.add(snake); }
            if (m.isAnnotationPresent(PgInclude.class)) { include.add(name); include.add(snake); }
        }

        if (include.isEmpty() && ignore.isEmpty()) return payload;
        java.util.Map<String,Object> out = new java.util.HashMap<>(payload.size());
        if (!include.isEmpty()) {
            for (Map.Entry<String,Object> e : payload.entrySet()) {
                String k = e.getKey();
                if (include.contains(k) && !ignore.contains(k)) out.put(k, e.getValue());
            }
        } else {
            for (Map.Entry<String,Object> e : payload.entrySet()) {
                String k = e.getKey();
                if (!ignore.contains(k)) out.put(k, e.getValue());
            }
        }
        return out;
    }

    private static String propertyNameFromGetter(Method m) {
        String n = m.getName();
        if (m.getParameterCount() != 0) return null;
        if (n.startsWith("get") && n.length() > 3) return decap(n.substring(3));
        if (n.startsWith("is") && n.length() > 2 && (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)) return decap(n.substring(2));
        return null;
    }

    private static String decap(String s) { return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1); }

    private static String toSnake(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(ch));
            } else sb.append(ch);
        }
        return sb.toString();
    }
}