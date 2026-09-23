package com.example.news.service;

import com.example.news.dao.impl.GenericDAO;
import com.example.news.dao.impl.RoleDAO;
import com.example.news.dao.impl.UserDAO;
import com.example.news.exception.AuthenticationException;
import com.example.news.mapper.UserMapper;
import com.example.news.model.RegisterForm;
import com.example.news.model.UserModel;
import com.example.news.security.AuthenticatedUser;
import com.example.news.security.BCryptPasswordHasher;
import com.example.news.service.impl.AuthService;
import com.example.news.service.impl.RoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "runDbTests", matches = "true")
public class AuthServiceIntegrationTest {

    private static final String TEST_USERNAME = "auth-sprint4-int";
    private static final String TEST_EMAIL = "auth-sprint4-int@example.com";

    private GenericDAO genericDAO;
    private IAuthService authService;

    @BeforeEach
    public void setUp() {
        genericDAO = new GenericDAO();
        cleanup();
        authService = new AuthService(new UserDAO(), new RoleService(new RoleDAO()), new BCryptPasswordHasher());
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    @Test
    public void registerAndLoginShouldPersistHashedUserWithDefaultUserRole() {
        long userId = authService.register(new RegisterForm(TEST_USERNAME, "secret1", "secret1",
                "Sprint Four User", TEST_EMAIL));

        UserModel savedUser = genericDAO.queryOne(
                "SELECT id, username, password, full_name, email, status, created_date, modified_date FROM `user` WHERE id = ?",
                new UserMapper(), userId);
        assertNotNull(savedUser);
        assertEquals(TEST_USERNAME, savedUser.getUsername());
        assertNotEquals("secret1", savedUser.getPasswordHash());
        assertEquals(1, savedUser.getStatus());
        assertNotNull(savedUser.getCreatedDate());

        long userRoleCount = genericDAO.count(
                "SELECT COUNT(*) FROM user_role ur JOIN `role` r ON r.id = ur.role_id WHERE ur.user_id = ? AND r.code = ?",
                userId, "USER");
        assertEquals(1L, userRoleCount);

        AuthenticatedUser authenticatedUser = authService.login(TEST_USERNAME, "secret1");
        assertEquals(userId, authenticatedUser.getId().longValue());
        assertTrue(authenticatedUser.getFullName().contains("Sprint Four"));
        assertTrue(authenticatedUser.hasRole("USER"));
        assertFalse(authenticatedUser.hasRole("ADMIN"));
    }

    @Test
    public void loginShouldLoadAdminRoleAssignedInDatabase() {
        long userId = authService.register(new RegisterForm(TEST_USERNAME, "secret1", "secret1",
                "Sprint Five Admin", TEST_EMAIL));
        int insertedRoles = genericDAO.update(
                "INSERT INTO user_role (user_id, role_id) SELECT ?, id FROM `role` WHERE code = ?",
                userId, "ADMIN");
        assertEquals(1, insertedRoles);

        AuthenticatedUser authenticatedUser = authService.login(TEST_USERNAME, "secret1");

        assertTrue(authenticatedUser.hasRole("USER"));
        assertTrue(authenticatedUser.hasRole("ADMIN"));
    }

    @Test
    public void loginShouldFailClosedWhenDatabaseUserHasNoRole() {
        BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();
        genericDAO.insert(
                "INSERT INTO `user` (username, password, full_name, email, status) VALUES (?, ?, ?, ?, ?)",
                TEST_USERNAME, passwordHasher.hash("secret1"), "No Role User", TEST_EMAIL, 1);

        assertThrows(AuthenticationException.class, () -> authService.login(TEST_USERNAME, "secret1"));
    }

    private void cleanup() {
        genericDAO.delete("DELETE FROM `user` WHERE username = ?", TEST_USERNAME);
    }
}
