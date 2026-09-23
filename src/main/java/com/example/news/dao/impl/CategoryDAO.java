package com.example.news.dao.impl;

import com.example.news.dao.ICategoryDAO;
import com.example.news.mapper.CategoryMapper;
import com.example.news.mapper.RowMapper;
import com.example.news.model.CategoryModel;
import com.example.news.model.CategoryListCriteria;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryDAO extends GenericDAO implements ICategoryDAO {

    private static final Map<String, String> SORT_COLUMNS;

    static {
        Map<String, String> columns = new HashMap<>();
        columns.put("id", "id");
        columns.put("name", "name");
        columns.put("code", "code");
        columns.put("createdDate", "created_date");
        SORT_COLUMNS = Collections.unmodifiableMap(columns);
    }

    private final RowMapper<CategoryModel> categoryMapper = new CategoryMapper();

    @Override
    public List<CategoryModel> findAll() {
        String sql = "SELECT id, name, code, created_date, modified_date FROM category ORDER BY id";
        return query(sql, categoryMapper);
    }

    @Override
    public List<CategoryModel> findPage(CategoryListCriteria criteria, long offset, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, name, code, created_date, modified_date FROM category");
        Object[] parameters = buildSearchParameters(sql, criteria == null ? null : criteria.getSearch());
        String sortColumn = resolveSortColumn(criteria == null ? null : criteria.getSortName());
        String sortDirection = "desc".equalsIgnoreCase(criteria == null ? null : criteria.getSortBy())
                ? "DESC" : "ASC";
        sql.append(" ORDER BY ").append(sortColumn).append(' ').append(sortDirection).append(", id ASC");
        sql.append(" LIMIT ?, ?");

        Object[] queryParameters = appendParameters(parameters, offset, limit);
        return query(sql.toString(), categoryMapper, queryParameters);
    }

    @Override
    public long countBySearch(String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM category");
        Object[] parameters = buildSearchParameters(sql, search);
        return count(sql.toString(), parameters);
    }

    @Override
    public CategoryModel findById(Long id) {
        String sql = "SELECT id, name, code, created_date, modified_date FROM category WHERE id = ?";
        return queryOne(sql, categoryMapper, id);
    }

    @Override
    public CategoryModel findByCode(String code) {
        String sql = "SELECT id, name, code, created_date, modified_date FROM category WHERE code = ?";
        return queryOne(sql, categoryMapper, code);
    }

    @Override
    public long insert(CategoryModel category) {
        String sql = "INSERT INTO category (name, code) VALUES (?, ?)";
        return insert(sql, category.getName(), category.getCode());
    }

    @Override
    public int update(CategoryModel category) {
        String sql = "UPDATE category SET name = ?, code = ?, modified_date = CURRENT_TIMESTAMP WHERE id = ?";
        return update(sql, category.getName(), category.getCode(), category.getId());
    }

    @Override
    public int delete(Long id) {
        String sql = "DELETE FROM category WHERE id = ?";
        return delete(sql, id);
    }

    @Override
    public long countNewsByCategoryId(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM news WHERE category_id = ?";
        return count(sql, categoryId);
    }

    private Object[] buildSearchParameters(StringBuilder sql, String search) {
        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.isEmpty()) {
            return new Object[0];
        }

        sql.append(" WHERE name LIKE ? ESCAPE '!' OR code LIKE ? ESCAPE '!'");
        String pattern = "%" + escapeLikePattern(normalizedSearch) + "%";
        return new Object[]{pattern, pattern};
    }

    private Object[] appendParameters(Object[] parameters, Object... additionalParameters) {
        Object[] allParameters = new Object[parameters.length + additionalParameters.length];
        System.arraycopy(parameters, 0, allParameters, 0, parameters.length);
        System.arraycopy(additionalParameters, 0, allParameters, parameters.length, additionalParameters.length);
        return allParameters;
    }

    private String resolveSortColumn(String sortName) {
        String column = SORT_COLUMNS.get(sortName);
        return column == null ? SORT_COLUMNS.get(CategoryListCriteria.DEFAULT_SORT_NAME) : column;
    }

    private String escapeLikePattern(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
