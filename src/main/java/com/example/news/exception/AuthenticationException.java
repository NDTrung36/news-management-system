package com.example.news.exception;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
