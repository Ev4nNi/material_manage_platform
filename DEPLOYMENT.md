# 素材管理平台 Docker 部署指南

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 推荐机器配置：`1C2G`

## 当前部署参数说明

当前源码中实际生效的容器与 JVM 参数如下：

### 后端容器资源限制

- `mem_limit: 1024m`
- `cpus: 1.0`
- `pids_limit: 256`
- `nofile: 65535`

### 前端容器资源限制

- `mem_limit: 256m`
- `cpus: 0.5`
- `pids_limit: 128`

### 后端默认 JVM 参数

后端镜像通过 `JAVA_TOOL_OPTIONS` 注入以下默认参数：

```text
-Xms256m
-Xmx384m
-XX:MaxMetaspaceSize=128m
-XX:MaxDirectMemorySize=128m
-Xss512k
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/logs/heapdump.hprof
-XX:+ExitOnOutOfMemoryError
```

说明：

- `Xmx384m` 控制 Java 堆上限，避免后端在 `1C2G` 机器上无限吃内存。
- `MaxDirectMemorySize=128m` 为 Undertow、NIO 和文件传输留出堆外缓冲。
- `Xss512k` 用于收紧线程栈占用。
- `HeapDumpOnOutOfMemoryError` 和 `ExitOnOutOfMemoryError` 用于故障排查和容器自动恢复。

## docker-compose 结构

当前源码仓库里的 `docker-compose.yml` 使用两个本地构建上下文：

- `backend`：执行 `build: ./backend`，后端镜像读取 `backend/target/*.jar`
- `frontend`：执行 `build: ./frontend`，源码环境下前端镜像会在容器内执行 `npm install` 和 `npm run build`

部署包场景单独使用前端运行时 Dockerfile：

- 源码开发：使用 `frontend/Dockerfile`
- 最小部署包：使用 `frontend/Dockerfile.runtime`
- 最小部署包中的前端只包含 `dist` 静态资源和 `nginx.conf`，服务器上不再执行前端构建

服务端数据目录固定为 `/data2/material_manage_platform/`：

```text
/data2/material_manage_platform/
├── deploy-package/   # 部署包解压目录
├── storage/          # 素材文件
├── db/               # SQLite 数据库
└── logs/             # 应用日志和 heapdump
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
│   ├── nginx.conf
│   └── dist/
│       ├── index.html
│       └── assets/
└── docker-compose.yml
```

不需要打包的内容：

- `backend/src/`、`backend/pom.xml`、`backend/src/test/`
- `frontend/src/`、`frontend/package.json`、`frontend/package-lock.json`、`frontend/vite.config.js`
- `frontend/node_modules/`
- `.git/`、IDE 配置、README、历史压缩包、测试报告等

## 打包步骤（Windows PowerShell）

在项目根目录 `D:\programProject\workproject\material_manage_platform` 执行：

```powershell
# 1. 构建后端 jar
mvn -f .\backend\pom.xml -DskipTests package

# 2. 构建前端 dist
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

Copy-Item -Path .\frontend\Dockerfile.runtime -Destination .\deploy-package\frontend\Dockerfile -Force
Copy-Item -Path .\frontend\nginx.conf -Destination .\deploy-package\frontend\nginx.conf -Force
Copy-Item -Path .\frontend\dist -Destination .\deploy-package\frontend\dist -Recurse -Force

# 4. 生成压缩包
tar -czvf .\material_manage_platform.tar.gz -C .\deploy-package .
```

注意：

- `deploy-package\backend\target` 中只保留正式 jar，不要放入 `*.jar.original`
- 否则 `backend/Dockerfile` 中的 `COPY target/*.jar app.jar` 可能匹配到多个文件
- `deploy-package\frontend` 中不再包含前端源码，服务器端不会再执行 `npm install` 或 `npm run build`

## 服务端部署步骤

上传 `material_manage_platform.tar.gz` 到 `/data2/material_manage_platform/`，然后执行：

```bash
cd /data2/material_manage_platform
mkdir -p storage db logs deploy-package
tar -xzvf material_manage_platform.tar.gz -C deploy-package
cd deploy-package
docker compose up -d --build
```

## 参数覆盖方式

如果线上机器和默认 `1C2G` 假设不一致，可以直接在 `docker-compose.yml` 中覆盖：

```yaml
services:
  backend:
    mem_limit: 768m
    cpus: 0.8
    environment:
      - JAVA_TOOL_OPTIONS=-Xms192m -Xmx320m -XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=96m -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/logs/heapdump.hprof -XX:+ExitOnOutOfMemoryError
```

建议：

- 不要把 `Xmx` 直接拉到容器上限附近
- `Xmx` 最好控制在容器内存的 `35%` 到 `50%`
- 如果上传大文件较多，优先保留 `MaxDirectMemorySize`

## 验证

```bash
docker compose ps
docker compose logs material-backend --tail=100
docker compose logs material-frontend --tail=100
curl http://localhost:8081/api/auth/me
curl http://localhost:3001
```

预期：

- 后端未登录接口返回 `401` 属于正常行为
- 前端首页可以正常返回

## 常用运维命令

```bash
cd /data2/material_manage_platform/deploy-package

docker compose ps
docker compose logs -f
docker compose restart
docker compose down
docker compose up -d --build
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

查看数据目录：

```bash
ls -la /data2/material_manage_platform/
```

如果发生 OOM：

- 检查 `/data2/material_manage_platform/logs/heapdump.hprof` 是否生成
- 查看 `docker inspect material-backend` 中的退出码和 OOMKilled 状态
- 根据实际峰值再调整 `mem_limit` 和 `JAVA_TOOL_OPTIONS`
