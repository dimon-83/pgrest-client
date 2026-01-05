package com.github.pgrest.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class PgQueryBuilder {
    private final List<String> parts = new ArrayList<>();
    private String select;
    private String order;
    private String group;
    private Integer limit;
    private Integer offset;

    public PgQueryBuilder select(String select) { this.select = select; return this; }
    public PgQueryBuilder orderAsc(String column) { this.order = encode(column) + ".asc"; return this; }
    public PgQueryBuilder orderDesc(String column) { this.order = encode(column) + ".desc"; return this; }
    public PgQueryBuilder groupBy(String... columns) { if (columns == null || columns.length == 0) return this; StringJoiner j = new StringJoiner(","); for (String c : columns) j.add(c); this.group = encode(j.toString()); return this; }
    public PgQueryBuilder columns(String... columns) { if (columns == null || columns.length == 0) return this; StringJoiner j = new StringJoiner(","); for (String c : columns) j.add(c); parts.add("columns=" + encode(j.toString())); return this; }
    public PgQueryBuilder eq(String column, Object value) { parts.add(encode(column) + "=eq." + encode(String.valueOf(value))); return this; }
    public PgQueryBuilder ne(String column, Object value) { parts.add(encode(column) + "=neq." + encode(String.valueOf(value))); return this; }
    public PgQueryBuilder gt(String column, Object value) { parts.add(encode(column) + "=gt." + encode(String.valueOf(value))); return this; }
    public PgQueryBuilder gte(String column, Object value) { parts.add(encode(column) + "=gte." + encode(String.valueOf(value))); return this; }
    public PgQueryBuilder lt(String column, Object value) { parts.add(encode(column) + "=lt." + encode(String.valueOf(value))); return this; }
    public PgQueryBuilder lte(String column, Object value) { parts.add(encode(column) + "=lte." + encode(String.valueOf(value))); return this; }
    public PgQueryBuilder like(String column, String pattern) { parts.add(encode(column) + "=like." + encode(pattern)); return this; }
    public PgQueryBuilder ilike(String column, String pattern) { parts.add(encode(column) + "=ilike." + encode(pattern)); return this; }
    public PgQueryBuilder in(String column, List<?> values) { StringJoiner joiner = new StringJoiner(","); for (Object v : values) joiner.add(String.valueOf(v)); parts.add(encode(column) + "=in.(" + encode(joiner.toString()) + ")"); return this; }
    public PgQueryBuilder isNull(String column) { parts.add(encode(column) + "=is.null"); return this; }
    public PgQueryBuilder notNull(String column) { parts.add(encode(column) + "=not.is.null"); return this; }
    public PgQueryBuilder limit(int limit) { this.limit = limit; return this; }
    public PgQueryBuilder offset(int offset) { this.offset = offset; return this; }
    public PgQueryBuilder copy() { PgQueryBuilder b = new PgQueryBuilder(); b.parts.addAll(this.parts); b.select = this.select; b.order = this.order; b.group = this.group; b.limit = this.limit; b.offset = this.offset; return b; }
    public PgQueryBuilder raw(String key, String value) { parts.add(encode(key) + "=" + encode(value)); return this; }
    public static PgQueryBuilder fromQuery(java.util.Map<String, String> query) { PgQueryBuilder b = new PgQueryBuilder(); if (query == null || query.isEmpty()) return b; for (java.util.Map.Entry<String,String> e : query.entrySet()) { String k = e.getKey(); String v = e.getValue(); if ("select".equalsIgnoreCase(k)) b.select(v); else if ("order".equalsIgnoreCase(k)) b.raw("order", v); else if ("limit".equalsIgnoreCase(k)) b.limit(Integer.parseInt(v)); else if ("offset".equalsIgnoreCase(k)) b.offset(Integer.parseInt(v)); else b.raw(k, v); } return b; }
    public String build() { List<String> q = new ArrayList<>(parts); if (select != null) q.add("select=" + encode(select)); if (order != null) q.add("order=" + order); if (group != null) q.add("group=" + group); if (limit != null) q.add("limit=" + limit); if (offset != null) q.add("offset=" + offset); if (q.isEmpty()) return ""; StringJoiner joiner = new StringJoiner("&", "?", ""); for (String s : q) joiner.add(s); return joiner.toString(); }
    private String encode(String s) { try { return URLEncoder.encode(s, StandardCharsets.UTF_8.name()); } catch (UnsupportedEncodingException e) { throw new RuntimeException(e); } }
}
