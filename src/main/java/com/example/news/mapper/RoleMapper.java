package com.example.news.mapper;

import com.example.news.model.RoleModel;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RoleMapper implements RowMapper<RoleModel> {

    @Override
    public RoleModel mapRow(ResultSet resultSet) throws SQLException {
        RoleModel role = new RoleModel();
        role.setId(resultSet.getLong("id"));
        role.setCode(resultSet.getString("code"));
        role.setName(resultSet.getString("name"));
        return role;
    }
}
