package com.example.news.jdbc;

import com.example.news.utils.DatabaseUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CategoryPreparedStatementIntegrationTest {

    @Test
    @DisplayName("Verify raw PreparedStatement and ResultSet query on Category with try-with-resources")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testSelectCategoryByCode() throws SQLException {
        String sql = "SELECT id, name, code, created_date, modified_date FROM category WHERE code = ?";
        String targetCode = "java";

        long id = -1;
        String name = null;
        String code = null;
        Timestamp createdDate = null;
        Timestamp modifiedDate = null;

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, targetCode);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "Category with code 'java' should exist in seed data");

                id = resultSet.getLong("id");
                name = resultSet.getString("name");
                code = resultSet.getString("code");
                createdDate = resultSet.getTimestamp("created_date");
                modifiedDate = resultSet.getTimestamp("modified_date");

                assertFalse(resultSet.next(), "Should not return multiple rows for unique category code");
            }
        }

        assertTrue(id > 0, "Category ID should be positive");
        assertEquals("Java", name, "Category name should match seed data");
        assertEquals("java", code, "Category code should match query parameter");
        assertNotNull(createdDate, "Created date should not be null");
    }
}
