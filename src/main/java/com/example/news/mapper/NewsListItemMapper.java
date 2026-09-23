package com.example.news.mapper;

import com.example.news.model.NewsListItem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class NewsListItemMapper implements RowMapper<NewsListItem> {

    @Override
    public NewsListItem mapRow(ResultSet resultSet) throws SQLException {
        NewsListItem item = new NewsListItem();
        item.setId(resultSet.getLong("id"));
        item.setTitle(resultSet.getString("title"));
        item.setThumbnail(resultSet.getString("thumbnail"));
        item.setCategoryId(resultSet.getLong("category_id"));
        item.setCategoryName(resultSet.getString("category_name"));
        item.setAuthorName(resultSet.getString("author_name"));

        Timestamp createdDate = resultSet.getTimestamp("created_date");
        if (createdDate != null) {
            item.setCreatedDate(createdDate.toLocalDateTime());
        }
        return item;
    }
}
