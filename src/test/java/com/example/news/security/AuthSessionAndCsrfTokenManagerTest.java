package com.example.news.security;

import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthSessionAndCsrfTokenManagerTest {

    @Test
    public void signInShouldRotateSessionAndSignOutShouldInvalidateIt() {
        TestRequest request = new TestRequest();
        AuthSession authSession = new AuthSession();
        TestSession oldSession = request.getOrCreateSession();
        oldSession.attributes.put("anonymous", "value");

        AuthenticatedUser user = new AuthenticatedUser(1L, "demo", "Demo User", Collections.singleton("USER"));
        authSession.signIn(request.proxy, user);

        TestSession newSession = request.getOrCreateSession();
        assertTrue(oldSession.invalidated);
        assertFalse(newSession == oldSession);
        assertNotNull(authSession.getAuthenticatedUser(request.proxy));
        assertNull(newSession.attributes.get("anonymous"));

        authSession.signOut(request.proxy);
        assertTrue(newSession.invalidated);
        assertNull(authSession.getAuthenticatedUser(request.proxy));
    }

    @Test
    public void csrfTokenShouldBeBoundToTheCurrentSession() {
        TestRequest request = new TestRequest();
        CsrfTokenManager csrfTokenManager = new CsrfTokenManager();

        String token = csrfTokenManager.ensureToken(request.proxy);
        request.parameters.put(CsrfTokenManager.PARAMETER_NAME, token);
        assertTrue(csrfTokenManager.isValid(request.proxy));

        request.parameters.put(CsrfTokenManager.PARAMETER_NAME, "incorrect-token");
        assertFalse(csrfTokenManager.isValid(request.proxy));

        request.getOrCreateSession().invalidated = true;
        assertFalse(csrfTokenManager.isValid(request.proxy));
    }

    private static class TestRequest implements InvocationHandler {

        private final Map<String, String> parameters = new HashMap<>();
        private final HttpServletRequest proxy;
        private TestSession currentSession;

        private TestRequest() {
            proxy = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{HttpServletRequest.class}, this);
        }

        private TestSession getOrCreateSession() {
            if (currentSession == null || currentSession.invalidated) {
                currentSession = new TestSession();
            }
            return currentSession;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getSession".equals(name)) {
                boolean create = args == null || args.length == 0 || Boolean.TRUE.equals(args[0]);
                if (currentSession == null || currentSession.invalidated) {
                    return create ? getOrCreateSession().proxy : null;
                }
                return currentSession.proxy;
            }
            if ("getParameter".equals(name)) {
                return parameters.get(args[0]);
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static class TestSession implements InvocationHandler {

        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpSession proxy;
        private boolean invalidated;

        private TestSession() {
            proxy = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{HttpSession.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getAttribute".equals(name)) {
                return attributes.get(args[0]);
            }
            if ("setAttribute".equals(name)) {
                attributes.put((String) args[0], args[1]);
                return null;
            }
            if ("removeAttribute".equals(name)) {
                attributes.remove(args[0]);
                return null;
            }
            if ("invalidate".equals(name)) {
                invalidated = true;
                attributes.clear();
                return null;
            }
            return defaultValue(method.getReturnType());
        }
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
        if (type == byte.class || type == short.class || type == int.class || type == long.class
                || type == float.class || type == double.class) {
            return 0;
        }
        return null;
    }
}
