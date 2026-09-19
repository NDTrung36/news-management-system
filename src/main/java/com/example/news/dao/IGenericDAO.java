package com.example.news.dao;

import com.example.news.mapper.RowMapper;

import java.sql.Connection;
import java.util.List;

public interface IGenericDAO {

    <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters);

    <T> T queryOne(String sql, RowMapper<T> mapper, Object... parameters);

    long insert(String sql, Object... parameters);

    long insert(Connection connection, String sql, Object... parameters);

    int update(String sql, Object... parameters);

    int update(Connection connection, String sql, Object... parameters);

    int delete(String sql, Object... parameters);

    long count(String sql, Object... parameters);
}
