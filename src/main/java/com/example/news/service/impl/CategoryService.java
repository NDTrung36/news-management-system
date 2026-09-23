package com.example.news.service.impl;

import com.example.news.dao.ICategoryDAO;
import com.example.news.dao.impl.CategoryDAO;
import com.example.news.exception.DatabaseException;
import com.example.news.exception.ValidationException;
import com.example.news.model.CategoryModel;
import com.example.news.model.CategoryListCriteria;
import com.example.news.model.PageResult;
import com.example.news.service.ICategoryService;

import java.util.Collections;
import java.util.List;

public class CategoryService implements ICategoryService {

    private static final int PAGE_SIZE = 10;

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
    public PageResult<CategoryModel> search(CategoryListCriteria criteria) {
        CategoryListCriteria normalizedCriteria = normalizeCriteria(criteria);
        long totalItems = categoryDAO.countBySearch(normalizedCriteria.getSearch());
        int totalPages = calculateTotalPages(totalItems, PAGE_SIZE);
        int currentPage = normalizePage(normalizedCriteria.getPage(), totalPages);
        normalizedCriteria.setPage(currentPage);

        long offset = ((long) currentPage - 1L) * PAGE_SIZE;
        List<CategoryModel> categories = totalItems == 0
                ? Collections.emptyList()
                : categoryDAO.findPage(normalizedCriteria, offset, PAGE_SIZE);
        return new PageResult<>(categories, currentPage, PAGE_SIZE, totalItems, totalPages);
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

        try {
            return categoryDAO.insert(category);
        } catch (DatabaseException e) {
            if (isDuplicateKey(e)) {
                throw duplicateCodeException(category.getCode(), e);
            }
            throw e;
        }
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

        try {
            return categoryDAO.update(category) > 0;
        } catch (DatabaseException e) {
            if (isDuplicateKey(e)) {
                throw duplicateCodeException(category.getCode(), e);
            }
            throw e;
        }
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

        try {
            return categoryDAO.delete(id) > 0;
        } catch (DatabaseException e) {
            if (isForeignKeyViolation(e)) {
                throw new ValidationException(
                        "Cannot delete category because it is being used by news articles", e);
            }
            throw e;
        }
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

    private CategoryListCriteria normalizeCriteria(CategoryListCriteria criteria) {
        CategoryListCriteria normalized = criteria == null
                ? new CategoryListCriteria()
                : criteria;

        String search = normalized.getSearch() == null ? "" : normalized.getSearch().trim();
        if (search.length() > CategoryListCriteria.MAX_SEARCH_LENGTH) {
            throw new ValidationException("Search must not exceed "
                    + CategoryListCriteria.MAX_SEARCH_LENGTH + " characters");
        }
        normalized.setSearch(search);

        String sortName = normalized.getSortName() == null ? "" : normalized.getSortName().trim();
        normalized.setSortName(sortName);
        if (!isSupportedSortName(sortName)) {
            normalized.setSortName(CategoryListCriteria.DEFAULT_SORT_NAME);
        }
        String sortBy = normalized.getSortBy() == null ? "" : normalized.getSortBy().trim();
        if (!"desc".equalsIgnoreCase(sortBy)) {
            normalized.setSortBy(CategoryListCriteria.DEFAULT_SORT_BY);
        } else {
            normalized.setSortBy("desc");
        }
        if (normalized.getPage() < 1) {
            normalized.setPage(CategoryListCriteria.DEFAULT_PAGE);
        }
        return normalized;
    }

    private boolean isSupportedSortName(String sortName) {
        return "id".equals(sortName)
                || "name".equals(sortName)
                || "code".equals(sortName)
                || "createdDate".equals(sortName);
    }

    private int calculateTotalPages(long totalItems, int pageSize) {
        if (totalItems <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, ((totalItems - 1L) / pageSize) + 1L);
    }

    private int normalizePage(int requestedPage, int totalPages) {
        if (totalPages == 0) {
            return CategoryListCriteria.DEFAULT_PAGE;
        }
        return Math.min(Math.max(requestedPage, CategoryListCriteria.DEFAULT_PAGE), totalPages);
    }

    private ValidationException duplicateCodeException(String code, Throwable cause) {
        return new ValidationException("Category code already exists: " + code, cause);
    }

    private boolean isDuplicateKey(DatabaseException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof java.sql.SQLException) {
                java.sql.SQLException sqlException = (java.sql.SQLException) current;
                if (sqlException.getErrorCode() == 1062
                        || (sqlException.getSQLState() != null
                        && sqlException.getSQLState().startsWith("23"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isForeignKeyViolation(DatabaseException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof java.sql.SQLException) {
                java.sql.SQLException sqlException = (java.sql.SQLException) current;
                if (sqlException.getErrorCode() == 1451
                        || sqlException.getErrorCode() == 1217) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
