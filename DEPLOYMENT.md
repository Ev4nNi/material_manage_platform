# 素材管理平台 Docker 部署指南

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+

## docker-compose 说明

当前 `docker-compose.yml` 使用两个本地构建上下文：

- `backend`：执行 `build: ./backend`，后端 Dockerfile 只会读取 `target/*.jar`。
- `frontend`：执行 `build: ./frontend`，前端 Dockerfile 会在容器内执行 `npm install` 和 `npm run build`。

服务端数据目录固定为 `/data2/material_manage_platform/`：

```text
/data2/material_manage_platform/
├── deploy-package/      # 部署包解压目录
├── storage/             # 素材文件
├── db/                  # SQLite 数据库
└── logs/                # 应用日志
```

端口映射：

```yaml
services:
  backend:
    ports:
      - "8081:8080"
  frontend:
    ports:
      - "3001:80"
```

## 最小部署包内容

部署包只需要包含 Docker 构建和运行必需文件：

```text
deploy-package/
├── backend/
│   ├── Dockerfile
│   └── target/
│       └── material-manage-platform-1.0.0.jar
├── frontend/
│   ├── Dockerfile
│   ├── index.html
│   ├── nginx.conf
│   ├── package.json
│   ├── package-lock.json
│   ├── vite.config.js
│   └── src/
└── docker-compose.yml
```

不需要打包的内容：

- `backend/src/`、`backend/pom.xml`、`backend/src/test/`：后端镜像直接使用已构建 jar。
- `frontend/dist/`、`frontend/node_modules/`：前端镜像会在 Docker build 中生成。
- `.git/`、IDE 配置、README、开发脚本、历史压缩包、测试报告等。

## 打包步骤（Windows PowerShell）

在项目根目录 `D:\programProject\workproject\material_manage_platform` 执行：

```powershell
# 1. 构建后端 jar
mvn -f .\backend\pom.xml -DskipTests package

# 2. 可选：验证前端可构建
npm --prefix .\frontend run build

# 3. 重建最小部署目录
$packageRoot = Resolve-Path .\deploy-package -ErrorAction SilentlyContinue
if ($packageRoot) {
    Remove-Item -LiteralPath $packageRoot.Path -Recurse -Force
}

New-Item -ItemType Directory -Force -Path .\deploy-package\backend\target | Out-Null
New-Item -ItemType Directory -Force -Path .\deploy-package\frontend\src | Out-Null

Copy-Item -Path .\docker-compose.yml -Destination .\deploy-package\docker-compose.yml -Force

Copy-Item -Path .\backend\Dockerfile -Destination .\deploy-package\backend\Dockerfile -Force
Copy-Item -Path .\backend\target\material-manage-platform-1.0.0.jar -Destination .\deploy-package\backend\target\material-manage-platform-1.0.0.jar -Force

$frontendFiles = @(
    '.\frontend\Dockerfile',
    '.\frontend\index.html',
    '.\frontend\nginx.conf',
    '.\frontend\package.json',
    '.\frontend\package-lock.json',
    '.\frontend\vite.config.js'
)
Copy-Item -Path $frontendFiles -Destination .\deploy-package\frontend -Force
Copy-Item -Path .\frontend\src\* -Destination .\deploy-package\frontend\src -Recurse -Force

# 4. 生成压缩包
tar -czvf .\material_manage_platform.tar.gz -C .\deploy-package .
```

注意：`deploy-package\backend\target` 中只保留正式 jar，不要放入 `*.jar.original`，否则 `backend/Dockerfile` 的 `COPY target/*.jar app.jar` 可能匹配到多个文件。

## 部署步骤（服务器）

上传 `material_manage_platform.tar.gz` 到 `/data2/material_manage_platform/`，然后执行：

```bash
cd /data2/material_manage_platform
mkdir -p storage db logs deploy-package
tar -xzvf material_manage_platform.tar.gz -C deploy-package
cd deploy-package
docker-compose up -d --build
```

验证：

```bash
docker-compose ps
curl http://localhost:8081/api/auth/me
curl http://localhost:3001
```

预期后端未登录接口返回 `401` 是正常行为。

## 常用运维命令

```bash
cd /data2/material_manage_platform/deploy-package

docker-compose ps
docker-compose logs -f
docker-compose restart
docker-compose down
docker-compose up -d --build
```

## 数据备份

```bash
cp /data2/material_manage_platform/db/material.db /data2/material_manage_platform/db/material.db.$(date +%Y%m%d)
tar -czf /data2/material_manage_platform/storage.tar.gz.$(date +%Y%m%d) /data2/material_manage_platform/storage/
```

## 故障排查

查看后端日志：

```bash
docker logs material-backend
```

查看前端日志：

```bash
docker logs material-frontend
```

检查数据目录：

```bash
ls -la /data2/material_manage_platform/
```

