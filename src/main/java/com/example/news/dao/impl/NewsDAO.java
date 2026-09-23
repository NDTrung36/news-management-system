package com.example.news.dao.impl;

import com.example.news.dao.INewsDAO;
import com.example.news.mapper.NewsDetailMapper;
import com.example.news.mapper.NewsListItemMapper;
import com.example.news.mapper.NewsMapper;
import com.example.news.mapper.RowMapper;
import com.example.news.model.NewsDetail;
import com.example.news.model.NewsListCriteria;
import com.example.news.model.NewsListItem;
import com.example.news.model.NewsModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsDAO extends GenericDAO implements INewsDAO {

    private static final String LIST_SELECT = "SELECT n.id, n.title, n.thumbnail, n.category_id, "
            + "c.name AS category_name, u.full_name AS author_name, n.created_date "
            + "FROM news n "
            + "JOIN category c ON c.id = n.category_id "
            + "JOIN `user` u ON u.id = n.created_by";
    private static final String DETAIL_SELECT = "SELECT n.id, n.title, n.short_description, n.content, n.thumbnail, "
            + "n.category_id, n.created_by, n.created_date, n.modified_date, "
            + "c.name AS category_name, u.full_name AS author_name "
            + "FROM news n "
            + "JOIN category c ON c.id = n.category_id "
            + "JOIN `user` u ON u.id = n.created_by";
    private static final Map<String, String> SORT_COLUMNS;

    static {
        Map<String, String> columns = new HashMap<>();
        columns.put("title", "n.title");
        columns.put("createdDate", "n.created_date");
        SORT_COLUMNS = Collections.unmodifiableMap(columns);
    }

    private final RowMapper<NewsModel> newsMapper = new NewsMapper();
    private final RowMapper<NewsListItem> newsListItemMapper = new NewsListItemMapper();
    private final RowMapper<NewsDetail> newsDetailMapper = new NewsDetailMapper();

    @Override
    public List<NewsListItem> findPage(NewsListCriteria criteria, long offset, int limit) {
        StringBuilder sql = new StringBuilder(LIST_SELECT);
        List<Object> parameters = appendFilters(sql, criteria);
        String sortColumn = resolveSortColumn(criteria == null ? null : criteria.getSortName());
        String sortDirection = isDescending(criteria == null ? null : criteria.getSortBy()) ? "DESC" : "ASC";
        sql.append(" ORDER BY ").append(sortColumn).append(' ').append(sortDirection)
                .append(", n.id ").append(sortDirection)
                .append(" LIMIT ?, ?");
        parameters.add(offset);
        parameters.add(limit);
        return query(sql.toString(), newsListItemMapper, parameters.toArray());
    }

    @Override
    public long countByCriteria(NewsListCriteria criteria) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM news n");
        List<Object> parameters = appendFilters(sql, criteria);
        return count(sql.toString(), parameters.toArray());
    }

    @Override
    public NewsModel findById(Long id) {
        String sql = "SELECT id, title, short_description, content, thumbnail, category_id, created_by, "
                + "created_date, modified_date FROM news WHERE id = ?";
        return queryOne(sql, newsMapper, id);
    }

    @Override
    public NewsDetail findDetailById(Long id) {
        String sql = DETAIL_SELECT + " WHERE n.id = ?";
        return queryOne(sql, newsDetailMapper, id);
    }

    @Override
    public long insert(NewsModel news) {
        String sql = "INSERT INTO news (title, short_description, content, category_id, created_by) "
                + "VALUES (?, ?, ?, ?, ?)";
        return insert(sql, news.getTitle(), news.getShortDescription(), news.getContent(),
                news.getCategoryId(), news.getCreatedById());
    }

    @Override
    public int update(NewsModel news) {
        String sql = "UPDATE news SET title = ?, short_description = ?, content = ?, category_id = ?, "
                + "modified_date = CURRENT_TIMESTAMP WHERE id = ?";
        return update(sql, news.getTitle(), news.getShortDescription(), news.getContent(),
                news.getCategoryId(), news.getId());
    }

    @Override
    public int delete(Long id) {
        String sql = "DELETE FROM news WHERE id = ?";
        return delete(sql, id);
    }

    private List<Object> appendFilters(StringBuilder sql, NewsListCriteria criteria) {
        List<String> filters = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        String search = criteria == null ? null : trim(criteria.getSearch());
        if (search != null) {
            filters.add("(n.title LIKE ? ESCAPE '!' OR n.short_description LIKE ? ESCAPE '!')");
            String pattern = "%" + escapeLikePattern(search) + "%";
            parameters.add(pattern);
            parameters.add(pattern);
        }

        Long categoryId = criteria == null ? null : criteria.getCategoryId();
        if (categoryId != null && categoryId > 0) {
            filters.add("n.category_id = ?");
            parameters.add(categoryId);
        }

        if (!filters.isEmpty()) {
            sql.append(" WHERE ");
            for (int index = 0; index < filters.size(); index++) {
                if (index > 0) {
                    sql.append(" AND ");
                }
                sql.append(filters.get(index));
            }
        }
        return parameters;
    }

    private String resolveSortColumn(String sortName) {
        String column = SORT_COLUMNS.get(sortName);
        return column == null ? SORT_COLUMNS.get(NewsListCriteria.DEFAULT_SORT_NAME) : column;
    }

    private boolean isDescending(String sortBy) {
        return "desc".equalsIgnoreCase(sortBy);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeLikePattern(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
