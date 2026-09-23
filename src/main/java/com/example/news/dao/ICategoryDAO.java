package com.example.news.dao;

import com.example.news.model.CategoryModel;
import com.example.news.model.CategoryListCriteria;

import java.util.List;

public interface ICategoryDAO {

    List<CategoryModel> findAll();

    List<CategoryModel> findPage(CategoryListCriteria criteria, long offset, int limit);

    long countBySearch(String search);

    CategoryModel findById(Long id);

    CategoryModel findByCode(String code);

    long insert(CategoryModel category);

    int update(CategoryModel category);

    int delete(Long id);

    long countNewsByCategoryId(Long categoryId);
}
