package com.material.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.material.platform.mapper")
public class MaterialPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaterialPlatformApplication.class, args);
    }

}
