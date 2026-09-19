package com.example.news.dao.impl;

import com.example.news.dao.IGenericDAO;
import com.example.news.dao.IUserDAO;
import com.example.news.exception.DatabaseException;
import com.example.news.exception.DuplicateUserException;
import com.example.news.mapper.RowMapper;
import com.example.news.mapper.UserMapper;
import com.example.news.model.UserModel;
import com.example.news.utils.DatabaseUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class UserDAO implements IUserDAO {

    private static final String USER_COLUMNS = "id, username, password, full_name, email, status, created_date, modified_date";
    private static final String USER_ROLE_CODE = "USER";

    private final IGenericDAO genericDAO;
    private final RowMapper<UserModel> userMapper;

    public UserDAO() {
        this(new GenericDAO());
    }

    public UserDAO(IGenericDAO genericDAO) {
        this.genericDAO = genericDAO;
        this.userMapper = new UserMapper();
    }

    @Override
    public UserModel findByUsername(String username) {
        String sql = "SELECT " + USER_COLUMNS + " FROM `user` WHERE username = ?";
        return genericDAO.queryOne(sql, userMapper, username);
    }

    @Override
    public UserModel findByEmail(String email) {
        String sql = "SELECT " + USER_COLUMNS + " FROM `user` WHERE email = ?";
        return genericDAO.queryOne(sql, userMapper, email);
    }

    @Override
    public long insertWithDefaultRole(UserModel user) {
        try (Connection connection = DatabaseUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String insertUser = "INSERT INTO `user` (username, password, full_name, email, status) VALUES (?, ?, ?, ?, ?)";
                long userId = genericDAO.insert(connection, insertUser,
                        user.getUsername(), user.getPasswordHash(), user.getFullName(), user.getEmail(), user.getStatus());

                String insertRole = "INSERT INTO user_role (user_id, role_id) "
                        + "SELECT ?, id FROM `role` WHERE code = ?";
                int mappedRows = genericDAO.update(connection, insertRole, userId, USER_ROLE_CODE);
                if (mappedRows != 1) {
                    throw new DatabaseException("Default USER role could not be assigned");
                }

                connection.commit();
                return userId;
            } catch (DatabaseException e) {
                rollback(connection, e);
                if (isDuplicateKey(e)) {
                    throw new DuplicateUserException("Username or email already exists", e);
                }
                throw e;
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error registering user", e);
        }
    }

    private void rollback(Connection connection, RuntimeException original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    private boolean isDuplicateKey(DatabaseException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException) {
                SQLException sqlException = (SQLException) current;
                if (sqlException.getErrorCode() == 1062
                        || (sqlException.getSQLState() != null && sqlException.getSQLState().startsWith("23"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
