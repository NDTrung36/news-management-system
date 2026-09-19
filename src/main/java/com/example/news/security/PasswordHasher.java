package com.example.news.security;

public interface PasswordHasher {

    String hash(String password);

    boolean verify(String password, String passwordHash);
}
