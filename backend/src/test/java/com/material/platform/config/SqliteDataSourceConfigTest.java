package com.material.platform.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqliteDataSourceConfigTest {

    @Test
    void shouldCreateSingleConnectionSqlitePoolWithBoundedCacheSettings() {
        SqliteDataSourceConfig config = new SqliteDataSourceConfig();

        try (HikariDataSource dataSource = config.sqliteDataSource("jdbc:sqlite:./data/test.db", "org.sqlite.JDBC")) {
            Properties properties = dataSource.getDataSourceProperties();

            assertEquals(1, dataSource.getMaximumPoolSize());
            assertEquals(1, dataSource.getMinimumIdle());
            assertEquals("SELECT 1", dataSource.getConnectionTestQuery());
            assertEquals("WAL", properties.getProperty("journal_mode"));
            assertEquals("NORMAL", properties.getProperty("synchronous"));
            assertEquals("FILE", properties.getProperty("temp_store"));
            assertEquals("-1024", properties.getProperty("cache_size"));
            assertEquals("5000", properties.getProperty("busy_timeout"));
        }
    }
}
