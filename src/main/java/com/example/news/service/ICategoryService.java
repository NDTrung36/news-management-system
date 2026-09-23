package com.example.news.service;

import com.example.news.model.CategoryModel;
import com.example.news.model.CategoryListCriteria;
import com.example.news.model.PageResult;

import java.util.List;

public interface ICategoryService {

    List<CategoryModel> findAll();

    PageResult<CategoryModel> search(CategoryListCriteria criteria);

    CategoryModel findById(Long id);

    long create(CategoryModel category);

    boolean update(CategoryModel category);

    boolean delete(Long id);
}
