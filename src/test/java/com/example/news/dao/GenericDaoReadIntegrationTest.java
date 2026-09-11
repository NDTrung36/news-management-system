package com.example.news.dao;

import com.example.news.dao.impl.GenericDAO;
import com.example.news.mapper.RowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenericDaoReadIntegrationTest {

    private IGenericDAO genericDAO;

    @BeforeEach
    public void setUp() {
        genericDAO = new GenericDAO();
    }

    private static class CategoryRow {
        private final Long id;
        private final String name;
        private final String code;
        private final Timestamp createdDate;

        public CategoryRow(Long id, String name, String code, Timestamp createdDate) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.createdDate = createdDate;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getCode() { return code; }
        public Timestamp getCreatedDate() { return createdDate; }
    }

    private final RowMapper<CategoryRow> categoryMapper = new RowMapper<CategoryRow>() {
        @Override
        public CategoryRow mapRow(ResultSet rs) throws SQLException {
            return new CategoryRow(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("code"),
                    rs.getTimestamp("created_date")
            );
        }
    };

    @Test
    @DisplayName("Verify GenericDAO.query returns populated list of categories")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testQueryCategories() {
        String sql = "SELECT id, name, code, created_date FROM category";
        List<CategoryRow> categories = genericDAO.query(sql, categoryMapper);

        assertNotNull(categories, "Result list should not be null");
        assertTrue(categories.size() >= 4, "Should have at least 4 seed categories");
    }

    @Test
    @DisplayName("Verify GenericDAO.queryOne returns matching entity or null")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testQueryOneCategory() {
        String sql = "SELECT id, name, code, created_date FROM category WHERE code = ?";
        CategoryRow found = genericDAO.queryOne(sql, categoryMapper, "java");

        assertNotNull(found, "Category 'java' should exist");
        assertEquals("Java", found.getName());
        assertEquals("java", found.getCode());

        CategoryRow notFound = genericDAO.queryOne(sql, categoryMapper, "non_existent_code_xyz");
        assertNull(notFound, "Should return null when no record matches");
    }

    @Test
    @DisplayName("Verify GenericDAO.count returns valid row count")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testCountCategories() {
        String sql = "SELECT COUNT(*) FROM category";
        long total = genericDAO.count(sql);

        assertTrue(total >= 4, "Category count should be at least 4");
    }
}
