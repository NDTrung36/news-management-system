package com.example.news.filter;

import com.example.news.security.AuthSession;
import com.example.news.security.AuthenticatedUser;
import com.example.news.security.RoleCodes;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(
        filterName = "authorizationFilter",
        urlPatterns = "/admin/*",
        dispatcherTypes = DispatcherType.REQUEST
)
public class AuthorizationFilter implements Filter {

    private final AuthSession authSession;

    public AuthorizationFilter() {
        this(new AuthSession());
    }

    public AuthorizationFilter(AuthSession authSession) {
        this.authSession = authSession;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // No filter configuration is required.
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        AuthenticatedUser user = authSession.getAuthenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (!user.hasRole(RoleCodes.ADMIN)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No resources to release.
    }
}
