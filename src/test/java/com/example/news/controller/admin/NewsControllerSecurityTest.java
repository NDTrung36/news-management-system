package com.example.news.controller.admin;

import com.example.news.exception.ValidationException;
import com.example.news.model.CategoryListCriteria;
import com.example.news.model.CategoryModel;
import com.example.news.model.NewsDetail;
import com.example.news.model.NewsListCriteria;
import com.example.news.model.NewsListItem;
import com.example.news.model.NewsModel;
import com.example.news.model.PageResult;
import com.example.news.security.AuthSession;
import com.example.news.security.AuthenticatedUser;
import com.example.news.security.CsrfTokenManager;
import com.example.news.service.ICategoryService;
import com.example.news.service.INewsService;
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
import static org.junit.jupiter.api.Assertions.assertNull;

public class NewsControllerSecurityTest {

    @Test
    public void postShouldRejectInvalidCsrfBeforeCallingNewsService() throws Exception {
        StubNewsService newsService = new StubNewsService();
        NewsController controller = controller(newsService, new StubCsrfTokenManager(false), admin(42L));
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("action", "create");
        exchange.parameters.put("title", "Title");
        exchange.parameters.put("shortDescription", "Short");
        exchange.parameters.put("content", "Content");
        exchange.parameters.put("categoryId", "7");

        controller.doPost(exchange.request, exchange.response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, exchange.errorStatus);
        assertEquals(0, newsService.createCalls);
        assertEquals(0, newsService.updateCalls);
        assertEquals(0, newsService.deleteCalls);
    }

    @Test
    public void validCsrfCreateShouldUseAuthorFromAuthenticatedSessionOnly() throws Exception {
        StubNewsService newsService = new StubNewsService();
        NewsController controller = controller(newsService, new StubCsrfTokenManager(true), admin(42L));
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("action", "create");
        exchange.parameters.put("title", "Title");
        exchange.parameters.put("shortDescription", "Short");
        exchange.parameters.put("content", "Content");
        exchange.parameters.put("categoryId", "7");
        exchange.parameters.put("createdBy", "999");
        exchange.parameters.put("thumbnail", "forged.png");

        controller.doPost(exchange.request, exchange.response);

        assertEquals(1, newsService.createCalls);
        assertEquals(42L, newsService.lastAuthorId.longValue());
        assertNotNull(newsService.lastCreatedNews);
        assertNull(newsService.lastCreatedNews.getCreatedById());
        assertNull(newsService.lastCreatedNews.getThumbnail());
        assertEquals("/news/admin/news", exchange.redirectLocation);
        assertEquals("test-csrf-token", exchange.attributes.get("csrfToken"));
    }

    @Test
    public void postCreateShouldRejectMissingAuthenticatedUser() throws Exception {
        StubNewsService newsService = new StubNewsService();
        NewsController controller = controller(newsService, new StubCsrfTokenManager(true), null);
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("action", "create");
        exchange.parameters.put("title", "Title");
        exchange.parameters.put("shortDescription", "Short");
        exchange.parameters.put("content", "Content");
        exchange.parameters.put("categoryId", "7");

        controller.doPost(exchange.request, exchange.response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, exchange.errorStatus);
        assertEquals(0, newsService.createCalls);
    }

    @Test
    public void getListShouldExposeTokenCategoriesAndCriteria() throws Exception {
        StubNewsService newsService = new StubNewsService();
        StubCategoryService categoryService = new StubCategoryService();
        categoryService.categories = Collections.singletonList(new CategoryModel(7L, "Technology", "technology"));
        NewsController controller = new NewsController(newsService, categoryService,
                new StubAuthSession(admin(42L)), new StubCsrfTokenManager(true));
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("search", " java ");
        exchange.parameters.put("categoryId", "7");
        exchange.parameters.put("sortName", "title");
        exchange.parameters.put("sortBy", "asc");
        exchange.parameters.put("page", "2");

        controller.doGet(exchange.request, exchange.response);

        assertEquals("/WEB-INF/views/admin/news/list.jsp", exchange.dispatcherPath);
        assertEquals("test-csrf-token", exchange.attributes.get("csrfToken"));
        assertEquals(1, categoryService.findAllCalls);
        assertNotNull(newsService.lastSearchCriteria);
        assertEquals(" java ", newsService.lastSearchCriteria.getSearch());
        assertEquals(7L, newsService.lastSearchCriteria.getCategoryId().longValue());
        assertEquals("title", newsService.lastSearchCriteria.getSortName());
        assertEquals("asc", newsService.lastSearchCriteria.getSortBy());
        assertEquals(2, newsService.lastSearchCriteria.getPage());
    }

    @Test
    public void getViewShouldReturnNotFoundWhenNewsDoesNotExist() throws Exception {
        NewsController controller = controller(new StubNewsService(), new StubCsrfTokenManager(true), admin(42L));
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("action", "view");
        exchange.parameters.put("id", "999");

        controller.doGet(exchange.request, exchange.response);

        assertEquals(HttpServletResponse.SC_NOT_FOUND, exchange.errorStatus);
    }

    @Test
    public void updateValidationErrorShouldReturnFormWithSubmittedValuesAndCsrfToken() throws Exception {
        StubNewsService newsService = new StubNewsService();
        newsService.throwValidationOnUpdate = true;
        StubCategoryService categoryService = new StubCategoryService();
        categoryService.categories = Collections.singletonList(new CategoryModel(7L, "Technology", "technology"));
        NewsController controller = new NewsController(newsService, categoryService,
                new StubAuthSession(admin(42L)), new StubCsrfTokenManager(true));
        TestExchange exchange = new TestExchange();
        exchange.parameters.put("action", "update");
        exchange.parameters.put("id", "3");
        exchange.parameters.put("title", "Submitted title");
        exchange.parameters.put("shortDescription", "Submitted short");
        exchange.parameters.put("content", "Submitted content");
        exchange.parameters.put("categoryId", "7");

        controller.doPost(exchange.request, exchange.response);

        assertEquals(1, newsService.updateCalls);
        assertEquals("News validation failed", exchange.attributes.get("error"));
        assertEquals("/WEB-INF/views/admin/news/form.jsp", exchange.dispatcherPath);
        assertEquals("test-csrf-token", exchange.attributes.get("csrfToken"));
        NewsModel formNews = (NewsModel) exchange.attributes.get("news");
        assertEquals("Submitted title", formNews.getTitle());
        assertEquals(3L, formNews.getId().longValue());
    }

    private NewsController controller(StubNewsService newsService, StubCsrfTokenManager csrfTokenManager,
                                      AuthenticatedUser user) {
        return new NewsController(newsService, new StubCategoryService(), new StubAuthSession(user), csrfTokenManager);
    }

    private AuthenticatedUser admin(Long id) {
        return new AuthenticatedUser(id, "admin", "Administrator", Collections.singleton("ADMIN"));
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

    private static class StubAuthSession extends AuthSession {

        private final AuthenticatedUser user;

        private StubAuthSession(AuthenticatedUser user) {
            this.user = user;
        }

        @Override
        public AuthenticatedUser getAuthenticatedUser(HttpServletRequest request) {
            return user;
        }
    }

    private static class StubNewsService implements INewsService {

        private int createCalls;
        private int updateCalls;
        private int deleteCalls;
        private Long lastAuthorId;
        private NewsModel lastCreatedNews;
        private NewsListCriteria lastSearchCriteria;
        private boolean throwValidationOnUpdate;

        @Override
        public PageResult<NewsListItem> search(NewsListCriteria criteria) {
            lastSearchCriteria = criteria;
            return new PageResult<>(Collections.emptyList(), 1, 10, 0, 0);
        }

        @Override
        public NewsModel findById(Long id) {
            return null;
        }

        @Override
        public NewsDetail findDetailById(Long id) {
            return null;
        }

        @Override
        public long create(NewsModel news, Long authorId) {
            createCalls++;
            lastCreatedNews = news;
            lastAuthorId = authorId;
            return 1L;
        }

        @Override
        public boolean update(NewsModel news) {
            updateCalls++;
            if (throwValidationOnUpdate) {
                throw new ValidationException("News validation failed");
            }
            return true;
        }

        @Override
        public boolean delete(Long id) {
            deleteCalls++;
            return true;
        }
    }

    private static class StubCategoryService implements ICategoryService {

        private List<CategoryModel> categories = Collections.emptyList();
        private int findAllCalls;

        @Override
        public List<CategoryModel> findAll() {
            findAllCalls++;
            return categories;
        }

        @Override
        public PageResult<CategoryModel> search(CategoryListCriteria criteria) {
            return new PageResult<>(Collections.emptyList(), 1, 10, 0, 0);
        }

        @Override
        public CategoryModel findById(Long id) {
            return null;
        }

        @Override
        public long create(CategoryModel category) {
            return 0;
        }

        @Override
        public boolean update(CategoryModel category) {
            return false;
        }

        @Override
        public boolean delete(Long id) {
            return false;
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
