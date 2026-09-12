package com.example.news.service.impl;

import com.example.news.dao.ICategoryDAO;
import com.example.news.dao.impl.CategoryDAO;
import com.example.news.exception.ValidationException;
import com.example.news.model.CategoryModel;
import com.example.news.service.ICategoryService;

import java.util.List;

public class CategoryService implements ICategoryService {

    private final ICategoryDAO categoryDAO;

    public CategoryService() {
        this(new CategoryDAO());
    }

    public CategoryService(ICategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    @Override
    public List<CategoryModel> findAll() {
        return categoryDAO.findAll();
    }

    @Override
    public CategoryModel findById(Long id) {
        if (id == null) {
            return null;
        }
        return categoryDAO.findById(id);
    }

    @Override
    public long create(CategoryModel category) {
        validateAndTrim(category);

        if (categoryDAO.findByCode(category.getCode()) != null) {
            throw new ValidationException("Category code already exists: " + category.getCode());
        }

        return categoryDAO.insert(category);
    }

    @Override
    public boolean update(CategoryModel category) {
        if (category == null || category.getId() == null) {
            throw new ValidationException("Category ID cannot be null");
        }

        validateAndTrim(category);

        CategoryModel existing = categoryDAO.findById(category.getId());
        if (existing == null) {
            return false;
        }

        CategoryModel duplicate = categoryDAO.findByCode(category.getCode());
        if (duplicate != null && !duplicate.getId().equals(category.getId())) {
            throw new ValidationException("Category code already exists: " + category.getCode());
        }

        return categoryDAO.update(category) > 0;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            throw new ValidationException("Category ID cannot be null");
        }

        CategoryModel existing = categoryDAO.findById(id);
        if (existing == null) {
            return false;
        }

        long newsCount = categoryDAO.countNewsByCategoryId(id);
        if (newsCount > 0) {
            throw new ValidationException("Cannot delete category because it is being used by news articles");
        }

        return categoryDAO.delete(id) > 0;
    }

    private void validateAndTrim(CategoryModel category) {
        if (category == null) {
            throw new ValidationException("Category cannot be null");
        }

        String name = category.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Category name cannot be empty");
        }
        name = name.trim();
        if (name.length() > 100) {
            throw new ValidationException("Category name must not exceed 100 characters");
        }
        category.setName(name);

        String code = category.getCode();
        if (code == null || code.trim().isEmpty()) {
            throw new ValidationException("Category code cannot be empty");
        }
        code = code.trim();
        if (code.length() > 100) {
            throw new ValidationException("Category code must not exceed 100 characters");
        }
        category.setCode(code);
    }
}
