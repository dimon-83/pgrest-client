package com.github.pgrest.client;

public class PgFn {
    private final PgRestClient client;
    private final String function;
    private final PgQueryBuilder builder = new PgQueryBuilder();
    private Object payload;

    public PgFn(PgRestClient client, String function) {
        this.client = client;
        this.function = function;
    }

    public PgFn params(Object payload) { this.payload = payload; return this; }

    public PgFn select(String select) { builder.select(select); return this; }
    public PgFn eq(String column, Object value) { builder.eq(column, value); return this; }
    public PgFn ne(String column, Object value) { builder.ne(column, value); return this; }
    public PgFn gt(String column, Object value) { builder.gt(column, value); return this; }
    public PgFn gte(String column, Object value) { builder.gte(column, value); return this; }
    public PgFn lt(String column, Object value) { builder.lt(column, value); return this; }
    public PgFn lte(String column, Object value) { builder.lte(column, value); return this; }
    public PgFn like(String column, String pattern) { builder.like(column, pattern); return this; }
    public PgFn ilike(String column, String pattern) { builder.ilike(column, pattern); return this; }
    public PgFn in(String column, java.util.List<?> values) { builder.in(column, values); return this; }
    public PgFn isNull(String column) { builder.isNull(column); return this; }
    public PgFn notNull(String column) { builder.notNull(column); return this; }
    public PgFn orderAsc(String column) { builder.orderAsc(column); return this; }
    public PgFn orderDesc(String column) { builder.orderDesc(column); return this; }
    public PgFn groupBy(String... columns) { builder.groupBy(columns); return this; }
    public PgFn limit(int limit) { builder.limit(limit); return this; }
    public PgFn offset(int offset) { builder.offset(offset); return this; }
    public PgFn raw(String key, String value) { builder.raw(key, value); return this; }

    private Object paramsOrEmpty() { return payload == null ? java.util.Collections.emptyMap() : payload; }

    public <T> java.util.List<T> list(Class<T> type) { return client.rpcForList(function, paramsOrEmpty(), builder, type); }

    public <T> T single(Class<T> type) {
        PgQueryBuilder qb = builder.copy().limit(1);
        java.util.List<T> list = client.rpcForList(function, paramsOrEmpty(), qb, type);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public <T> T object(Class<T> type) { return client.rpcForObject(function, paramsOrEmpty(), builder, type); }

    public void invokeVoid() { client.rpcForList(function, paramsOrEmpty(), builder, java.util.Map.class); }
}
