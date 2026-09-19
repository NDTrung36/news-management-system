package com.example.news.dao;

import com.example.news.dao.impl.CategoryDAO;
import com.example.news.dao.impl.GenericDAO;
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
    private static final String USED_CATEGORY_CODE = "dao-int-used-category";
    private static final String UNUSED_CATEGORY_CODE = "dao-int-unused-category";

    private ICategoryDAO categoryDAO;
    private GenericDAO genericDAO;

    @BeforeEach
    public void setUp() {
        categoryDAO = new CategoryDAO();
        genericDAO = new GenericDAO();
        cleanupTestData();
    }

    @AfterEach
    public void tearDown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        genericDAO.delete("DELETE n FROM news n JOIN category c ON c.id = n.category_id WHERE c.code IN (?, ?)",
                USED_CATEGORY_CODE, UNUSED_CATEGORY_CODE);
        CategoryModel byCode = categoryDAO.findByCode(TEST_CODE);
        if (byCode != null) {
            categoryDAO.delete(byCode.getId());
        }
        CategoryModel byUpdatedCode = categoryDAO.findByCode(UPDATED_CODE);
        if (byUpdatedCode != null) {
            categoryDAO.delete(byUpdatedCode.getId());
        }
        CategoryModel usedCategory = categoryDAO.findByCode(USED_CATEGORY_CODE);
        if (usedCategory != null) {
            categoryDAO.delete(usedCategory.getId());
        }
        CategoryModel unusedCategory = categoryDAO.findByCode(UNUSED_CATEGORY_CODE);
        if (unusedCategory != null) {
            categoryDAO.delete(unusedCategory.getId());
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
    @DisplayName("countNewsByCategoryId should return positive count for a referenced category and 0 for unused")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testCountNewsByCategoryId() {
        long usedCategoryId = categoryDAO.insert(new CategoryModel("DAO Used Category", USED_CATEGORY_CODE));
        long unusedCategoryId = categoryDAO.insert(new CategoryModel("DAO Unused Category", UNUSED_CATEGORY_CODE));
        Long createdById = genericDAO.queryOne("SELECT id FROM `user` ORDER BY id LIMIT 1",
                resultSet -> resultSet.getLong("id"));
        assertNotNull(createdById, "Test database must contain a user for the news fixture");

        genericDAO.insert("INSERT INTO news (title, short_description, content, thumbnail, category_id, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                "DAO category count test", "Temporary integration-test news", "Temporary integration-test content",
                null, usedCategoryId, createdById);

        assertEquals(1L, categoryDAO.countNewsByCategoryId(usedCategoryId));
        assertEquals(0L, categoryDAO.countNewsByCategoryId(unusedCategoryId));
    }
}
