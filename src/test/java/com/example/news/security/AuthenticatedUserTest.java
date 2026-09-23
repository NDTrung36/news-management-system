package com.example.news.security;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthenticatedUserTest {

    @Test
    public void rolesShouldBeNormalizedAndDefensivelyCopied() {
        Set<String> sourceRoles = new LinkedHashSet<>();
        sourceRoles.add(" user ");
        sourceRoles.add("admin");

        AuthenticatedUser user = new AuthenticatedUser(1L, "demo", "Demo User", sourceRoles);
        sourceRoles.clear();

        assertTrue(user.hasRole("USER"));
        assertTrue(user.hasRole(" admin "));
        assertFalse(user.hasRole(null));
        assertFalse(user.hasRole("EDITOR"));
        assertThrows(UnsupportedOperationException.class, () -> user.getRoleCodes().add("EDITOR"));
    }
}
