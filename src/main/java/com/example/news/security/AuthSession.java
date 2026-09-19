package com.example.news.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class AuthSession {

    public static final String AUTH_USER_ATTRIBUTE = "AUTH_USER";

    public AuthenticatedUser getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(AUTH_USER_ATTRIBUTE);
        return user instanceof AuthenticatedUser ? (AuthenticatedUser) user : null;
    }

    public void signIn(HttpServletRequest request, AuthenticatedUser user) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute(AUTH_USER_ATTRIBUTE, user);
    }

    public void signOut(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
