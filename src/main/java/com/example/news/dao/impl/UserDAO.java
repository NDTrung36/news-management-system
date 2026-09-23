package com.example.news.dao.impl;

import com.example.news.dao.IUserDAO;
import com.example.news.exception.DatabaseException;
import com.example.news.exception.DuplicateUserException;
import com.example.news.mapper.RowMapper;
import com.example.news.mapper.UserMapper;
import com.example.news.model.UserModel;

import java.sql.SQLException;

public class UserDAO extends GenericDAO implements IUserDAO {

    private static final String USER_COLUMNS = "id, username, password, full_name, email, status, created_date, modified_date";
    private static final String USER_ROLE_CODE = "USER";

    private final RowMapper<UserModel> userMapper = new UserMapper();

    @Override
    public UserModel findById(Long id) {
        String sql = "SELECT " + USER_COLUMNS + " FROM `user` WHERE id = ?";
        return queryOne(sql, userMapper, id);
    }

    @Override
    public UserModel findByUsername(String username) {
        String sql = "SELECT " + USER_COLUMNS + " FROM `user` WHERE username = ?";
        return queryOne(sql, userMapper, username);
    }

    @Override
    public UserModel findByEmail(String email) {
        String sql = "SELECT " + USER_COLUMNS + " FROM `user` WHERE email = ?";
        return queryOne(sql, userMapper, email);
    }

    @Override
    public long insertWithDefaultRole(UserModel user) {
        try {
            return executeInTransaction(transactionDAO -> {
                String insertUser = "INSERT INTO `user` (username, password, full_name, email, status) VALUES (?, ?, ?, ?, ?)";
                long userId = transactionDAO.insert(insertUser,
                        user.getUsername(), user.getPasswordHash(), user.getFullName(), user.getEmail(), user.getStatus());

                String insertRole = "INSERT INTO user_role (user_id, role_id) "
                        + "SELECT ?, id FROM `role` WHERE code = ?";
                int mappedRows = transactionDAO.update(insertRole, userId, USER_ROLE_CODE);
                if (mappedRows != 1) {
                    throw new DatabaseException("Default USER role could not be assigned");
                }
                return userId;
            });
        } catch (DatabaseException e) {
            if (isDuplicateKey(e)) {
                throw new DuplicateUserException("Username or email already exists", e);
            }
            throw e;
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
