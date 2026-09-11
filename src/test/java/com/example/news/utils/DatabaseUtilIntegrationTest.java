package com.example.news.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseUtilIntegrationTest {

    @Test
    @DisplayName("Verify DatabaseUtil establishes valid connection and closes properly")
    @EnabledIfSystemProperty(named = "runDbTests", matches = "true")
    public void testGetConnectionSuccess() throws SQLException {
        Connection connectionRef = null;
        try (Connection connection = DatabaseUtil.getConnection()) {
            connectionRef = connection;
            assertNotNull(connection, "Connection should not be null");
            assertFalse(connection.isClosed(), "Connection should be open");

            DatabaseMetaData metaData = connection.getMetaData();
            assertNotNull(metaData, "Metadata should not be null");
            assertEquals("MySQL", metaData.getDatabaseProductName(), "Database product name should be MySQL");
        }

        assertNotNull(connectionRef, "Connection reference should have been assigned");
        assertTrue(connectionRef.isClosed(), "Connection should be closed after try-with-resources block");
    }
}
