package com.example.news.model;

public class NewsModel extends AbstractModel {

    private String title;
    private String shortDescription;
    private String content;
    private String thumbnail;
    private Long categoryId;
    private Long createdById;

    public NewsModel() {
    }

    public NewsModel(String title, String shortDescription, String content, Long categoryId) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.content = content;
        this.categoryId = categoryId;
    }

    public NewsModel(Long id, String title, String shortDescription, String content, Long categoryId) {
        setId(id);
        this.title = title;
        this.shortDescription = shortDescription;
        this.content = content;
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }
}
