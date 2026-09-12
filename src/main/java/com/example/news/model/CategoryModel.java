package com.example.news.model;

public class CategoryModel extends AbstractModel {

    private String name;
    private String code;

    public CategoryModel() {
    }

    public CategoryModel(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public CategoryModel(Long id, String name, String code) {
        setId(id);
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
