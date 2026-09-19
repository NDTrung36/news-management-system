package com.example.news.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class BCryptPasswordHasher implements PasswordHasher {

    public static final int COST = 12;

    @Override
    public String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray());
    }

    @Override
    public boolean verify(String password, String passwordHash) {
        if (password == null || passwordHash == null || passwordHash.trim().isEmpty()) {
            return false;
        }
        try {
            return BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
