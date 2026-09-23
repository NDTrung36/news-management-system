package com.example.news.controller.web;

import org.junit.jupiter.api.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForbiddenControllerTest {

    @Test
    public void getShouldKeepForbiddenStatusAndForwardToForbiddenView() throws Exception {
        TestExchange exchange = new TestExchange();

        new ForbiddenController().doGet(exchange.request, exchange.response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, exchange.status);
        assertEquals("text/html;charset=UTF-8", exchange.contentType);
        assertEquals("/WEB-INF/views/403.jsp", exchange.dispatcherPath);
        assertTrue(exchange.forwarded);
    }

    private static class TestExchange {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private String dispatcherPath;
        private boolean forwarded;
        private int status;
        private String contentType;

        private TestExchange() {
            RequestDispatcher dispatcher = proxy(RequestDispatcher.class, (proxy, method, args) -> {
                if ("forward".equals(method.getName())) {
                    forwarded = true;
                }
                return null;
            });
            request = proxy(HttpServletRequest.class, (proxy, method, args) -> {
                if ("getRequestDispatcher".equals(method.getName())) {
                    dispatcherPath = (String) args[0];
                    return dispatcher;
                }
                return defaultValue(method.getReturnType());
            });
            response = proxy(HttpServletResponse.class, (proxy, method, args) -> {
                if ("setStatus".equals(method.getName())) {
                    status = (Integer) args[0];
                    return null;
                }
                if ("setContentType".equals(method.getName())) {
                    contentType = (String) args[0];
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
