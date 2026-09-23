package com.example.news.service;

import com.example.news.dao.ICategoryDAO;
import com.example.news.dao.INewsDAO;
import com.example.news.dao.IUserDAO;
import com.example.news.exception.DatabaseException;
import com.example.news.exception.ValidationException;
import com.example.news.model.CategoryListCriteria;
import com.example.news.model.CategoryModel;
import com.example.news.model.NewsDetail;
import com.example.news.model.NewsListCriteria;
import com.example.news.model.NewsListItem;
import com.example.news.model.NewsModel;
import com.example.news.model.PageResult;
import com.example.news.model.UserModel;
import com.example.news.service.impl.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NewsServiceTest {

    private FakeNewsDAO newsDAO;
    private FakeCategoryDAO categoryDAO;
    private FakeUserDAO userDAO;
    private INewsService newsService;

    @BeforeEach
    public void setUp() {
        newsDAO = new FakeNewsDAO();
        categoryDAO = new FakeCategoryDAO();
        userDAO = new FakeUserDAO();
        categoryDAO.categories.put(10L, new CategoryModel(10L, "Technology", "technology"));
        userDAO.users.put(20L, user(20L, "author"));
        newsService = new NewsService(newsDAO, categoryDAO, userDAO);
    }

    @Test
    public void createShouldTrimFieldsAndUseAuthenticatedAuthor() {
        NewsModel news = new NewsModel("  Java  ", "  Short description  ", "  Content  ", 10L);

        long id = newsService.create(news, 20L);

        assertEquals(1L, id);
        assertEquals("Java", news.getTitle());
        assertEquals("Short description", news.getShortDescription());
        assertEquals("Content", news.getContent());
        assertEquals(20L, news.getCreatedById().longValue());
        assertSame(news, newsDAO.lastInserted);
        assertNull(news.getThumbnail());
    }

    @Test
    public void createShouldRejectMissingTextCategoryOrAuthor() {
        assertThrows(ValidationException.class,
                () -> newsService.create(new NewsModel("", "Short", "Content", 10L), 20L));
        assertThrows(ValidationException.class,
                () -> newsService.create(new NewsModel("Title", "", "Content", 10L), 20L));
        assertThrows(ValidationException.class,
                () -> newsService.create(new NewsModel("Title", "Short", "", 10L), 20L));
        assertThrows(ValidationException.class,
                () -> newsService.create(new NewsModel("Title", "Short", "Content", 999L), 20L));
        assertThrows(ValidationException.class,
                () -> newsService.create(new NewsModel("Title", "Short", "Content", 10L), 999L));
    }

    @Test
    public void createShouldEnforceDatabaseColumnLengths() {
        String titleTooLong = repeat('t', 256);
        String descriptionTooLong = repeat('d', 501);

        assertThrows(ValidationException.class,
                () -> newsService.create(new NewsModel(titleTooLong, "Short", "Content", 10L), 20L));
        assertThrows(ValidationException.class,
                () -> newsService.create(new NewsModel("Title", descriptionTooLong, "Content", 10L), 20L));
    }

    @Test
    public void updateShouldPreserveAuthorThumbnailAndCreationMetadata() {
        NewsModel existing = new NewsModel(1L, "Old", "Old short", "Old content", 10L);
        existing.setCreatedById(20L);
        existing.setThumbnail("uploads/existing.png");
        LocalDateTime createdDate = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime modifiedDate = LocalDateTime.of(2026, 1, 2, 9, 0);
        existing.setCreatedDate(createdDate);
        existing.setModifiedDate(modifiedDate);
        newsDAO.storage.put(1L, existing);

        NewsModel update = new NewsModel(1L, "  New title  ", "  New short  ", "  New content  ", 10L);
        update.setCreatedById(999L);
        update.setThumbnail("uploads/forged.png");

        assertTrue(newsService.update(update));
        assertEquals("New title", update.getTitle());
        assertEquals("New short", update.getShortDescription());
        assertEquals("New content", update.getContent());
        assertEquals(20L, update.getCreatedById().longValue());
        assertEquals("uploads/existing.png", update.getThumbnail());
        assertEquals(createdDate, update.getCreatedDate());
        assertEquals(modifiedDate, update.getModifiedDate());
        assertSame(update, newsDAO.lastUpdated);
    }

    @Test
    public void updateShouldReturnFalseForMissingNewsAndRejectInvalidId() {
        assertFalse(newsService.update(new NewsModel(999L, "Title", "Short", "Content", 10L)));
        assertThrows(ValidationException.class,
                () -> newsService.update(new NewsModel(null, "Title", "Short", "Content", 10L)));
    }

    @Test
    public void searchShouldNormalizeCriteriaAndClampPage() {
        NewsListItem item = new NewsListItem();
        item.setId(21L);
        newsDAO.pageItems = Collections.singletonList(item);
        newsDAO.totalItems = 21L;
        NewsListCriteria criteria = new NewsListCriteria("  matching text  ", 10L,
                "not-a-column", "ASC", 999);

        PageResult<NewsListItem> result = newsService.search(criteria);

        assertEquals("matching text", criteria.getSearch());
        assertEquals("createdDate", criteria.getSortName());
        assertEquals("asc", criteria.getSortBy());
        assertEquals(3, criteria.getPage());
        assertEquals(3, result.getPage());
        assertEquals(3, result.getTotalPages());
        assertEquals(20L, newsDAO.lastOffset);
        assertEquals(10, newsDAO.lastLimit);
        assertEquals(10L, newsDAO.lastCriteria.getCategoryId().longValue());
        assertEquals(1, result.getItems().size());
    }

    @Test
    public void searchShouldRejectOversizedSearchAndInvalidCategoryFilter() {
        assertThrows(ValidationException.class,
                () -> newsService.search(new NewsListCriteria(repeat('s', 101), null, "title", "asc", 1)));
        assertThrows(ValidationException.class,
                () -> newsService.search(new NewsListCriteria("", 0L, "title", "asc", 1)));
    }

    @Test
    public void deleteShouldReturnFalseForMissingNewsAndRejectInvalidId() {
        assertFalse(newsService.delete(999L));
        assertThrows(ValidationException.class, () -> newsService.delete(null));
    }

    @Test
    public void createShouldMapConcurrentForeignKeyFailureToValidationError() {
        newsDAO.insertFailure = new DatabaseException("foreign key",
                new SQLException("foreign key", "23000", 1452));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> newsService.create(new NewsModel("Title", "Short", "Content", 10L), 20L));

        assertTrue(exception.getMessage().contains("no longer available"));
    }

    private UserModel user(Long id, String username) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setUsername(username);
        user.setFullName(username);
        user.setEmail(username + "@example.com");
        user.setStatus(1);
        return user;
    }

    private String repeat(char value, int length) {
        StringBuilder result = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            result.append(value);
        }
        return result.toString();
    }

    private static class FakeNewsDAO implements INewsDAO {

        private final Map<Long, NewsModel> storage = new HashMap<>();
        private long nextId = 1L;
        private long totalItems;
        private List<NewsListItem> pageItems = Collections.emptyList();
        private NewsModel lastInserted;
        private NewsModel lastUpdated;
        private NewsListCriteria lastCriteria;
        private long lastOffset;
        private int lastLimit;
        private DatabaseException insertFailure;

        @Override
        public List<NewsListItem> findPage(NewsListCriteria criteria, long offset, int limit) {
            lastCriteria = criteria;
            lastOffset = offset;
            lastLimit = limit;
            return pageItems;
        }

        @Override
        public long countByCriteria(NewsListCriteria criteria) {
            lastCriteria = criteria;
            return totalItems;
        }

        @Override
        public NewsModel findById(Long id) {
            return storage.get(id);
        }

        @Override
        public NewsDetail findDetailById(Long id) {
            return null;
        }

        @Override
        public long insert(NewsModel news) {
            if (insertFailure != null) {
                throw insertFailure;
            }
            long id = nextId++;
            news.setId(id);
            storage.put(id, news);
            lastInserted = news;
            return id;
        }

        @Override
        public int update(NewsModel news) {
            if (!storage.containsKey(news.getId())) {
                return 0;
            }
            storage.put(news.getId(), news);
            lastUpdated = news;
            return 1;
        }

        @Override
        public int delete(Long id) {
            return storage.remove(id) == null ? 0 : 1;
        }
    }

    private static class FakeCategoryDAO implements ICategoryDAO {

        private final Map<Long, CategoryModel> categories = new HashMap<>();

        @Override
        public List<CategoryModel> findAll() {
            return new ArrayList<>(categories.values());
        }

        @Override
        public List<CategoryModel> findPage(CategoryListCriteria criteria, long offset, int limit) {
            return Collections.emptyList();
        }

        @Override
        public long countBySearch(String search) {
            return 0;
        }

        @Override
        public CategoryModel findById(Long id) {
            return categories.get(id);
        }

        @Override
        public CategoryModel findByCode(String code) {
            return null;
        }

        @Override
        public long insert(CategoryModel category) {
            return 0;
        }

        @Override
        public int update(CategoryModel category) {
            return 0;
        }

        @Override
        public int delete(Long id) {
            return 0;
        }

        @Override
        public long countNewsByCategoryId(Long categoryId) {
            return 0;
        }
    }

    private static class FakeUserDAO implements IUserDAO {

        private final Map<Long, UserModel> users = new HashMap<>();

        @Override
        public UserModel findById(Long id) {
            return users.get(id);
        }

        @Override
        public UserModel findByUsername(String username) {
            return null;
        }

        @Override
        public UserModel findByEmail(String email) {
            return null;
        }

        @Override
        public long insertWithDefaultRole(UserModel user) {
            return 0;
        }
    }
}
