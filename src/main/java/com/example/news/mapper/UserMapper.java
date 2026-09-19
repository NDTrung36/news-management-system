package com.example.news.mapper;

import com.example.news.model.UserModel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UserMapper implements RowMapper<UserModel> {

    @Override
    public UserModel mapRow(ResultSet resultSet) throws SQLException {
        UserModel user = new UserModel();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password"));
        user.setFullName(resultSet.getString("full_name"));
        user.setEmail(resultSet.getString("email"));
        user.setStatus(resultSet.getInt("status"));

        Timestamp createdDate = resultSet.getTimestamp("created_date");
        if (createdDate != null) {
            user.setCreatedDate(createdDate.toLocalDateTime());
        }

        Timestamp modifiedDate = resultSet.getTimestamp("modified_date");
        if (modifiedDate != null) {
            user.setModifiedDate(modifiedDate.toLocalDateTime());
        }
        return user;
    }
}
