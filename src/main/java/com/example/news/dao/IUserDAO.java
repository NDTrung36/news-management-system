package com.example.news.dao;

import com.example.news.model.UserModel;

public interface IUserDAO {

    UserModel findById(Long id);

    UserModel findByUsername(String username);

    UserModel findByEmail(String email);

    long insertWithDefaultRole(UserModel user);
}
