package com.example.news.service;

import com.example.news.model.CategoryModel;

import java.util.List;

public interface ICategoryService {

    List<CategoryModel> findAll();

    CategoryModel findById(Long id);

    long create(CategoryModel category);

    boolean update(CategoryModel category);

    boolean delete(Long id);
}
