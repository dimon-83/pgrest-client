package com.github.pgrest.client;

import java.util.List;

public class PageResult<T> {
    private final int page;
    private final int size;
    private final long total;
    private final List<T> records;

    public PageResult(int page, int size, long total, List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.records = records;
    }

    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotal() { return total; }
    public List<T> getRecords() { return records; }
}