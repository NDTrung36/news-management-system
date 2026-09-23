package com.example.news.service;

import com.example.news.dao.IUserDAO;
import com.example.news.exception.AuthenticationException;
import com.example.news.exception.DuplicateUserException;
import com.example.news.exception.ValidationException;
import com.example.news.model.RegisterForm;
import com.example.news.model.UserModel;
import com.example.news.security.AuthenticatedUser;
import com.example.news.security.PasswordHasher;
import com.example.news.service.impl.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthServiceTest {

    private FakeUserDAO userDAO;
    private IAuthService authService;
    private Set<String> loginRoleCodes;

    @BeforeEach
    public void setUp() {
        userDAO = new FakeUserDAO();
        loginRoleCodes = new LinkedHashSet<>();
        loginRoleCodes.add("USER");
        authService = new AuthService(userDAO, userId -> loginRoleCodes, new FakePasswordHasher());
    }

    @Test
    public void registerShouldNormalizeFieldsHashPasswordAndAssignDefaultRole() {
        RegisterForm form = new RegisterForm("  new-user  ", "secret1", "secret1",
                "  New User  ", "  new@example.com  ");

        long userId = authService.register(form);

        UserModel user = userDAO.findByUsername("new-user");
        assertEquals(1L, userId);
        assertNotNull(user);
        assertEquals("new-user", user.getUsername());
        assertEquals("New User", user.getFullName());
        assertEquals("new@example.com", user.getEmail());
        assertEquals(1, user.getStatus());
        assertNotEquals("secret1", user.getPasswordHash());
        assertTrue(userDAO.defaultRoleAssigned);
    }

    @Test
    public void registerShouldRejectInvalidInput() {
        assertThrows(ValidationException.class,
                () -> authService.register(new RegisterForm("", "secret1", "secret1", "Name", "name@example.com")));
        assertThrows(ValidationException.class,
                () -> authService.register(new RegisterForm("user", "short", "short", "Name", "name@example.com")));
        assertThrows(ValidationException.class,
                () -> authService.register(new RegisterForm("user", "secret1", "different", "Name", "name@example.com")));
        assertThrows(ValidationException.class,
                () -> authService.register(new RegisterForm("user", "secret1", "secret1", "Name", "invalid-email")));

        String tooLongPassword = new String(new char[73]).replace('\0', 'p');
        assertThrows(ValidationException.class,
                () -> authService.register(new RegisterForm("user", tooLongPassword, tooLongPassword, "Name", "name@example.com")));
    }

    @Test
    public void registerShouldRejectDuplicateUsernameAndEmail() {
        authService.register(new RegisterForm("user-one", "secret1", "secret1", "User One", "one@example.com"));

        assertThrows(ValidationException.class,
                () -> authService.register(new RegisterForm("user-one", "secret1", "secret1", "Another", "another@example.com")));
        assertThrows(ValidationException.class,
                () -> authService.register(new RegisterForm("user-two", "secret1", "secret1", "Another", "one@example.com")));
    }

    @Test
    public void registerShouldMapConcurrentDuplicateToValidationError() {
        userDAO.throwDuplicateOnInsert = true;

        assertThrows(ValidationException.class,
                () -> authService.register(new RegisterForm("user", "secret1", "secret1", "Name", "name@example.com")));
    }

    @Test
    public void loginShouldReturnMinimalAuthenticatedUserForActiveCredentials() {
        authService.register(new RegisterForm("user-one", "secret1", "secret1", "User One", "one@example.com"));

        AuthenticatedUser user = authService.login(" user-one ", "secret1");

        assertEquals("user-one", user.getUsername());
        assertEquals("User One", user.getFullName());
        assertNotNull(user.getId());
        assertTrue(user.hasRole("USER"));
    }

    @Test
    public void loginShouldIncludeAllAssignedRoles() {
        authService.register(new RegisterForm("admin", "secret1", "secret1", "Admin", "admin@example.com"));
        loginRoleCodes.add("ADMIN");

        AuthenticatedUser user = authService.login("admin", "secret1");

        assertTrue(user.hasRole("USER"));
        assertTrue(user.hasRole("ADMIN"));
    }

    @Test
    public void loginShouldRejectAccountWithoutAnyRole() {
        authService.register(new RegisterForm("user-one", "secret1", "secret1", "User One", "one@example.com"));
        loginRoleCodes.clear();

        assertThrows(AuthenticationException.class, () -> authService.login("user-one", "secret1"));
    }

    @Test
    public void loginShouldUseSameMessageForUnknownWrongAndLockedAccounts() {
        authService.register(new RegisterForm("user-one", "secret1", "secret1", "User One", "one@example.com"));
        String unknownMessage = assertThrows(AuthenticationException.class,
                () -> authService.login("unknown", "secret1")).getMessage();
        String wrongPasswordMessage = assertThrows(AuthenticationException.class,
                () -> authService.login("user-one", "wrong-password")).getMessage();

        userDAO.findByUsername("user-one").setStatus(0);
        String lockedMessage = assertThrows(AuthenticationException.class,
                () -> authService.login("user-one", "secret1")).getMessage();

        assertEquals(unknownMessage, wrongPasswordMessage);
        assertEquals(unknownMessage, lockedMessage);
    }

    private static class FakePasswordHasher implements PasswordHasher {

        @Override
        public String hash(String password) {
            return "hash:" + password;
        }

        @Override
        public boolean verify(String password, String passwordHash) {
            return ("hash:" + password).equals(passwordHash);
        }
    }

    private static class FakeUserDAO implements IUserDAO {

        private final Map<String, UserModel> usersByUsername = new HashMap<>();
        private long nextId = 1L;
        private boolean defaultRoleAssigned;
        private boolean throwDuplicateOnInsert;

        @Override
        public UserModel findByUsername(String username) {
            return usersByUsername.get(username);
        }

        @Override
        public UserModel findByEmail(String email) {
            for (UserModel user : usersByUsername.values()) {
                if (user.getEmail().equals(email)) {
                    return user;
                }
            }
            return null;
        }

        @Override
        public long insertWithDefaultRole(UserModel user) {
            if (throwDuplicateOnInsert) {
                throw new DuplicateUserException("duplicate", null);
            }
            user.setId(nextId++);
            usersByUsername.put(user.getUsername(), user);
            defaultRoleAssigned = true;
            return user.getId();
        }
    }
}
