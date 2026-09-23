package com.example.news.mapper;

import com.example.news.model.NewsDetail;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NewsDetailMapper implements RowMapper<NewsDetail> {

    @Override
    public NewsDetail mapRow(ResultSet resultSet) throws SQLException {
        NewsDetail news = new NewsDetail();
        news.setId(resultSet.getLong("id"));
        news.setTitle(resultSet.getString("title"));
        news.setShortDescription(resultSet.getString("short_description"));
        news.setContent(resultSet.getString("content"));
        news.setThumbnail(resultSet.getString("thumbnail"));
        news.setCategoryId(resultSet.getLong("category_id"));
        news.setCreatedById(resultSet.getLong("created_by"));
        news.setCategoryName(resultSet.getString("category_name"));
        news.setAuthorName(resultSet.getString("author_name"));
        NewsMapper.setTimestamps(news, resultSet);
        return news;
    }
}
