# 素材管理平台

轻量级图片和视频素材管理平台，支持文件夹组织、批量操作、元数据提取、预览和下载。

## 系统简介

本项目基于 Spring Boot 3.x 和 Vue 3 构建，用于管理图片、视频等素材文件。系统提供目录树组织、单文件和整目录上传、批量移动与删除、素材筛选、元数据提取、预览和下载等能力。

## 核心能力

- 文件夹管理：支持多级目录、创建、重命名、删除。
- 素材上传：支持单文件上传和整目录上传，保留相对路径。
- 批量操作：支持批量移动、批量删除、批量下载。
- 元数据提取：自动提取图片尺寸、视频基础信息。
- 在线预览：支持图片和视频直接预览。
- 用户管理：支持管理员和普通用户。

## 技术栈

**后端**

- Java 17
- Spring Boot 3.x
- Undertow
- MyBatis-Plus
- SQLite

**前端**

- Vue 3
- Element Plus
- Axios
- Vite

## 快速开始

### 环境要求

- Java 17
- Maven 3.6+
- Node.js 18+

### 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或使用脚本：

```bash
scripts\run-dev.bat
```

后端默认地址：`http://localhost:8080`

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:3000`

### Docker 部署

详细说明见 [DEPLOYMENT.md](DEPLOYMENT.md)。

```bash
docker-compose up -d --build
```

默认地址：

- 前端：`http://localhost:3001`
- 后端 API：`http://localhost:8081`

### 默认账号

- 用户名：`admin`
- 密码：`admin123`

## 素材标识说明

素材表同时存在两类标识：

- `id`：数据库内部自增主键。
- `publicId`：对外使用的随机公开标识，上传时自动生成。

接口兼容规则：

- 新接口和前端页面优先使用 `publicId`。
- 历史数据和旧调用仍兼容数字 `id`。
- 文档中的 `{assetRef}` 表示可传 `publicId`，也可传旧的数字 `id`。

推荐：

- 对外链接、下载链接、预览链接、详情接口统一使用 `publicId`。
- 不要把数据库自增 `id` 暴露成长期稳定的公开资源地址。

## 常用操作

### 上传素材

1. 在左侧目录树中选择目标文件夹。
2. 点击“上传文件”选择单个或多个文件。
3. 点击“上传文件夹”可整目录导入，系统会保留原始目录结构。

支持格式：

- 图片：`jpg` `jpeg` `png` `gif` `webp` `bmp`
- 视频：`mp4` `avi` `mov` `mkv` `webm`

默认限制：

- 单文件最大 `500MB`

### 管理素材

- 预览：在线查看图片或视频。
- 下载：直接下载原文件。
- 移动：可单个或批量移动到其他文件夹。
- 删除：可单个或批量删除。
- 筛选：支持按日期、类型、上传者、文件名筛选。

## 目录结构

```text
material_manage_platform/
├── backend/
│   ├── src/main/java/com/material/platform/
│   │   ├── common/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── mapper/
│   │   ├── metadata/
│   │   ├── service/
│   │   └── storage/
│   ├── src/main/resources/
│   │   ├── db/schema.sql
│   │   └── mapper/
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── docker-compose.yml
├── DEPLOYMENT.md
└── README.md
```

## 数据库

默认使用 SQLite，数据库文件位于：

`backend/data/material.db`

主要表：

- `folders`：文件夹表
- `assets`：素材表
- `users`：用户表

`assets` 关键字段：

- `id`：自增主键
- `public_id`：对外公开标识，唯一
- `storage_key`：存储路径键，唯一
- `uploaded_by`：上传者

## 认证方式

当前后端使用服务端 Session 认证，不是 Bearer Token 模式。

认证流程：

1. 调用 `/api/auth/login`
2. 服务端在 Session 中写入登录用户
3. 后续请求通过 `JSESSIONID` Cookie 维持登录态

除 `/api/auth/login` 外，其余 `/api/**` 接口都要求已登录。

请求示例：

```text
Cookie: JSESSIONID=xxx
```

前端浏览器场景下，登录成功后会自动携带 Cookie，无需手动追加 `Authorization` 头。

## API 文档

### 通用返回结构

除文件预览和文件下载接口外，其余接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页列表返回结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

### 素材接口 `/api/assets`

| 方法 | 路径 | 说明 | 参数 / 请求体 |
|------|------|------|------|
| GET | `/api/assets` | 分页查询素材 | Query: `folderId` 或 `startDate + endDate`，以及 `pageNum`, `pageSize`, `fileType`, `uploadedBy`, `fileName` |
| GET | `/api/assets/{assetRef}` | 获取素材详情 | `assetRef` 推荐传 `publicId` |
| GET | `/api/assets/{assetRef}/preview` | 预览素材 | 返回文件流 |
| GET | `/api/assets/{assetRef}/download` | 下载素材 | 返回文件流 |
| POST | `/api/assets/upload` | 上传单个素材 | Multipart: `file`, `folderId`, `relativePath`(可选) |
| POST | `/api/assets/upload-directory` | 批量上传目录 | Multipart: `files`, `relativePaths`, `folderId` |
| PUT | `/api/assets/{assetRef}` | 更新素材信息 | JSON: `{ "originalName": "...", "folderId": 1 }`，字段均可选 |
| DELETE | `/api/assets/{assetRef}` | 删除素材 | - |
| POST | `/api/assets/{assetRef}/re-extract` | 重新提取元数据 | - |
| POST | `/api/assets/batch-download` | 生成批量下载地址 | JSON: `{ "assetRefs": ["public-id-1", "public-id-2"] }`，兼容旧字段 `assetIds` |

说明：

- `{assetRef}` 推荐传 `publicId`
- 旧的数字 `id` 仍兼容，但不建议继续作为公开链接使用
- `batch-download` 返回的是下载 URL 列表，不直接返回压缩包

### 文件夹接口 `/api/folders`

| 方法 | 路径 | 说明 | 参数 / 请求体 |
|------|------|------|------|
| GET | `/api/folders` | 获取指定父目录下的直接子文件夹 | Query: `parentId`，默认 `0` |
| GET | `/api/folders/tree` | 获取完整文件夹树 | - |
| POST | `/api/folders` | 创建文件夹 | JSON: `{ "name": "...", "parentId": 0 }`，`parentId` 可省略 |
| PUT | `/api/folders/{id}` | 更新文件夹 | JSON: `{ "name": "...", "parentId": 0 }`，`parentId` 可选 |
| DELETE | `/api/folders/{id}` | 删除文件夹 | - |

### 用户接口 `/api/users`

以下接口均要求管理员权限。

| 方法 | 路径 | 说明 | 参数 / 请求体 |
|------|------|------|------|
| GET | `/api/users` | 获取用户列表 | - |
| POST | `/api/users` | 创建用户 | JSON: `{ "username": "...", "displayName": "...", "password": "...", "role": "ADMIN|USER" }` |
| PUT | `/api/users/{id}` | 更新用户 | JSON: `{ "displayName": "...", "role": "ADMIN|USER" }` |
| PUT | `/api/users/{id}/password` | 重置密码 | JSON: `{ "password": "..." }` |
| DELETE | `/api/users/{id}` | 删除用户 | - |

### 认证接口 `/api/auth`

| 方法 | 路径 | 说明 | 参数 / 请求体 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录并建立 Session | JSON: `{ "username": "...", "password": "..." }` |
| POST | `/api/auth/logout` | 登出并销毁 Session | - |
| GET | `/api/auth/me` | 获取当前登录用户 | - |

## 下载示例

### 浏览器下载

登录后直接在前端页面点击“下载”按钮。

### curl 下载

推荐使用 `publicId`：

```bash
curl -L -o output.mp4 \
  "http://localhost:8081/api/assets/<asset-public-id>/download" \
  -H "Cookie: JSESSIONID=your_session_id"
```

历史数字 `id` 仍兼容：

```bash
curl -L -o output.mp4 \
  "http://localhost:8081/api/assets/1/download" \
  -H "Cookie: JSESSIONID=your_session_id"
```

### Python 脚本下载

```python
import requests

session = requests.Session()
session.post(
    "http://<server-ip>:8081/api/auth/login",
    json={"username": "admin", "password": "admin123"},
)

resp = session.get("http://<server-ip>:8081/api/assets/<asset-public-id>/download")
with open("downloaded_file.mp4", "wb") as f:
    f.write(resp.content)
```

## 说明

- 根目录文档以当前后端实现为准。
- 前端历史设计文档见 `frontend/README.md`，其中部分接口示例可能早于当前实现。
