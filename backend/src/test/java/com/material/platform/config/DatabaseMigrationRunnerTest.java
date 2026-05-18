package com.material.platform.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void schemaInitializationAndMigrationShouldWorkForLegacyAssetsTable() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:sqlite:" + tempDir.resolve("legacy.db"));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE folders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    parent_id INTEGER NOT NULL DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE assets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    folder_id INTEGER NOT NULL,
                    original_name TEXT NOT NULL,
                    storage_key TEXT NOT NULL UNIQUE,
                    file_type TEXT,
                    file_size INTEGER,
                    upload_date TEXT NOT NULL,
                    metadata TEXT DEFAULT '{}',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    display_name TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.update("""
                INSERT INTO assets(folder_id, original_name, storage_key, file_type, file_size, upload_date, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, 1L, "legacy.jpg", "2026/05/06/legacy.jpg", "image", 123L, "2026-05-06", "{}");
        jdbcTemplate.update("""
                INSERT INTO folders(id, name, parent_id, created_at)
                VALUES (?, ?, ?, ?)
                """, 1L, "20260506", 0L, "2026-05-06 10:00:00");
        jdbcTemplate.update("""
                INSERT INTO users(username, password_hash, display_name)
                VALUES (?, ?, ?)
                """, "admin", "hash", "Administrator");

        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new ClassPathResource("db/schema.sql")),
                dataSource
        );

        DatabaseMigrationRunner runner = new DatabaseMigrationRunner(jdbcTemplate);
        runner.migrateUsersTable();

        List<Map<String, Object>> assetColumns = jdbcTemplate.queryForList("PRAGMA table_info(assets)");
        assertTrue(hasColumn(assetColumns, "public_id"));
        assertTrue(hasColumn(assetColumns, "uploaded_by"));

        List<Map<String, Object>> folderColumns = jdbcTemplate.queryForList("PRAGMA table_info(folders)");
        assertTrue(hasColumn(folderColumns, "updated_at"));
        Map<String, Object> migratedFolder = jdbcTemplate.queryForMap(
                "SELECT updated_at FROM folders WHERE id = 1"
        );
        assertEquals("2026-05-06 10:00:00", migratedFolder.get("updated_at"));

        Map<String, Object> migratedAsset = jdbcTemplate.queryForMap(
                "SELECT public_id, uploaded_by FROM assets WHERE id = 1"
        );
        assertNotNull(migratedAsset.get("public_id"));
        assertFalse(String.valueOf(migratedAsset.get("public_id")).isBlank());
        assertEquals("admin", migratedAsset.get("uploaded_by"));

        List<Map<String, Object>> assetIndexes = jdbcTemplate.queryForList("PRAGMA index_list(assets)");
        assertTrue(assetIndexes.stream().anyMatch(index -> "idx_assets_public_id".equals(index.get("name"))));
        assertTrue(assetIndexes.stream().anyMatch(index -> "idx_assets_uploaded_by".equals(index.get("name"))));

        List<Map<String, Object>> folderIndexes = jdbcTemplate.queryForList("PRAGMA index_list(folders)");
        assertTrue(folderIndexes.stream().anyMatch(index -> "idx_folders_updated_at".equals(index.get("name"))));
    }

    private boolean hasColumn(List<Map<String, Object>> columns, String columnName) {
        return columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(columnName::equalsIgnoreCase);
    }
}
