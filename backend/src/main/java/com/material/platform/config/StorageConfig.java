package com.material.platform.config;

import com.material.platform.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Value("${storage.strategy:local}")
    private String strategy;

    @Bean
    public StorageService storageService(
            StorageService localStorageService,
            StorageService tosStorageService
    ) {
        switch (strategy.toLowerCase()) {
            case "local":
                return localStorageService;
            case "tos":
                return tosStorageService;
            default:
                throw new IllegalArgumentException("不支持的存储策略: " + strategy + "，支持的值: local, tos");
        }
    }
}
