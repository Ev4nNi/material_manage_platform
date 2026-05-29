package com.material.platform.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.sqlite.SQLiteConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class SqliteDataSourceConfig {

    private static final int SQLITE_BUSY_TIMEOUT_MS = 5_000;
    private static final int SQLITE_CACHE_SIZE_KIB = -1_024;

    @Bean(destroyMethod = "close")
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.driver-class-name:org.sqlite.JDBC}") String driverClassName
    ) {
        return sqliteDataSource(jdbcUrl, driverClassName);
    }

    HikariDataSource sqliteDataSource(String jdbcUrl, String driverClassName) {
        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.setBusyTimeout(SQLITE_BUSY_TIMEOUT_MS);
        sqliteConfig.setCacheSize(SQLITE_CACHE_SIZE_KIB);
        sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        sqliteConfig.setTempStore(SQLiteConfig.TempStore.FILE);
        sqliteConfig.setSharedCache(false);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("sqlite-pool");
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaxLifetime(0);
        hikariConfig.setConnectionTimeout(10_000);
        hikariConfig.setValidationTimeout(5_000);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setDataSourceProperties(sqliteConfig.toProperties());
        return new HikariDataSource(hikariConfig);
    }
}
