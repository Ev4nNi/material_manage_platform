package com.material.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DatabaseMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateUsersTable() {
        migrateUsersRoleColumn();
        migrateAssetsUploadedByColumn();
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
    }
}
