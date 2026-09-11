package com.example.news.utils;

import com.example.news.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseUtil {

    private static final String CONFIG_FILE = "db.properties";
    private static final String KEY_URL = "db.url";
    private static final String KEY_USERNAME = "db.username";
    private static final String KEY_PASSWORD = "db.password";

    private DatabaseUtil() {
        // Prevent instantiation
    }

    public static Connection getConnection() {
        Properties properties = loadProperties();
        String url = properties.getProperty(KEY_URL);
        String username = properties.getProperty(KEY_USERNAME);
        String password = properties.getProperty(KEY_PASSWORD, "");

        if (url == null || url.trim().isEmpty()) {
            throw new DatabaseException("Missing required property: " + KEY_URL);
        }
        if (username == null || username.trim().isEmpty()) {
            throw new DatabaseException("Missing required property: " + KEY_USERNAME);
        }

        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish database connection to: " + url, e);
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new DatabaseException("Database configuration file not found on classpath: " + CONFIG_FILE);
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new DatabaseException("Failed to load database configuration file: " + CONFIG_FILE, e);
        }
    }
}
