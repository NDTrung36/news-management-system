package com.example.news.service;

import com.example.news.model.RegisterForm;
import com.example.news.security.AuthenticatedUser;

public interface IAuthService {

    long register(RegisterForm form);

    AuthenticatedUser login(String username, String password);
}
