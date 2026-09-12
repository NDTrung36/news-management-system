package com.example.news.dao;

import com.example.news.model.CategoryModel;

import java.util.List;

public interface ICategoryDAO {

    List<CategoryModel> findAll();

    CategoryModel findById(Long id);

    CategoryModel findByCode(String code);

    long insert(CategoryModel category);

    int update(CategoryModel category);

    int delete(Long id);

    long countNewsByCategoryId(Long categoryId);
}
