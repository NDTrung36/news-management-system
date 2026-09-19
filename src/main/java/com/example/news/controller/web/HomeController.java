package com.example.news.controller.web;

import java.io.IOException;

import com.example.news.security.AuthSession;
import com.example.news.security.AuthenticatedUser;
import com.example.news.security.CsrfTokenManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/home")
public class HomeController extends HttpServlet {

    private final AuthSession authSession;
    private final CsrfTokenManager csrfTokenManager;

    public HomeController() {
        this(new AuthSession(), new CsrfTokenManager());
    }

    public HomeController(AuthSession authSession, CsrfTokenManager csrfTokenManager) {
        this.authSession = authSession;
        this.csrfTokenManager = csrfTokenManager;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        AuthenticatedUser user = authSession.getAuthenticatedUser(request);
        boolean authenticated = user != null;
        String username = authenticated ? user.getUsername() : "Guest";

        if (authenticated) {
            request.setAttribute("csrfToken", csrfTokenManager.ensureToken(request));
        }

        request.setAttribute("username", username);
        request.setAttribute("authenticated", authenticated);

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/home.jsp");
        dispatcher.forward(request, response);
    }
}
