package com.example.news.security;

import java.io.Serializable;

public final class AuthenticatedUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String fullName;

    public AuthenticatedUser(Long id, String username, String fullName) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }
}
