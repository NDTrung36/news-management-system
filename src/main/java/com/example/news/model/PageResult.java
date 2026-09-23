package com.example.news.model;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class PageResult<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long totalItems;
    private final int totalPages;

    public PageResult(List<T> items, int page, int pageSize, long totalItems, int totalPages) {
        this.items = items == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(items));
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return totalPages > 0 && page < totalPages;
    }
}
