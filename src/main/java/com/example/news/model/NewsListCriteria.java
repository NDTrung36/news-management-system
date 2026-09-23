package com.example.news.model;

public class NewsListCriteria {

    public static final String DEFAULT_SORT_NAME = "createdDate";
    public static final String DEFAULT_SORT_BY = "desc";
    public static final int DEFAULT_PAGE = 1;
    public static final int MAX_SEARCH_LENGTH = 100;

    private String search;
    private Long categoryId;
    private String sortName;
    private String sortBy;
    private int page;

    public NewsListCriteria() {
        this("", null, DEFAULT_SORT_NAME, DEFAULT_SORT_BY, DEFAULT_PAGE);
    }

    public NewsListCriteria(String search, Long categoryId, String sortName, String sortBy, int page) {
        this.search = search;
        this.categoryId = categoryId;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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
