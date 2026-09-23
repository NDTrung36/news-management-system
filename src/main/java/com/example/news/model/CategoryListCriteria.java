package com.example.news.model;

public class CategoryListCriteria {

    public static final String DEFAULT_SORT_NAME = "id";
    public static final String DEFAULT_SORT_BY = "asc";
    public static final int DEFAULT_PAGE = 1;
    public static final int MAX_SEARCH_LENGTH = 100;

    private String search;
    private String sortName;
    private String sortBy;
    private int page;

    public CategoryListCriteria() {
        this("", DEFAULT_SORT_NAME, DEFAULT_SORT_BY, DEFAULT_PAGE);
    }

    public CategoryListCriteria(String search, String sortName, String sortBy, int page) {
        this.search = search;
        this.sortName = sortName;
        this.sortBy = sortBy;
        this.page = page;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getSortName() {
        return sortName;
    }

    public void setSortName(String sortName) {
        this.sortName = sortName;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
