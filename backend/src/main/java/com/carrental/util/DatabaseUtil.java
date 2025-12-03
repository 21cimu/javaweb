package com.carrental.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database utility class for managing database connections
 * Uses HikariCP connection pool for optimal performance
 */
public class DatabaseUtil {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);
    private static HikariDataSource dataSource;
    private static final Properties properties = new Properties();

    static {
        try {
            loadProperties();
            initializeDataSource();
        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void loadProperties() throws IOException {
        try (InputStream input = DatabaseUtil.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("Unable to find application.properties");
            }
            properties.load(input);
        }
    }

    private static void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getProperty("db.url"));
        config.setUsername(properties.getProperty("db.username"));
        config.setPassword(properties.getProperty("db.password"));
        config.setDriverClassName(properties.getProperty("db.driver"));
        
        config.setMaximumPoolSize(Integer.parseInt(
            properties.getProperty("db.pool.size", "10")));
        config.setMinimumIdle(Integer.parseInt(
            properties.getProperty("db.pool.minIdle", "5")));
        config.setMaxLifetime(Long.parseLong(
            properties.getProperty("db.pool.maxLifetime", "1800000")));
        config.setConnectionTimeout(Long.parseLong(
            properties.getProperty("db.pool.connectionTimeout", "30000")));
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized successfully");
    }

    /**
     * Get a connection from the pool
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Close the connection pool
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    /**
     * Get a property value
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get a property value with default
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
