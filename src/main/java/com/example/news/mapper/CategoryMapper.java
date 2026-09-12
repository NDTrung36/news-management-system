package com.example.news.mapper;

import com.example.news.model.CategoryModel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CategoryMapper implements RowMapper<CategoryModel> {

    @Override
    public CategoryModel mapRow(ResultSet resultSet) throws SQLException {
        CategoryModel category = new CategoryModel();
        category.setId(resultSet.getLong("id"));
        category.setName(resultSet.getString("name"));
        category.setCode(resultSet.getString("code"));

        Timestamp createdTimestamp = resultSet.getTimestamp("created_date");
        if (createdTimestamp != null) {
            category.setCreatedDate(createdTimestamp.toLocalDateTime());
        }

        Timestamp modifiedTimestamp = resultSet.getTimestamp("modified_date");
        if (modifiedTimestamp != null) {
            category.setModifiedDate(modifiedTimestamp.toLocalDateTime());
        }

        return category;
    }
}
