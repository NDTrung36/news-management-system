package com.example.news.controller.admin;

import com.example.news.model.CategoryModel;
import com.example.news.model.CategoryListCriteria;
import com.example.news.model.PageResult;
import com.example.news.exception.ValidationException;
import com.example.news.security.CsrfTokenManager;
import com.example.news.service.ICategoryService;
import org.junit.jupiter.api.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CategoryControllerSecurityTest {

    @Test
    public void postShouldRejectInvalidCsrfBeforeCallingCategoryService() throws Exception {
        StubCategoryService categoryService = new StubCategoryService();
        StubCsrfTokenManager csrfTokenManager = new StubCsrfTokenManager(false);
        CategoryController controller = new CategoryController(categoryService, csrfTokenManager);
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("action", "create");
        exchange.parameters.put("name", "Java");
        exchange.parameters.put("code", "java");

        controller.doPost(exchange.request, exchange.response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, exchange.errorStatus);
        assertEquals(0, categoryService.createCalls);
        assertEquals(0, categoryService.updateCalls);
        assertEquals(0, categoryService.deleteCalls);
    }

    @Test
    public void validCsrfShouldAllowCreateAndRedirectToCategoryList() throws Exception {
        StubCategoryService categoryService = new StubCategoryService();
        StubCsrfTokenManager csrfTokenManager = new StubCsrfTokenManager(true);
        CategoryController controller = new CategoryController(categoryService, csrfTokenManager);
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("action", "create");
        exchange.parameters.put("name", "Java");
        exchange.parameters.put("code", "java");

        controller.doPost(exchange.request, exchange.response);

        assertEquals(1, categoryService.createCalls);
        assertEquals("/news/admin/category", exchange.redirectLocation);
        assertEquals("test-csrf-token", exchange.attributes.get("csrfToken"));
    }

    @Test
    public void getShouldExposeCsrfTokenToCategoryView() throws Exception {
        StubCategoryService categoryService = new StubCategoryService();
        StubCsrfTokenManager csrfTokenManager = new StubCsrfTokenManager(true);
        CategoryController controller = new CategoryController(categoryService, csrfTokenManager);
        TestExchange exchange = new TestExchange();

        controller.doGet(exchange.request, exchange.response);

        assertEquals("test-csrf-token", exchange.attributes.get("csrfToken"));
        assertEquals("/WEB-INF/views/admin/category/list.jsp", exchange.dispatcherPath);
        assertEquals(0, categoryService.findAllCalls);
        assertEquals(0L, ((PageResult<?>) exchange.attributes.get("pageResult")).getTotalItems());
    }

    @Test
    public void getShouldPassSearchSortAndPageCriteriaToService() throws Exception {
        StubCategoryService categoryService = new StubCategoryService();
        StubCsrfTokenManager csrfTokenManager = new StubCsrfTokenManager(true);
        CategoryController controller = new CategoryController(categoryService, csrfTokenManager);
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("search", " java ");
        exchange.parameters.put("sortName", "code");
        exchange.parameters.put("sortBy", "desc");
        exchange.parameters.put("page", "2");

        controller.doGet(exchange.request, exchange.response);

        assertNotNull(categoryService.lastSearchCriteria);
        assertEquals(" java ", categoryService.lastSearchCriteria.getSearch());
        assertEquals("code", categoryService.lastSearchCriteria.getSortName());
        assertEquals("desc", categoryService.lastSearchCriteria.getSortBy());
        assertEquals(2, categoryService.lastSearchCriteria.getPage());
    }

    @Test
    public void deleteValidationErrorShouldRenderListWithPageResult() throws Exception {
        StubCategoryService categoryService = new StubCategoryService();
        categoryService.throwValidationOnDelete = true;
        StubCsrfTokenManager csrfTokenManager = new StubCsrfTokenManager(true);
        CategoryController controller = new CategoryController(categoryService, csrfTokenManager);
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("action", "delete");
        exchange.parameters.put("id", "1");

        controller.doPost(exchange.request, exchange.response);

        assertEquals(1, categoryService.deleteCalls);
        assertEquals("Category is being used", exchange.attributes.get("error"));
        assertEquals("/WEB-INF/views/admin/category/list.jsp", exchange.dispatcherPath);
        assertEquals(0L, ((PageResult<?>) exchange.attributes.get("pageResult")).getTotalItems());
    }

    private static class StubCsrfTokenManager extends CsrfTokenManager {

        private final boolean valid;

        private StubCsrfTokenManager(boolean valid) {
            this.valid = valid;
        }

        @Override
        public String ensureToken(HttpServletRequest request) {
            return "test-csrf-token";
        }

        @Override
        public boolean isValid(HttpServletRequest request) {
            return valid;
        }
    }

    private static class StubCategoryService implements ICategoryService {

        private int findAllCalls;
        private int createCalls;
        private int updateCalls;
        private int deleteCalls;
        private boolean throwValidationOnDelete;
        private CategoryListCriteria lastSearchCriteria;

        @Override
        public List<CategoryModel> findAll() {
            findAllCalls++;
            return Collections.emptyList();
        }

        @Override
        public CategoryModel findById(Long id) {
            return null;
        }

        @Override
        public PageResult<CategoryModel> search(CategoryListCriteria criteria) {
            lastSearchCriteria = criteria;
            return new PageResult<>(Collections.emptyList(), 1, 10, 0, 0);
        }

        @Override
        public long create(CategoryModel category) {
            createCalls++;
            return 1L;
        }

        @Override
        public boolean update(CategoryModel category) {
            updateCalls++;
            return true;
        }

        @Override
        public boolean delete(Long id) {
            deleteCalls++;
            if (throwValidationOnDelete) {
                throw new ValidationException("Category is being used");
            }
            return true;
        }
    }

    private static class TestExchange {

        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private String redirectLocation;
        private String dispatcherPath;
        private int errorStatus;

        private TestExchange() {
            RequestDispatcher dispatcher = proxy(RequestDispatcher.class, (proxy, method, args) -> null);
            request = proxy(HttpServletRequest.class, (proxy, method, args) -> {
                String methodName = method.getName();
                if ("getParameter".equals(methodName)) {
                    return parameters.get(args[0]);
                }
                if ("setAttribute".equals(methodName)) {
                    attributes.put((String) args[0], args[1]);
                    return null;
                }
                if ("getAttribute".equals(methodName)) {
                    return attributes.get(args[0]);
                }
                if ("getContextPath".equals(methodName)) {
                    return "/news";
                }
                if ("getRequestDispatcher".equals(methodName)) {
                    dispatcherPath = (String) args[0];
                    return dispatcher;
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
