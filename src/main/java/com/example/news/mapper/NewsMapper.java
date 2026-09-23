package com.example.news.mapper;

import com.example.news.model.NewsModel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class NewsMapper implements RowMapper<NewsModel> {

    @Override
    public NewsModel mapRow(ResultSet resultSet) throws SQLException {
        NewsModel news = new NewsModel();
        news.setId(resultSet.getLong("id"));
        news.setTitle(resultSet.getString("title"));
        news.setShortDescription(resultSet.getString("short_description"));
        news.setContent(resultSet.getString("content"));
        news.setThumbnail(resultSet.getString("thumbnail"));
        news.setCategoryId(resultSet.getLong("category_id"));
        news.setCreatedById(resultSet.getLong("created_by"));
        setTimestamps(news, resultSet);
        return news;
    }

    static void setTimestamps(NewsModel news, ResultSet resultSet) throws SQLException {
        Timestamp createdDate = resultSet.getTimestamp("created_date");
        if (createdDate != null) {
            news.setCreatedDate(createdDate.toLocalDateTime());
        }

        Timestamp modifiedDate = resultSet.getTimestamp("modified_date");
        if (modifiedDate != null) {
            news.setModifiedDate(modifiedDate.toLocalDateTime());
        }
    }
}
