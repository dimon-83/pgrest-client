package com.github.pgrest.client;

import java.util.List;

public class PageResult<T> {
    private final int page;
    private final int size;
    private final long total;
    private final List<T> items;

    public PageResult(int page, int size, long total, List<T> items) {
        this.page = page; this.size = size; this.total = total; this.items = items;
    }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotal() { return total; }
    public List<T> getItems() { return items; }
}