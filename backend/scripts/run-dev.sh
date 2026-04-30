#!/bin/bash

echo "============================================="
echo "  素材管理平台 - 开发环境启动脚本"
echo "============================================="

cd "$(dirname "$0")/.."

mkdir -p data
mkdir -p storage

echo ""
echo "启动配置："
echo "  - 数据库路径: ./data/material.db"
echo "  - 存储路径: ./storage"
echo "  - 服务端口: 8080"
echo ""

echo "正在启动 Spring Boot 应用..."
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=dev
