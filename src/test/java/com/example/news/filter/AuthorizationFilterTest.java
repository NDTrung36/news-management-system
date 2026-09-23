package com.example.news.filter;

import com.example.news.security.AuthSession;
import com.example.news.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorizationFilterTest {

    @Test
    public void unauthenticatedRequestShouldRedirectToLogin() throws Exception {
        TestExchange exchange = new TestExchange();
        AtomicInteger chainCalls = new AtomicInteger();

        new AuthorizationFilter(new AuthSession()).doFilter(
                exchange.request, exchange.response, countingChain(chainCalls));

        assertEquals("/news/login", exchange.redirectLocation);
        assertEquals(0, chainCalls.get());
    }

    @Test
    public void userWithoutAdminRoleShouldReceiveForbidden() throws Exception {
        TestExchange exchange = new TestExchange();
        exchange.sessionAttributes.put(AuthSession.AUTH_USER_ATTRIBUTE,
                new AuthenticatedUser(1L, "user", "User", Collections.singleton("USER")));
        AtomicInteger chainCalls = new AtomicInteger();

        new AuthorizationFilter(new AuthSession()).doFilter(
                exchange.request, exchange.response, countingChain(chainCalls));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, exchange.errorStatus);
        assertEquals(0, chainCalls.get());
    }

    @Test
    public void adminRequestShouldContinueTheFilterChain() throws Exception {
        TestExchange exchange = new TestExchange();
        exchange.sessionAttributes.put(AuthSession.AUTH_USER_ATTRIBUTE,
                new AuthenticatedUser(1L, "admin", "Admin", Collections.singleton("ADMIN")));
        AtomicInteger chainCalls = new AtomicInteger();

        new AuthorizationFilter(new AuthSession()).doFilter(
                exchange.request, exchange.response, countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertEquals(0, exchange.errorStatus);
    }

    private static FilterChain countingChain(AtomicInteger calls) {
        return (request, response) -> calls.incrementAndGet();
    }

    private static class TestExchange {

        private final Map<String, Object> sessionAttributes = new HashMap<>();
        private final HttpSession session;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private String redirectLocation;
        private int errorStatus;

        private TestExchange() {
            session = proxy(HttpSession.class, (proxy, method, args) -> {
                if ("getAttribute".equals(method.getName())) {
                    return sessionAttributes.get(args[0]);
                }
                return defaultValue(method.getReturnType());
            });
            request = proxy(HttpServletRequest.class, (proxy, method, args) -> {
                if ("getSession".equals(method.getName())) {
                    return sessionAttributes.isEmpty() ? null : session;
                }
                if ("getContextPath".equals(method.getName())) {
                    return "/news";
                }
                return defaultValue(method.getReturnType());
            });
            response = proxy(HttpServletResponse.class, (proxy, method, args) -> {
                if ("sendRedirect".equals(method.getName())) {
                    redirectLocation = (String) args[0];
                    return null;
                }
                if ("sendError".equals(method.getName())) {
                    errorStatus = (Integer) args[0];
                    return null;
                }
                return defaultValue(method.getReturnType());
            });
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
