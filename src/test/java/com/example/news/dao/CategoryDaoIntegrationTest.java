package com.example.news.dao;

import com.example.news.dao.impl.CategoryDAO;
import com.example.news.model.CategoryModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CategoryDaoIntegrationTest {

    private static final String TEST_CODE = "dao-int-code";
    private static final String TEST_NAME = "DAO Integration Category";
    private static final String UPDATED_CODE = "dao-int-code-upd";
    private static final String UPDATED_NAME = "DAO Integration Category Updated";

    private ICategoryDAO categoryDAO;

    @BeforeEach
    public void setUp() {
        categoryDAO = new CategoryDAO();
        cleanupTestData();
    }

    @AfterEach
    public void tearDown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        CategoryModel byCode = categoryDAO.findByCode(TEST_CODE);
        if (byCode != null) {
            categoryDAO.delete(byCode.getId());
        }
        CategoryModel byUpdatedCode = categoryDAO.findByCode(UPDATED_CODE);
        if (byUpdatedCode != null) {
            categoryDAO.delete(byUpdatedCode.getId());
        }
    }

    @Test
    @DisplayName("findAll should return seed categories with properly mapped fields")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testFindAll() {
        List<CategoryModel> categories = categoryDAO.findAll();
        assertNotNull(categories);
        assertTrue(categories.size() >= 4, "Should have at least the 4 seed categories");

        CategoryModel first = categories.get(0);
        assertNotNull(first.getId());
        assertNotNull(first.getName());
        assertNotNull(first.getCode());
        assertNotNull(first.getCreatedDate(), "createdDate should be mapped from created_date");
    }

    @Test
    @DisplayName("findByCode and findById should return matching category")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testFindByCodeAndFindById() {
        CategoryModel byCode = categoryDAO.findByCode("java");
        assertNotNull(byCode);
        assertEquals("Java", byCode.getName());
        assertEquals("java", byCode.getCode());

        CategoryModel byId = categoryDAO.findById(byCode.getId());
        assertNotNull(byId);
        assertEquals(byCode.getId(), byId.getId());
        assertEquals("Java", byId.getName());
    }

    @Test
    @DisplayName("verify insert, find, update and delete lifecycle via CategoryDAO")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testCategoryCrudLifecycle() {
        // 1. Precondition
        assertNull(categoryDAO.findByCode(TEST_CODE));

        // 2. Insert
        CategoryModel newCategory = new CategoryModel(TEST_NAME, TEST_CODE);
        long generatedId = categoryDAO.insert(newCategory);
        assertTrue(generatedId > 0, "Insert should return generated positive ID");

        // 3. Find By ID
        CategoryModel inserted = categoryDAO.findById(generatedId);
        assertNotNull(inserted);
        assertEquals(TEST_NAME, inserted.getName());
        assertEquals(TEST_CODE, inserted.getCode());
        assertNotNull(inserted.getCreatedDate());

        // 4. Update
        inserted.setName(UPDATED_NAME);
        inserted.setCode(UPDATED_CODE);
        int updatedRows = categoryDAO.update(inserted);
        assertEquals(1, updatedRows);

        CategoryModel updated = categoryDAO.findById(generatedId);
        assertNotNull(updated);
        assertEquals(UPDATED_NAME, updated.getName());
        assertEquals(UPDATED_CODE, updated.getCode());
        assertNotNull(updated.getModifiedDate(), "modifiedDate should be set by CURRENT_TIMESTAMP");

        // 5. Delete
        int deletedRows = categoryDAO.delete(generatedId);
        assertEquals(1, deletedRows);

        assertNull(categoryDAO.findById(generatedId));
        assertNull(categoryDAO.findByCode(UPDATED_CODE));
    }

    @Test
    @DisplayName("countNewsByCategoryId should return positive count for categories in use and 0 for unused")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testCountNewsByCategoryId() {
        CategoryModel javaCategory = categoryDAO.findByCode("java");
        assertNotNull(javaCategory);
        long javaNewsCount = categoryDAO.countNewsByCategoryId(javaCategory.getId());
        assertTrue(javaNewsCount > 0, "Java category should have associated news");

        CategoryModel programmingCategory = categoryDAO.findByCode("programming");
        assertNotNull(programmingCategory);
        long progNewsCount = categoryDAO.countNewsByCategoryId(programmingCategory.getId());
        assertEquals(0, progNewsCount, "Programming category should have 0 news");
    }
}
