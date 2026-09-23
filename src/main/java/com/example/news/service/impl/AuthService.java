package com.example.news.service.impl;

import com.example.news.dao.IUserDAO;
import com.example.news.dao.impl.UserDAO;
import com.example.news.exception.AuthenticationException;
import com.example.news.exception.DuplicateUserException;
import com.example.news.exception.ValidationException;
import com.example.news.model.RegisterForm;
import com.example.news.model.UserModel;
import com.example.news.security.AuthenticatedUser;
import com.example.news.security.BCryptPasswordHasher;
import com.example.news.security.PasswordHasher;
import com.example.news.service.IAuthService;
import com.example.news.service.IRoleService;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

public class AuthService implements IAuthService {

    private static final int ACTIVE_STATUS = 1;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_FULL_NAME_LENGTH = 150;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String INVALID_CREDENTIALS_MESSAGE = "Username hoặc password không đúng. Vui lòng kiểm tra và thử lại.";

    private final IUserDAO userDAO;
    private final IRoleService roleService;
    private final PasswordHasher passwordHasher;
    private final String dummyPasswordHash;

    public AuthService() {
        this(new UserDAO(), new RoleService(), new BCryptPasswordHasher());
    }

    public AuthService(IUserDAO userDAO, IRoleService roleService, PasswordHasher passwordHasher) {
        this.userDAO = userDAO;
        this.roleService = roleService;
        this.passwordHasher = passwordHasher;
        this.dummyPasswordHash = passwordHasher.hash("dummy-password-not-used");
    }

    @Override
    public long register(RegisterForm form) {
        validateAndNormalize(form);

        if (userDAO.findByUsername(form.getUsername()) != null) {
            throw new ValidationException("Username already exists");
        }
        if (userDAO.findByEmail(form.getEmail()) != null) {
            throw new ValidationException("Email already exists");
        }

        UserModel user = new UserModel();
        user.setUsername(form.getUsername());
        user.setPasswordHash(passwordHasher.hash(form.getPassword()));
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setStatus(ACTIVE_STATUS);

        try {
            return userDAO.insertWithDefaultRole(user);
        } catch (DuplicateUserException e) {
            throw new ValidationException("Username or email already exists", e);
        }
    }

    @Override
    public AuthenticatedUser login(String username, String password) {
        String normalizedUsername = trimToNull(username);
        if (normalizedUsername == null || password == null || password.trim().isEmpty()) {
            throw invalidCredentials();
        }

        UserModel user = userDAO.findByUsername(normalizedUsername);
        if (user == null) {
            passwordHasher.verify(password, dummyPasswordHash);
            throw invalidCredentials();
        }

        boolean passwordMatches = passwordHasher.verify(password, user.getPasswordHash());
        if (!passwordMatches || user.getStatus() != ACTIVE_STATUS) {
            throw invalidCredentials();
        }

        Set<String> roleCodes = roleService.findRoleCodesByUserId(user.getId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw invalidCredentials();
        }

        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getFullName(), roleCodes);
    }

    private void validateAndNormalize(RegisterForm form) {
        if (form == null) {
            throw new ValidationException("Registration data cannot be null");
        }

        form.setUsername(validateRequiredText(form.getUsername(), "Username", MAX_USERNAME_LENGTH));
        form.setFullName(validateRequiredText(form.getFullName(), "Full name", MAX_FULL_NAME_LENGTH));

        String email = validateRequiredText(form.getEmail(), "Email", MAX_EMAIL_LENGTH);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Email format is invalid");
        }
        form.setEmail(email);

        String password = form.getPassword();
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password cannot be empty");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must contain at least 6 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ValidationException("Password must not exceed 72 UTF-8 bytes");
        }
        if (!password.equals(form.getConfirmPassword())) {
            throw new ValidationException("Password confirmation does not match");
        }
    }

    private String validateRequiredText(String value, String fieldName, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
        if (normalized.length() > maxLength) {
            throw new ValidationException(fieldName + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AuthenticationException invalidCredentials() {
        return new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
    }
}
