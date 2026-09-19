package com.example.news.dao.impl;

import com.example.news.dao.IGenericDAO;
import com.example.news.exception.DatabaseException;
import com.example.news.mapper.RowMapper;
import com.example.news.utils.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class GenericDAO implements IGenericDAO {

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
        List<T> results = new ArrayList<>();
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapper.mapRow(resultSet));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Error executing query: " + sql, e);
        }
    }

    @Override
    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... parameters) {
        List<T> results = query(sql, mapper, parameters);
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new DatabaseException("Expected single row result, but found: " + results.size());
        }
        return results.get(0);
    }

    @Override
    public long insert(String sql, Object... parameters) {
        try (Connection connection = DatabaseUtil.getConnection()) {
            return insert(connection, sql, parameters);
        } catch (SQLException e) {
            throw new DatabaseException("Error closing connection after insert: " + sql, e);
        }
    }

    @Override
    public long insert(Connection connection, String sql, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setParameters(statement, parameters);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Insert failed, no rows affected: " + sql);
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new DatabaseException("Insert succeeded, but no generated key was retrieved: " + sql);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error executing insert: " + sql, e);
        }
    }

    @Override
    public int update(String sql, Object... parameters) {
        try (Connection connection = DatabaseUtil.getConnection()) {
            return update(connection, sql, parameters);
        } catch (SQLException e) {
            throw new DatabaseException("Error closing connection after update: " + sql, e);
        }
    }

    @Override
    public int update(Connection connection, String sql, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, parameters);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error executing update: " + sql, e);
        }
    }

    @Override
    public int delete(String sql, Object... parameters) {
        return update(sql, parameters);
    }

    @Override
    public long count(String sql, Object... parameters) {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            setParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                throw new DatabaseException("Count query did not return any results: " + sql);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error executing count: " + sql, e);
        }
    }

    private void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        if (parameters == null) {
            return;
        }
        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            int index = i + 1;
            if (param == null) {
                statement.setNull(index, Types.NULL);
            } else if (param instanceof String) {
                statement.setString(index, (String) param);
            } else if (param instanceof Long) {
                statement.setLong(index, (Long) param);
            } else if (param instanceof Integer) {
                statement.setInt(index, (Integer) param);
            } else if (param instanceof Boolean) {
                statement.setBoolean(index, (Boolean) param);
            } else if (param instanceof Timestamp) {
                statement.setTimestamp(index, (Timestamp) param);
            } else {
                statement.setObject(index, param);
            }
        }
    }
}
