package com.example.news.service;

import com.example.news.dao.ICategoryDAO;
import com.example.news.exception.DatabaseException;
import com.example.news.exception.ValidationException;
import com.example.news.model.CategoryModel;
import com.example.news.model.CategoryListCriteria;
import com.example.news.model.PageResult;
import com.example.news.service.impl.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CategoryServiceTest {

    private FakeCategoryDAO fakeCategoryDAO;
    private ICategoryService categoryService;

    @BeforeEach
    public void setUp() {
        fakeCategoryDAO = new FakeCategoryDAO();
        categoryService = new CategoryService(fakeCategoryDAO);
    }

    @Test
    @DisplayName("create should throw ValidationException when Category is null")
    public void testCreateWithNullCategory() {
        ValidationException ex = assertThrows(ValidationException.class, () -> categoryService.create(null));
        assertTrue(ex.getMessage().contains("null"));
    }

    @Test
    @DisplayName("create should throw ValidationException when name is blank")
    public void testCreateWithBlankName() {
        assertThrows(ValidationException.class, () -> categoryService.create(new CategoryModel(null, "code1")));
        assertThrows(ValidationException.class, () -> categoryService.create(new CategoryModel("", "code1")));
        assertThrows(ValidationException.class, () -> categoryService.create(new CategoryModel("   ", "code1")));
    }

    @Test
    @DisplayName("create should throw ValidationException when code is blank")
    public void testCreateWithBlankCode() {
        assertThrows(ValidationException.class, () -> categoryService.create(new CategoryModel("Name 1", null)));
        assertThrows(ValidationException.class, () -> categoryService.create(new CategoryModel("Name 1", "")));
        assertThrows(ValidationException.class, () -> categoryService.create(new CategoryModel("Name 1", "   ")));
    }

    @Test
    @DisplayName("create should throw ValidationException when name exceeds 100 characters")
    public void testCreateWithNameExceeding100Chars() {
        String longName = new String(new char[101]).replace('\0', 'a');
        ValidationException ex = assertThrows(ValidationException.class,
                () -> categoryService.create(new CategoryModel(longName, "valid-code")));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    @DisplayName("create should throw ValidationException when code exceeds 100 characters")
    public void testCreateWithCodeExceeding100Chars() {
        String longCode = new String(new char[101]).replace('\0', 'c');
        ValidationException ex = assertThrows(ValidationException.class,
                () -> categoryService.create(new CategoryModel("Valid Name", longCode)));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    @DisplayName("create should throw ValidationException when category code already exists")
    public void testCreateDuplicateCode() {
        CategoryModel existing = new CategoryModel("Java", "java");
        categoryService.create(existing);

        CategoryModel duplicate = new CategoryModel("Java News", "java");
        ValidationException ex = assertThrows(ValidationException.class,
                () -> categoryService.create(duplicate));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("create should trim fields and return generated ID on success")
    public void testCreateSuccess() {
        CategoryModel category = new CategoryModel("  Technology  ", "  tech  ");
        long id = categoryService.create(category);

        assertTrue(id > 0);
        CategoryModel saved = categoryService.findById(id);
        assertNotNull(saved);
        assertEquals("Technology", saved.getName());
        assertEquals("tech", saved.getCode());
    }

    @Test
    @DisplayName("update should return false when category is missing")
    public void testUpdateMissingCategory() {
        CategoryModel model = new CategoryModel(999L, "Missing", "missing");
        boolean result = categoryService.update(model);
        assertFalse(result);
    }

    @Test
    @DisplayName("update should throw ValidationException when code belongs to another category")
    public void testUpdateDuplicateCodeOnAnotherCategory() {
        long id1 = categoryService.create(new CategoryModel("Cat 1", "code-1"));
        categoryService.create(new CategoryModel("Cat 2", "code-2"));

        CategoryModel updateToCode2 = new CategoryModel(id1, "Cat 1 Updated", "code-2");
        ValidationException ex = assertThrows(ValidationException.class,
                () -> categoryService.update(updateToCode2));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("update should succeed when keeping its own code")
    public void testUpdateSuccessWithSameCode() {
        long id = categoryService.create(new CategoryModel("Initial Name", "code-keep"));
        CategoryModel updateModel = new CategoryModel(id, "Updated Name", "code-keep");

        boolean updated = categoryService.update(updateModel);
        assertTrue(updated);

        CategoryModel retrieved = categoryService.findById(id);
        assertEquals("Updated Name", retrieved.getName());
        assertEquals("code-keep", retrieved.getCode());
    }

    @Test
    @DisplayName("delete should return false when category does not exist")
    public void testDeleteMissing() {
        boolean deleted = categoryService.delete(999L);
        assertFalse(deleted);
    }

    @Test
    @DisplayName("delete should throw ValidationException when category is referenced by news")
    public void testDeleteReferencedCategory() {
        long id = categoryService.create(new CategoryModel("Sports", "sports"));
        fakeCategoryDAO.setNewsCount(id, 3L);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> categoryService.delete(id));
        assertTrue(ex.getMessage().contains("news"));
    }

    @Test
    @DisplayName("delete should succeed when category is not referenced")
    public void testDeleteSuccess() {
        long id = categoryService.create(new CategoryModel("Design", "design"));
        boolean deleted = categoryService.delete(id);

        assertTrue(deleted);
        assertNotNull(fakeCategoryDAO);
        assertEquals(0, fakeCategoryDAO.findAll().size());
    }

    @Test
    @DisplayName("search should normalize criteria and calculate pagination")
    public void testSearchPagination() {
        categoryService.create(new CategoryModel("Java", "java"));
        categoryService.create(new CategoryModel("Technology", "technology"));

        CategoryListCriteria criteria = new CategoryListCriteria("  java  ", "unsupported", "invalid", 0);
        PageResult<CategoryModel> result = categoryService.search(criteria);

        assertEquals("java", criteria.getSearch());
        assertEquals("id", criteria.getSortName());
        assertEquals("asc", criteria.getSortBy());
        assertEquals(1, result.getPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(1L, result.getTotalItems());
        assertEquals("java", result.getItems().get(0).getCode());
        assertFalse(result.hasPrevious());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("search should reject a search term longer than the supported limit")
    public void testSearchTooLong() {
        String longSearch = new String(new char[101]).replace('\0', 'a');

        ValidationException ex = assertThrows(ValidationException.class,
                () -> categoryService.search(new CategoryListCriteria(longSearch, "id", "asc", 1)));

        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    @DisplayName("create should map a concurrent duplicate code to validation error")
    public void testCreateConcurrentDuplicateCode() {
        fakeCategoryDAO.insertFailure = new DatabaseException("duplicate category code",
                new SQLException("duplicate", "23000", 1062));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> categoryService.create(new CategoryModel("Java", "java")));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("non-duplicate integrity errors should remain database errors")
    public void testNonDuplicateIntegrityErrorsAreNotReportedAsDuplicateCodes() {
        DatabaseException insertFailure = new DatabaseException("other integrity error",
                new SQLException("foreign key", "23000", 1451));
        fakeCategoryDAO.insertFailure = insertFailure;

        assertSame(insertFailure, assertThrows(DatabaseException.class,
                () -> categoryService.create(new CategoryModel("Java", "java"))));

        fakeCategoryDAO.insertFailure = null;
        long id = categoryService.create(new CategoryModel("Java", "java"));
        DatabaseException updateFailure = new DatabaseException("other integrity error",
                new SQLException("foreign key", "23000", 1451));
        fakeCategoryDAO.updateFailure = updateFailure;

        assertSame(updateFailure, assertThrows(DatabaseException.class,
                () -> categoryService.update(new CategoryModel(id, "Java News", "java"))));
    }

    @Test
    @DisplayName("delete should map a foreign key violation to validation error")
    public void testDeleteForeignKeyViolation() {
        long id = categoryService.create(new CategoryModel("News Category", "news-category"));
        fakeCategoryDAO.throwForeignKeyOnDelete = true;

        ValidationException ex = assertThrows(ValidationException.class,
                () -> categoryService.delete(id));

        assertTrue(ex.getMessage().contains("news"));
    }

    private static class FakeCategoryDAO implements ICategoryDAO {
        private final Map<Long, CategoryModel> storage = new HashMap<>();
        private final Map<Long, Long> newsCountMap = new HashMap<>();
        private long autoIncrementId = 1L;
        private DatabaseException insertFailure;
        private DatabaseException updateFailure;
        private boolean throwForeignKeyOnDelete;

        public void setNewsCount(Long categoryId, Long count) {
            newsCountMap.put(categoryId, count);
        }

        @Override
        public List<CategoryModel> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public List<CategoryModel> findPage(CategoryListCriteria criteria, long offset, int limit) {
            List<CategoryModel> matches = new ArrayList<>();
            String search = criteria == null || criteria.getSearch() == null
                    ? "" : criteria.getSearch().toLowerCase(Locale.ROOT);
            for (CategoryModel model : storage.values()) {
                if (search.isEmpty()
                        || model.getName().toLowerCase(Locale.ROOT).contains(search)
                        || model.getCode().toLowerCase(Locale.ROOT).contains(search)) {
                    matches.add(model);
                }
            }
            int fromIndex = (int) Math.min(offset, matches.size());
            int toIndex = Math.min(fromIndex + limit, matches.size());
            return new ArrayList<>(matches.subList(fromIndex, toIndex));
        }

        @Override
        public long countBySearch(String search) {
            String normalizedSearch = search == null ? "" : search.toLowerCase(Locale.ROOT);
            return findPage(new CategoryListCriteria(normalizedSearch, "id", "asc", 1), 0,
                    Integer.MAX_VALUE).size();
        }

        @Override
        public CategoryModel findById(Long id) {
            return storage.get(id);
        }

        @Override
        public CategoryModel findByCode(String code) {
            for (CategoryModel model : storage.values()) {
                if (model.getCode().equalsIgnoreCase(code)) {
                    return model;
                }
            }
            return null;
        }

        @Override
        public long insert(CategoryModel category) {
            if (insertFailure != null) {
                throw insertFailure;
            }
            long newId = autoIncrementId++;
            CategoryModel saved = new CategoryModel(newId, category.getName(), category.getCode());
            storage.put(newId, saved);
            return newId;
        }

        @Override
        public int update(CategoryModel category) {
            if (updateFailure != null) {
                throw updateFailure;
            }
            if (storage.containsKey(category.getId())) {
                storage.put(category.getId(), category);
                return 1;
            }
            return 0;
        }

        @Override
        public int delete(Long id) {
            if (throwForeignKeyOnDelete) {
                throw new DatabaseException("category is referenced",
                        new SQLException("foreign key", "23000", 1451));
            }
            if (storage.remove(id) != null) {
                return 1;
            }
            return 0;
        }

        @Override
        public long countNewsByCategoryId(Long categoryId) {
            return newsCountMap.getOrDefault(categoryId, 0L);
        }
    }
}
