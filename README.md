# 素材管理平台

轻量级图片和视频素材管理平台，支持文件夹组织、批量操作、元数据提取和预览功能。

## 系统简介

素材管理平台是一个基于 Spring Boot 3.x 和 Vue 3 构建的全栈应用，用于高效管理图片和视频素材。系统提供直观的文件夹树结构组织素材，支持批量上传、移动、删除等操作，并自动提取素材元数据（尺寸、时长等）。

### 核心特性

- **文件夹管理**: 无限层级嵌套的文件夹结构，支持创建、重命名、删除文件夹
- **素材上传**: 支持单文件上传和整文件夹上传，自动保留目录结构
- **批量操作**: 多选素材进行批量移动或删除
- **元数据提取**: 自动提取图片尺寸、视频时长等关键信息
- **素材预览**: 内置图片和视频预览功能
- **用户管理**: 管理员可管理用户账号和权限（ADMIN/USER）
- **分页查询**: 支持分页显示和总数统计

### 技术栈

**后端**:

- Java 17
- Spring Boot 3.x
- Undertow (替代 Tomcat)
- SQLite 数据库
- MyBatis-Plus ORM
- BCrypt 密码加密

**前端**:

- Vue 3 (Composition API)
- Element Plus UI 组件库
- Axios HTTP 客户端
- Vite 构建工具

## 快速开始

### 环境要求

- Java 17
- Node.js 18+
- Maven 3.6+
- Docker 20.10+ (Docker 部署时)

### 开发模式启动

#### 后端

```bash
cd backend

# 使用 Maven 启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或使用脚本
scripts\run-dev.bat   # Windows
./scripts/run-dev.sh  # Linux/Mac
```

后端运行在 `http://localhost:8080`

#### 前端

```bash
cd frontend

npm install
npm run dev
```

前端运行在 `http://localhost:3000`

### Docker 部署

详细部署指南请参阅 [DEPLOYMENT.md](DEPLOYMENT.md)

```bash
# 构建并启动所有服务
docker-compose up -d --build

# 服务地址
# 前端: http://localhost:3001
# 后端 API: http://localhost:8081
```

### 首次登录

打开浏览器访问前端地址，使用默认管理员账号登录：

- **用户名**: `admin`
- **密码**: `admin123`

## 功能操作指南

### 文件夹管理

1. **创建根文件夹**: 点击左侧文件夹面板右上角的「新建」按钮
2. **创建子文件夹**: 右键点击目标文件夹，选择「新建子文件夹」
3. **重命名文件夹**: 右键点击目标文件夹，选择「重命名」
4. **删除文件夹**: 右键点击目标文件夹，选择「删除」（需确保文件夹为空）

### 素材上传

1. **选择目标文件夹**: 在左侧文件夹树中点击目标文件夹
2. **上传文件**: 点击「上传文件」按钮，选择一个或多个文件
3. **上传文件夹**: 点击「上传文件夹」按钮，选择整个文件夹（自动保留目录结构）

**支持的文件格式**:

- 图片: JPG, JPEG, PNG, GIF, WEBP, BMP
- 视频: MP4, AVI, MOV, MKV, WEBM

**上传限制**: 单个文件最大 500MB

### 素材管理

- **预览**: 点击素材行右侧的「预览」按钮
- **下载**: 点击「下载」按钮下载素材文件
- **移动**: 点击「移动」按钮选择目标文件夹，支持批量移动
- **删除**: 点击「删除」按钮，支持批量删除
- **筛选**: 使用日期范围、文件类型、上传者等条件筛选

### 用户管理（仅管理员）

1. **切换视图**: 点击顶部导航栏的「用户管理」标签
2. **新建用户**: 点击「新建用户」按钮
3. **编辑用户**: 点击用户行的「编辑」按钮
4. **重置密码**: 点击用户行的「重置密码」按钮
5. **删除用户**: 点击用户行的「删除」按钮

## 项目结构

```
material_manage_platform/
├── backend/                    # 后端代码
│   ├── src/main/java/
│   │   └── com/material/platform/
│   │       ├── common/        # 公共类
│   │       ├── config/        # 配置类
│   │       ├── controller/    # REST 控制器
│   │       ├── dto/           # 数据传输对象
│   │       ├── entity/        # 数据库实体
│   │       ├── mapper/        # MyBatis Mapper
│   │       ├── metadata/     # 元数据提取器
│   │       ├── service/       # 业务逻辑层
│   │       └── storage/       # 存储实现
│   ├── src/main/resources/
│   │   ├── db/schema.sql      # 数据库建表语句
│   │   └── mapper/            # MyBatis XML
│   └── pom.xml
├── frontend/                   # 前端代码
│   ├── src/
│   │   ├── api/              # API 请求封装
│   │   ├── App.vue           # 主组件
│   │   └── main.js           # 入口文件
│   ├── package.json
│   └── vite.config.js
├── docker-compose.yml          # Docker Compose 配置
├── DEPLOYMENT.md              # 部署指南
└── README.md
```

## 数据库

使用 SQLite 数据库，文件位于 `backend/data/material.db`

### 主要表结构

- **folders**: 文件夹表
- **assets**: 素材表
- **users**: 用户表

## 部署

详细部署步骤请参阅 [DEPLOYMENT.md](DEPLOYMENT.md)

## API 文档

### 认证

所有接口（除登录外）需要在请求头或 Cookie 中携带 token：
```
Cookie: JSESSIONID=xxx
或
Header: Authorization: Bearer <token>
```

### 素材接口 `/api/assets`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/api/assets` | 分页查询素材 | `folderId`, `pageNum`, `pageSize`, `startDate`, `endDate`, `fileType`, `uploadedBy`, `fileName` |
| GET | `/api/assets/{id}` | 获取素材详情 | - |
| GET | `/api/assets/{id}/preview` | 预览素材（浏览器内嵌） | - |
| GET | `/api/assets/{id}/download` | 下载素材文件 | - |
| POST | `/api/assets/upload` | 上传素材 | `file`(file), `folderId`, `relativePath`(可选) |
| POST | `/api/assets/upload-directory` | 批量上传目录 | `files`(file[]), `relativePaths`(string[]), `folderId` |
| PUT | `/api/assets/{id}` | 更新素材信息 | `originalName`, `folderId` |
| DELETE | `/api/assets/{id}` | 删除素材 | - |

### 文件夹接口 `/api/folders`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/api/folders/tree` | 获取文件夹树 | - |
| POST | `/api/folders` | 创建文件夹 | `name`, `parentId`(可选) |
| PUT | `/api/folders/{id}` | 重命名文件夹 | `name` |
| DELETE | `/api/folders/{id}` | 删除文件夹 | - |

### 用户接口 `/api/users`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| GET | `/api/users` | 获取用户列表 | `pageNum`, `pageSize` |
| POST | `/api/users` | 创建用户 | `username`, `password`, `role` |
| PUT | `/api/users/{id}` | 更新用户 | `username`, `role` |
| DELETE | `/api/users/{id}` | 删除用户 | - |
| PUT | `/api/users/{id}/password` | 重置密码 | `newPassword` |

### 认证接口 `/api/auth`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录 | `username`, `password` |
| POST | `/api/auth/logout` | 登出 | - |
| GET | `/api/auth/me` | 获取当前用户信息 | - |

### 下载素材示例

```bash
# 方式1: 浏览器直接下载（通过前端界面点击「下载」按钮）

# 方式2: 使用 curl 命令行下载
curl -L -o output.mp4 \
  "http://localhost:8081/api/assets/1/download" \
  -H "Cookie: JSESSIONID=your_session_id"

# 方式3: 使用 wget 下载
wget -O output.mp4 \
  "http://localhost:8081/api/assets/1/download" \
  --header="Cookie: JSESSIONID=your_session_id"
```

### 在内网集群中获取素材

如果你在内网集群中部署了系统，可以通过以下方式获取素材：

1. **通过前端界面下载**：登录后点击素材行的「下载」按钮
2. **通过 curl 命令行**：
   ```bash
   # 先登录获取 cookie
   curl -c cookies.txt -X POST http://<server-ip>:8081/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   
   # 下载素材
   curl -b cookies.txt -L -o myvideo.mp4 \
     http://<server-ip>:8081/api/assets/1/download
   ```
3. **通过脚本批量下载**：
   ```python
   import requests
   
   # 登录
   session = requests.Session()
   session.post('http://<server-ip>:8081/api/auth/login',
                json={'username': 'admin', 'password': 'admin123'})
   
   # 下载素材
   resp = session.get('http://<server-ip>:8081/api/assets/1/download')
   with open('downloaded_file.mp4', 'wb') as f:
       f.write(resp.content)
   ```
