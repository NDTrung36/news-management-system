package com.example.news.controller.web;

import com.example.news.exception.ValidationException;
import com.example.news.model.RegisterForm;
import com.example.news.security.AuthSession;
import com.example.news.security.CsrfTokenManager;
import com.example.news.service.IAuthService;
import com.example.news.service.impl.AuthService;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterController extends HttpServlet {

    private final IAuthService authService;
    private final AuthSession authSession;
    private final CsrfTokenManager csrfTokenManager;

    public RegisterController() {
        this(new AuthService(), new AuthSession(), new CsrfTokenManager());
    }

    public RegisterController(IAuthService authService, AuthSession authSession, CsrfTokenManager csrfTokenManager) {
        this.authService = authService;
        this.authSession = authSession;
        this.csrfTokenManager = csrfTokenManager;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        if (authSession.getAuthenticatedUser(request) != null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        forwardToRegisterPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        if (!csrfTokenManager.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        RegisterForm form = new RegisterForm(
                request.getParameter("username"),
                request.getParameter("password"),
                request.getParameter("confirmPassword"),
                request.getParameter("fullName"),
                request.getParameter("email"));
        try {
            authService.register(form);
            response.sendRedirect(request.getContextPath() + "/login?registered=1");
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("username", form.getUsername());
            request.setAttribute("fullName", form.getFullName());
            request.setAttribute("email", form.getEmail());
            forwardToRegisterPage(request, response);
        }
    }

    private void forwardToRegisterPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("csrfToken", csrfTokenManager.ensureToken(request));
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/register.jsp");
        dispatcher.forward(request, response);
    }
}
