package com.example.news.dao;

import com.example.news.mapper.RowMapper;

import java.util.List;

public interface IGenericDAO {

    <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters);

    <T> T queryOne(String sql, RowMapper<T> mapper, Object... parameters);

    long insert(String sql, Object... parameters);

    int update(String sql, Object... parameters);

    int delete(String sql, Object... parameters);

    long count(String sql, Object... parameters);

    <T> T executeInTransaction(TransactionCallback<T> callback);

    @FunctionalInterface
    interface TransactionCallback<T> {

        T execute(IGenericDAO transactionDAO);
    }
}
