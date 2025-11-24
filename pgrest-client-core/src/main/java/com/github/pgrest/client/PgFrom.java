package com.github.pgrest.client;

public class PgFrom {
    private final PgRestClient client;
    private final String resource;
    private final PgQueryBuilder builder = new PgQueryBuilder();
    private PayloadFieldFilter filter;

    public PgFrom(PgRestClient client, String resource) {
        this.client = client;
        this.resource = resource;
    }

    public PgFrom select(String select) { builder.select(select); return this; }
    public PgFrom eq(String column, Object value) { builder.eq(column, value); return this; }
    public PgFrom ne(String column, Object value) { builder.ne(column, value); return this; }
    public PgFrom gt(String column, Object value) { builder.gt(column, value); return this; }
    public PgFrom gte(String column, Object value) { builder.gte(column, value); return this; }
    public PgFrom lt(String column, Object value) { builder.lt(column, value); return this; }
    public PgFrom lte(String column, Object value) { builder.lte(column, value); return this; }
    public PgFrom like(String column, String pattern) { builder.like(column, pattern); return this; }
    public PgFrom ilike(String column, String pattern) { builder.ilike(column, pattern); return this; }
    public PgFrom in(String column, java.util.List<?> values) { builder.in(column, values); return this; }
    public PgFrom isNull(String column) { builder.isNull(column); return this; }
    public PgFrom notNull(String column) { builder.notNull(column); return this; }
    public PgFrom orderAsc(String column) { builder.orderAsc(column); return this; }
    public PgFrom orderDesc(String column) { builder.orderDesc(column); return this; }
    public PgFrom groupBy(String... columns) { builder.groupBy(columns); return this; }
    public PgFrom limit(int limit) { builder.limit(limit); return this; }
    public PgFrom offset(int offset) { builder.offset(offset); return this; }
    public PgFrom raw(String key, String value) { builder.raw(key, value); return this; }

    public PgFrom withFilter(PayloadFieldFilter filter) { this.filter = filter; return this; }

    public <T> java.util.List<T> list(Class<T> type) { return client.list(resource, builder, type); }

    public <T> PageResult<T> page(int page, int size, Class<T> type) { return client.page(resource, builder, page, size, type); }

    public <T> T single(Class<T> type) {
        PgQueryBuilder qb = builder.copy().limit(1);
        java.util.List<T> list = client.list(resource, qb, type);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public <T> java.util.List<T> insert(Object payload, Class<T> type) {
        return filter == null ? client.insert(resource, payload, builder, type) : client.insert(resource, payload, builder, filter, type);
    }

    public <T> java.util.List<T> update(Object payload, Class<T> type) {
        return filter == null ? client.update(resource, builder, payload, type) : client.update(resource, builder, payload, filter, type);
    }

    public int delete() { return client.delete(resource, builder); }
}
