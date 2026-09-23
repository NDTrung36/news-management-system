package com.example.news.service.impl;

import com.example.news.dao.ICategoryDAO;
import com.example.news.dao.INewsDAO;
import com.example.news.dao.IUserDAO;
import com.example.news.dao.impl.CategoryDAO;
import com.example.news.dao.impl.NewsDAO;
import com.example.news.dao.impl.UserDAO;
import com.example.news.exception.DatabaseException;
import com.example.news.exception.ValidationException;
import com.example.news.model.CategoryModel;
import com.example.news.model.NewsDetail;
import com.example.news.model.NewsListCriteria;
import com.example.news.model.NewsListItem;
import com.example.news.model.NewsModel;
import com.example.news.model.PageResult;
import com.example.news.model.UserModel;
import com.example.news.service.INewsService;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class NewsService implements INewsService {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_SHORT_DESCRIPTION_LENGTH = 500;

    private final INewsDAO newsDAO;
    private final ICategoryDAO categoryDAO;
    private final IUserDAO userDAO;

    public NewsService() {
        this(new NewsDAO(), new CategoryDAO(), new UserDAO());
    }

    public NewsService(INewsDAO newsDAO, ICategoryDAO categoryDAO, IUserDAO userDAO) {
        this.newsDAO = newsDAO;
        this.categoryDAO = categoryDAO;
        this.userDAO = userDAO;
    }

    @Override
    public PageResult<NewsListItem> search(NewsListCriteria criteria) {
        NewsListCriteria normalizedCriteria = normalizeCriteria(criteria);
        long totalItems = newsDAO.countByCriteria(normalizedCriteria);
        int totalPages = calculateTotalPages(totalItems, PAGE_SIZE);
        int currentPage = normalizePage(normalizedCriteria.getPage(), totalPages);
        normalizedCriteria.setPage(currentPage);

        long offset = ((long) currentPage - 1L) * PAGE_SIZE;
        List<NewsListItem> items = totalItems == 0
                ? Collections.emptyList()
                : newsDAO.findPage(normalizedCriteria, offset, PAGE_SIZE);
        return new PageResult<>(items, currentPage, PAGE_SIZE, totalItems, totalPages);
    }

    @Override
    public NewsModel findById(Long id) {
        return isPositiveId(id) ? newsDAO.findById(id) : null;
    }

    @Override
    public NewsDetail findDetailById(Long id) {
        return isPositiveId(id) ? newsDAO.findDetailById(id) : null;
    }

    @Override
    public long create(NewsModel news, Long authorId) {
        validateAndNormalize(news);
        validateCategory(news.getCategoryId());
        validateAuthor(authorId);
        news.setCreatedById(authorId);

        try {
            return newsDAO.insert(news);
        } catch (DatabaseException e) {
            if (isForeignKeyViolation(e)) {
                throw unavailableRelation(e);
            }
            throw e;
        }
    }

    @Override
    public boolean update(NewsModel news) {
        if (news == null || !isPositiveId(news.getId())) {
            throw new ValidationException("News ID is invalid or missing");
        }

        NewsModel existing = newsDAO.findById(news.getId());
        if (existing == null) {
            return false;
        }

        validateAndNormalize(news);
        validateCategory(news.getCategoryId());
        preserveImmutableFields(news, existing);

        try {
            return newsDAO.update(news) > 0;
        } catch (DatabaseException e) {
            if (isForeignKeyViolation(e)) {
                throw unavailableRelation(e);
            }
            throw e;
        }
    }

    @Override
    public boolean delete(Long id) {
        if (!isPositiveId(id)) {
            throw new ValidationException("News ID is invalid or missing");
        }
        if (newsDAO.findById(id) == null) {
            return false;
        }
        return newsDAO.delete(id) > 0;
    }

    private void validateAndNormalize(NewsModel news) {
        if (news == null) {
            throw new ValidationException("News cannot be null");
        }
        news.setTitle(validateRequiredText(news.getTitle(), "Title", MAX_TITLE_LENGTH));
        news.setShortDescription(validateRequiredText(news.getShortDescription(), "Short description",
                MAX_SHORT_DESCRIPTION_LENGTH));
        news.setContent(validateRequiredText(news.getContent(), "Content", Integer.MAX_VALUE));
        if (!isPositiveId(news.getCategoryId())) {
            throw new ValidationException("Category is required");
        }
    }

    private void validateCategory(Long categoryId) {
        CategoryModel category = categoryDAO.findById(categoryId);
        if (category == null) {
            throw new ValidationException("Selected category does not exist");
        }
    }

    private void validateAuthor(Long authorId) {
        if (!isPositiveId(authorId)) {
            throw new ValidationException("Authenticated author is invalid");
        }
        UserModel author = userDAO.findById(authorId);
        if (author == null) {
            throw new ValidationException("Authenticated author is not available");
        }
    }

    private NewsListCriteria normalizeCriteria(NewsListCriteria criteria) {
        NewsListCriteria normalized = criteria == null ? new NewsListCriteria() : criteria;

        String search = trim(normalized.getSearch());
        if (search.length() > NewsListCriteria.MAX_SEARCH_LENGTH) {
            throw new ValidationException("Search must not exceed "
                    + NewsListCriteria.MAX_SEARCH_LENGTH + " characters");
        }
        normalized.setSearch(search);

        if (normalized.getCategoryId() != null && !isPositiveId(normalized.getCategoryId())) {
            throw new ValidationException("Category filter is invalid");
        }

        String sortName = trim(normalized.getSortName());
        if (!isSupportedSortName(sortName)) {
            normalized.setSortName(NewsListCriteria.DEFAULT_SORT_NAME);
        } else {
            normalized.setSortName(sortName);
        }

        String sortBy = trim(normalized.getSortBy());
        normalized.setSortBy("asc".equalsIgnoreCase(sortBy) ? "asc" : NewsListCriteria.DEFAULT_SORT_BY);

        if (normalized.getPage() < NewsListCriteria.DEFAULT_PAGE) {
            normalized.setPage(NewsListCriteria.DEFAULT_PAGE);
        }
        return normalized;
    }

    private void preserveImmutableFields(NewsModel news, NewsModel existing) {
        news.setCreatedById(existing.getCreatedById());
        news.setThumbnail(existing.getThumbnail());
        news.setCreatedDate(existing.getCreatedDate());
        news.setModifiedDate(existing.getModifiedDate());
    }

    private String validateRequiredText(String value, String fieldName, int maxLength) {
        String normalized = trim(value);
        if (normalized.isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
        if (normalized.length() > maxLength) {
            throw new ValidationException(fieldName + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private int calculateTotalPages(long totalItems, int pageSize) {
        if (totalItems <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, ((totalItems - 1L) / pageSize) + 1L);
    }

    private int normalizePage(int requestedPage, int totalPages) {
        if (totalPages == 0) {
            return NewsListCriteria.DEFAULT_PAGE;
        }
        return Math.min(Math.max(requestedPage, NewsListCriteria.DEFAULT_PAGE), totalPages);
    }

    private boolean isSupportedSortName(String sortName) {
        return "title".equals(sortName) || "createdDate".equals(sortName);
    }

    private boolean isPositiveId(Long id) {
        return id != null && id > 0;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private ValidationException unavailableRelation(DatabaseException cause) {
        return new ValidationException("Selected category or author is no longer available", cause);
    }

    private boolean isForeignKeyViolation(DatabaseException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException) {
                int errorCode = ((SQLException) current).getErrorCode();
                if (errorCode == 1451 || errorCode == 1452 || errorCode == 1216 || errorCode == 1217) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
