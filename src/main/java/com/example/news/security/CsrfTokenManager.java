package com.example.news.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class CsrfTokenManager {

    public static final String PARAMETER_NAME = "_csrf";
    private static final String SESSION_ATTRIBUTE = CsrfTokenManager.class.getName() + ".TOKEN";
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;

    public CsrfTokenManager() {
        this(new SecureRandom());
    }

    public CsrfTokenManager(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String ensureToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object currentToken = session.getAttribute(SESSION_ATTRIBUTE);
        if (currentToken instanceof String) {
            return (String) currentToken;
        }

        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }

    public boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_ATTRIBUTE);
        String supplied = request.getParameter(PARAMETER_NAME);
        if (!(expected instanceof String) || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(((String) expected).getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
