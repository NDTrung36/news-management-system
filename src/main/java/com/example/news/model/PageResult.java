package com.example.news.model;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class PageResult<T> {

    private static final int PAGE_WINDOW_SIZE = 5;

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

    public int getFirstVisiblePage() {
        if (totalPages <= 0) {
            return 0;
        }
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int centeredStart = Math.max(1, currentPage - PAGE_WINDOW_SIZE / 2);
        return Math.min(centeredStart, Math.max(1, totalPages - PAGE_WINDOW_SIZE + 1));
    }

    public int getLastVisiblePage() {
        if (totalPages <= 0) {
            return 0;
        }
        return Math.min(totalPages, getFirstVisiblePage() + PAGE_WINDOW_SIZE - 1);
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return totalPages > 0 && page < totalPages;
    }
}
