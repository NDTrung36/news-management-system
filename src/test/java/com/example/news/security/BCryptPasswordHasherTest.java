package com.example.news.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BCryptPasswordHasherTest {

    @Test
    public void hashShouldVerifyOriginalPasswordAndUseDistinctSalts() {
        PasswordHasher passwordHasher = new BCryptPasswordHasher();

        String firstHash = passwordHasher.hash(" mật khẩu có khoảng trắng ");
        String secondHash = passwordHasher.hash(" mật khẩu có khoảng trắng ");

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordHasher.verify(" mật khẩu có khoảng trắng ", firstHash));
        assertFalse(passwordHasher.verify("mật khẩu có khoảng trắng", firstHash));
    }

    @Test
    public void verifyShouldRejectMalformedHash() {
        PasswordHasher passwordHasher = new BCryptPasswordHasher();

        assertFalse(passwordHasher.verify("password", "not-a-bcrypt-hash"));
    }
}
