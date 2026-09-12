package com.example.news.dao.impl;

import com.example.news.dao.ICategoryDAO;
import com.example.news.dao.IGenericDAO;
import com.example.news.mapper.CategoryMapper;
import com.example.news.mapper.RowMapper;
import com.example.news.model.CategoryModel;

import java.util.List;

public class CategoryDAO implements ICategoryDAO {

    private final IGenericDAO genericDAO;
    private final RowMapper<CategoryModel> categoryMapper;

    public CategoryDAO() {
        this(new GenericDAO());
    }

    public CategoryDAO(IGenericDAO genericDAO) {
        this.genericDAO = genericDAO;
        this.categoryMapper = new CategoryMapper();
    }

    @Override
    public List<CategoryModel> findAll() {
        String sql = "SELECT id, name, code, created_date, modified_date FROM category ORDER BY id";
        return genericDAO.query(sql, categoryMapper);
    }

    @Override
    public CategoryModel findById(Long id) {
        String sql = "SELECT id, name, code, created_date, modified_date FROM category WHERE id = ?";
        return genericDAO.queryOne(sql, categoryMapper, id);
    }

    @Override
    public CategoryModel findByCode(String code) {
        String sql = "SELECT id, name, code, created_date, modified_date FROM category WHERE code = ?";
        return genericDAO.queryOne(sql, categoryMapper, code);
    }

    @Override
    public long insert(CategoryModel category) {
        String sql = "INSERT INTO category (name, code) VALUES (?, ?)";
        return genericDAO.insert(sql, category.getName(), category.getCode());
    }

    @Override
    public int update(CategoryModel category) {
        String sql = "UPDATE category SET name = ?, code = ?, modified_date = CURRENT_TIMESTAMP WHERE id = ?";
        return genericDAO.update(sql, category.getName(), category.getCode(), category.getId());
    }

    @Override
    public int delete(Long id) {
        String sql = "DELETE FROM category WHERE id = ?";
        return genericDAO.delete(sql, id);
    }

    @Override
    public long countNewsByCategoryId(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM news WHERE category_id = ?";
        return genericDAO.count(sql, categoryId);
    }
}
