package com.example.news.controller.web;

import com.example.news.security.AuthSession;
import com.example.news.security.CsrfTokenManager;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/logout")
public class LogoutController extends HttpServlet {

    private final AuthSession authSession;
    private final CsrfTokenManager csrfTokenManager;

    public LogoutController() {
        this(new AuthSession(), new CsrfTokenManager());
    }

    public LogoutController(AuthSession authSession, CsrfTokenManager csrfTokenManager) {
        this.authSession = authSession;
        this.csrfTokenManager = csrfTokenManager;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (authSession.getAuthenticatedUser(request) != null && !csrfTokenManager.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        authSession.signOut(request);
        response.sendRedirect(request.getContextPath() + "/login?loggedOut=1");
    }
}
