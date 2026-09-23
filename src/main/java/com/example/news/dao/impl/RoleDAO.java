package com.example.news.dao.impl;

import com.example.news.dao.IRoleDAO;
import com.example.news.mapper.RoleMapper;
import com.example.news.mapper.RowMapper;
import com.example.news.model.RoleModel;

import java.util.List;

public class RoleDAO extends GenericDAO implements IRoleDAO {

    private static final String ROLE_COLUMNS = "r.id, r.code, r.name";

    private final RowMapper<RoleModel> roleMapper = new RoleMapper();

    @Override
    public List<RoleModel> findByUserId(Long userId) {
        String sql = "SELECT " + ROLE_COLUMNS + " FROM `role` r "
                + "INNER JOIN user_role ur ON ur.role_id = r.id "
                + "WHERE ur.user_id = ? ORDER BY r.id";
        return query(sql, roleMapper, userId);
    }

    @Override
    public RoleModel findByCode(String code) {
        String sql = "SELECT " + ROLE_COLUMNS + " FROM `role` r WHERE r.code = ?";
        return queryOne(sql, roleMapper, code);
    }
}
