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
import java.util.Objects;
import java.util.function.Supplier;

public class GenericDAO implements IGenericDAO {

    private final Supplier<Connection> connectionProvider;

    public GenericDAO() {
        this(DatabaseUtil::getConnection);
    }

    GenericDAO(Supplier<Connection> connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
        return withConnection("query", sql,
                connection -> query(connection, sql, mapper, parameters));
    }

    @Override
    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... parameters) {
        return withConnection("query one", sql,
                connection -> queryOne(connection, sql, mapper, parameters));
    }

    @Override
    public long insert(String sql, Object... parameters) {
        return withConnection("insert", sql,
                connection -> insert(connection, sql, parameters));
    }

    @Override
    public int update(String sql, Object... parameters) {
        return withConnection("update", sql,
                connection -> update(connection, sql, parameters));
    }

    @Override
    public int delete(String sql, Object... parameters) {
        return withConnection("delete", sql,
                connection -> update(connection, sql, parameters));
    }

    @Override
    public long count(String sql, Object... parameters) {
        return withConnection("count", sql,
                connection -> count(connection, sql, parameters));
    }

    @Override
    public <T> T executeInTransaction(TransactionCallback<T> callback) {
        Objects.requireNonNull(callback, "Transaction callback cannot be null");
        return withConnection("transaction", null, connection -> {
            boolean transactionStarted = false;
            try {
                connection.setAutoCommit(false);
                transactionStarted = true;

                T result = callback.execute(new TransactionScopedDAO(connection));
                connection.commit();
                return result;
            } catch (SQLException e) {
                DatabaseException wrapped = new DatabaseException("Error completing database transaction", e);
                if (transactionStarted) {
                    rollback(connection, wrapped);
                }
                throw wrapped;
            } catch (RuntimeException | Error e) {
                if (transactionStarted) {
                    rollback(connection, e);
                }
                throw e;
            }
        });
    }

    private <T> List<T> query(Connection connection, String sql, RowMapper<T> mapper,
                              Object... parameters) throws SQLException {
        Objects.requireNonNull(mapper, "Row mapper cannot be null");
        return executeSelect(connection, sql, resultSet -> {
            List<T> results = new ArrayList<>();
            while (resultSet.next()) {
                results.add(mapper.mapRow(resultSet));
            }
            return results;
        }, parameters);
    }

    private <T> T queryOne(Connection connection, String sql, RowMapper<T> mapper,
                           Object... parameters) throws SQLException {
        List<T> results = query(connection, sql, mapper, parameters);
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new DatabaseException("Expected single row result, but found: " + results.size());
        }
        return results.get(0);
    }

    private long insert(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(statement, parameters);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Insert failed, no rows affected: " + sql);
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new DatabaseException("Insert succeeded, but no generated key was retrieved: " + sql);
            }
        }
    }

    private int update(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, parameters);
            return statement.executeUpdate();
        }
    }

    private long count(Connection connection, String sql, Object... parameters) throws SQLException {
        return executeSelect(connection, sql, resultSet -> {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            throw new DatabaseException("Count query did not return any results: " + sql);
        }, parameters);
    }

    private <T> T executeSelect(Connection connection, String sql, ResultSetExtractor<T> extractor,
                                Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                return extractor.extract(resultSet);
            }
        }
    }

    private <T> T withConnection(String operation, String sql, ConnectionCallback<T> callback) {
        try (Connection connection = connectionProvider.get()) {
            if (connection == null) {
                throw new DatabaseException("Connection provider returned null");
            }
            return callback.execute(connection);
        } catch (DatabaseException e) {
            throw e;
        } catch (SQLException e) {
            String suffix = sql == null ? "" : ": " + sql;
            throw new DatabaseException("Error executing " + operation + suffix, e);
        }
    }

    private void rollback(Connection connection, Throwable original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
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

    @FunctionalInterface
    private interface ConnectionCallback<T> {

        T execute(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface ResultSetExtractor<T> {

        T extract(ResultSet resultSet) throws SQLException;
    }

    private final class TransactionScopedDAO implements IGenericDAO {

        private final Connection connection;

        private TransactionScopedDAO(Connection connection) {
            this.connection = connection;
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
            return execute("query", sql, () -> GenericDAO.this.query(connection, sql, mapper, parameters));
        }

        @Override
        public <T> T queryOne(String sql, RowMapper<T> mapper, Object... parameters) {
            return execute("query one", sql, () -> GenericDAO.this.queryOne(connection, sql, mapper, parameters));
        }

        @Override
        public long insert(String sql, Object... parameters) {
            return execute("insert", sql, () -> GenericDAO.this.insert(connection, sql, parameters));
        }

        @Override
        public int update(String sql, Object... parameters) {
            return execute("update", sql, () -> GenericDAO.this.update(connection, sql, parameters));
        }

        @Override
        public int delete(String sql, Object... parameters) {
            return execute("delete", sql, () -> GenericDAO.this.update(connection, sql, parameters));
        }

        @Override
        public long count(String sql, Object... parameters) {
            return execute("count", sql, () -> GenericDAO.this.count(connection, sql, parameters));
        }

        @Override
        public <T> T executeInTransaction(TransactionCallback<T> callback) {
            Objects.requireNonNull(callback, "Transaction callback cannot be null");
            return callback.execute(this);
        }

        private <T> T execute(String operation, String sql, SqlCallback<T> callback) {
            try {
                return callback.execute();
            } catch (DatabaseException e) {
                throw e;
            } catch (SQLException e) {
                throw new DatabaseException("Error executing " + operation + ": " + sql, e);
            }
        }
    }

    @FunctionalInterface
    private interface SqlCallback<T> {

        T execute() throws SQLException;
    }
}
