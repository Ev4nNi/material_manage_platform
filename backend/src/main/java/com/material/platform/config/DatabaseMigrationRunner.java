package com.material.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DatabaseMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateUsersTable() {
        migrateUsersRoleColumn();
        migrateAssetsUploadedByColumn();
        migrateAssetsPublicIdColumn();
        migrateFoldersUpdatedAtColumn();
    }

    private void migrateUsersRoleColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(users)");
        boolean hasRoleColumn = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch("role"::equalsIgnoreCase);

        if (!hasRoleColumn) {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN role TEXT DEFAULT 'USER'");
        }

        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = 'admin'");
        jdbcTemplate.update("UPDATE users SET role = 'USER' WHERE username <> 'admin' AND (role IS NULL OR role = '')");
    }

    private void migrateAssetsUploadedByColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(assets)");
        boolean hasUploadedByColumn = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch("uploaded_by"::equalsIgnoreCase);

        if (!hasUploadedByColumn) {
            jdbcTemplate.execute("ALTER TABLE assets ADD COLUMN uploaded_by TEXT DEFAULT 'admin'");
            jdbcTemplate.execute("UPDATE assets SET uploaded_by = 'admin' WHERE uploaded_by IS NULL OR uploaded_by = ''");
        }

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_assets_uploaded_by ON assets(uploaded_by)");
    }

    private void migrateAssetsPublicIdColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(assets)");
        boolean hasPublicIdColumn = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch("public_id"::equalsIgnoreCase);

        if (!hasPublicIdColumn) {
            jdbcTemplate.execute("ALTER TABLE assets ADD COLUMN public_id TEXT");
        }

        List<Map<String, Object>> assetsWithoutPublicId = jdbcTemplate.queryForList(
                "SELECT id FROM assets WHERE public_id IS NULL OR public_id = ''");
        for (Map<String, Object> asset : assetsWithoutPublicId) {
            jdbcTemplate.update(
                    "UPDATE assets SET public_id = ? WHERE id = ?",
                    UUID.randomUUID().toString(),
                    asset.get("id")
            );
        }

        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_assets_public_id ON assets(public_id)");
    }

    private void migrateFoldersUpdatedAtColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(folders)");
        boolean hasUpdatedAtColumn = columns.stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch("updated_at"::equalsIgnoreCase);

        if (!hasUpdatedAtColumn) {
            jdbcTemplate.execute("ALTER TABLE folders ADD COLUMN updated_at DATETIME");
        }

        jdbcTemplate.execute("""
                UPDATE folders
                SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
                WHERE updated_at IS NULL OR updated_at = ''
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_folders_updated_at ON folders(updated_at)");
    }
}
