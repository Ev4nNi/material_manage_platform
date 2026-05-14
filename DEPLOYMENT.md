# 素材管理平台 - Docker 部署指南

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+

---

## 服务器目录结构

服务器数据目录：`/data2/material_manage_platform/`

```
/data2/material_manage_platform/
├── deploy-package/      # 部署包（解压后）
├── storage/             # 素材文件存储
├── db/                  # SQLite 数据库
└── logs/                # 应用日志
```

---

## 打包步骤（在开发机器上执行）

### 1. 创建打包目录

在项目根目录下创建 `deploy-package` 文件夹：

```bash
mkdir deploy-package
```

### 2. 复制以下文件到 deploy-package 目录

```
deploy-package/
├── backend/             # 后端服务（含 Dockerfile）
├── frontend/            # 前端服务（含 Dockerfile）
├── docker-compose.yml   # Docker Compose 配置
└── DEPLOYMENT.md        # 部署说明文档（可选）
```

### 3. 执行打包

在项目根目录执行：

```bash
tar -czvf material_manage_platform.tar.gz -C deploy-package .
```

---

## 部署步骤（在服务器上执行）

### 1. 上传并解压

```bash
# 上传 material_manage_platform.tar.gz 到服务器 /data2/material_manage_platform/

# 解压
cd /data2/material_manage_platform
tar -xzvf material_manage_platform.tar.gz
```

解压后目录结构：

```
/data2/material_manage_platform/
└── deploy-package/
    ├── backend/
    ├── frontend/
    ├── docker-compose.yml
    └── DEPLOYMENT.md
```

### 2. 创建数据目录

```bash
mkdir -p /data2/material_manage_platform/{storage,db,logs}
```

### 3. 配置（如需要修改端口）

```bash
cd /data2/material_manage_platform/deploy-package
vim docker-compose.yml
```

```yaml
services:
  backend:
    ports:
      - "8081:8080"    # 主机端口:容器端口
  frontend:
    ports:
      - "3001:80"      # 主机端口:容器端口
```

### 4. 构建并启动

```bash
cd /data2/material_manage_platform/deploy-package

# 前后端一起构建并启动
docker-compose up -d --build
```

### 5. 验证部署

```bash
# 查看容器状态
docker ps

# 测试后端 API
curl http://localhost:8081/api/auth/me
# 预期返回: {"code":401,"message":"请先登录","data":null}

# 测试前端
curl http://localhost:3001
```

---

## 服务地址

| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8081 |
| 前端界面 | http://localhost:3001 |

---

## 常用运维命令

```bash
# 查看所有服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
docker logs -f material-backend

# 重启服务
docker-compose restart

# 停止服务
docker-compose down

# 重新构建并启动
docker-compose up -d --build
```

---

## 数据管理

### 备份

```bash
# 备份数据库
cp /data2/material_manage_platform/db/material.db /data2/material_manage_platform/db/material.db.$(date +%Y%m%d)

# 备份存储
tar -czf /data2/material_manage_platform/storage.tar.gz.$(date +%Y%m%d) /data2/material_manage_platform/storage/
```

### 恢复

```bash
# 停止服务
docker-compose down

# 恢复数据库
cp /data2/material_manage_platform/db/material.db.backup /data2/material_manage_platform/db/material.db

# 重新启动
docker-compose up -d
```

---

## 故障排查

### 容器显示 unhealthy

健康检查把 401 响应当作错误。如遇此情况，修改 `docker-compose.yml`：

```yaml
healthcheck:
  test: "wget -q -O - http://localhost:8080/api/auth/me || true"
```

### 后端无法启动

```bash
# 查看日志
docker logs material-backend

# 检查数据目录权限
ls -la /data2/material_manage_platform/
```

### 前端无法访问

```bash
# 查看前端日志
docker logs material-frontend

# 检查端口占用
netstat -tlnp | grep 3001
```

---

## 环境变量说明

### 后端

| 变量 | 默认值 | 说明 |
|------|--------|------|
| APP_PORT | 8080 | 容器内端口 |
| STORAGE_PATH | /data/storage | 素材存储路径 |
| DB_PATH | /data/db/material.db | 数据库路径 |
| LOG_PATH | /data/logs/application.log | 日志路径 |
| MAX_FILE_SIZE | 500MB | 最大上传文件大小 |

---

## 卸载

### 停止并删除容器

```bash
cd /data2/material_manage_platform/deploy-package

# 停止服务
docker-compose down

# 删除容器（如需要）
docker rm -f material-backend material-frontend
```

### 删除部署文件

```bash
# 删除部署目录
rm -rf /data2/material_manage_platform/deploy-package

# 删除 Docker 镜像（如需要）
docker rmi material-backend:latest
docker rmi material-frontend:latest
```

### 数据清理（可选）

如需完全清理所有数据：

```bash
# 删除数据目录（请提前备份重要数据）
rm -rf /data2/material_manage_platform/{storage,db,logs}

# 删除 Docker 卷（如使用了 volume）
docker volume rm material_manage_platform_storage material_manage_platform_db
```
