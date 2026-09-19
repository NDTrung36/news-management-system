package com.example.news.model;

public class RegisterForm {

    private String username;
    private String password;
    private String confirmPassword;
    private String fullName;
    private String email;

    public RegisterForm() {
    }

    public RegisterForm(String username, String password, String confirmPassword, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.fullName = fullName;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
