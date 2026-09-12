package com.example.news.service;

import com.example.news.dao.ICategoryDAO;
import com.example.news.exception.ValidationException;
import com.example.news.model.CategoryModel;
import com.example.news.service.impl.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private static class FakeCategoryDAO implements ICategoryDAO {
        private final Map<Long, CategoryModel> storage = new HashMap<>();
        private final Map<Long, Long> newsCountMap = new HashMap<>();
        private long autoIncrementId = 1L;

        public void setNewsCount(Long categoryId, Long count) {
            newsCountMap.put(categoryId, count);
        }

        @Override
        public List<CategoryModel> findAll() {
            return new ArrayList<>(storage.values());
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
            long newId = autoIncrementId++;
            CategoryModel saved = new CategoryModel(newId, category.getName(), category.getCode());
            storage.put(newId, saved);
            return newId;
        }

        @Override
        public int update(CategoryModel category) {
            if (storage.containsKey(category.getId())) {
                storage.put(category.getId(), category);
                return 1;
            }
            return 0;
        }

        @Override
        public int delete(Long id) {
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
