# 素材管理平台前端

前端基于 Vue 3、Element Plus 和 Vite，实现登录、素材管理、目录管理和管理员用户管理。

## 技术栈

- Vue 3
- Element Plus
- Axios
- Vite

当前依赖版本见 [package.json](D:/programProject/workproject/material_manage_platform/frontend/package.json)。

## 启动方式

```bash
cd frontend
npm install
npm run dev
```

默认开发地址：`http://localhost:3000`

Vite 会将 `/api` 代理到 `http://localhost:8080`，配置见 [vite.config.js](D:/programProject/workproject/material_manage_platform/frontend/vite.config.js)。

构建命令：

```bash
npm run build
```

## 当前实现概览

当前前端不是多页面路由应用，而是以 [App.vue](D:/programProject/workproject/material_manage_platform/frontend/src/App.vue) 为核心的单页控制台。

界面分为三块：

- 登录态初始化和登录界面
- 素材中心：文件夹树、筛选、上传、预览、移动、删除、批量下载
- 用户管理：仅管理员可见

入口文件见 [main.js](D:/programProject/workproject/material_manage_platform/frontend/src/main.js)。

## 目录说明

```text
frontend/
├── src/
│   ├── api/
│   │   ├── request.js
│   │   ├── auth.js
│   │   ├── folders.js
│   │   ├── assets.js
│   │   └── users.js
│   ├── App.vue
│   └── main.js
├── package.json
└── vite.config.js
```

## 关键交互

### 1. 登录与会话恢复

- 页面加载时调用 `authApi.getCurrentUser()` 检查会话。
- 已登录则加载文件夹树和用户数据。
- 401 会在 [request.js](D:/programProject/workproject/material_manage_platform/frontend/src/api/request.js) 中派发 `auth-expired` 事件，`App.vue` 监听后重置界面状态。

### 2. 文件夹树

- 通过 `folderApi.getTree()` 获取完整目录树。
- 当前选中文件夹变化后，重新请求素材列表。
- 支持右键创建子文件夹、重命名、删除。

### 3. 素材列表

- 素材列表按 `folderId + pageNum + pageSize` 分页加载。
- 支持日期范围、文件类型、上传者、文件名过滤。
- 列表字段与后端当前返回结构一致，使用驼峰命名，例如 `originalName`、`fileType`、`uploadedBy`。

### 4. 上传

- 上传入口不是旧版文档里的 `el-upload action` 直传模式，而是通过原生 `input[type=file]` 选择文件。
- 单文件上传调用 `assetApi.uploadAsset(formData)`。
- 目录上传同样逐文件调用上传接口，并通过 `relativePath` 保留目录结构。

### 5. 预览、下载、删除、移动

素材相关操作统一通过 `assetRef(asset)` 获取素材引用：

```js
function assetRef(asset) {
  return asset?.publicId || asset?.id
}
```

规则如下：

- 优先使用 `publicId`
- 老数据回退到数字 `id`
- 预览、下载、详情、删除、移动、重新提取、批量下载都应使用这套规则

## API 封装

### 请求层

[request.js](D:/programProject/workproject/material_manage_platform/frontend/src/api/request.js)

- `baseURL` 为 `/api`
- 统一处理业务返回 `code`
- 401 时触发 `auth-expired`
- 默认展示接口错误消息
- 当前前端依赖浏览器自动携带 `JSESSIONID` Cookie，不主动注入 `Authorization: Bearer ...`

### 认证接口

[auth.js](D:/programProject/workproject/material_manage_platform/frontend/src/api/auth.js)

- `login(data)`
- `logout()`
- `getCurrentUser()`

### 文件夹接口

[folders.js](D:/programProject/workproject/material_manage_platform/frontend/src/api/folders.js)

- `getTree()`
- `getRootFolders()`
- `getChildrenFolders(parentId)`
- `createFolder(data)`
- `updateFolder(id, data)`
- `deleteFolder(id)`
- `getFolderDetail(id)`

### 素材接口

[assets.js](D:/programProject/workproject/material_manage_platform/frontend/src/api/assets.js)

- `getAssets(params)`
- `uploadAsset(formData, onUploadProgress)`
- `uploadDirectory(formData, onUploadProgress)`
- `deleteAsset(assetRef)`
- `updateAsset(assetRef, data)`
- `getAssetDetail(assetRef)`
- `reExtractMetadata(assetRef)`
- `batchDownload(assetRefs)`

说明：

- 旧文档中的 `moveAsset(id, data)`、`batchDeleteAssets(ids)`、`/assets/{id}/move`、`/assets/batch-delete` 已不属于当前实现。
- 当前移动素材通过 `updateAsset(assetRef, { folderId })` 完成。
- 当前批量删除由前端循环调用 `deleteAsset(assetRef)` 完成。

### 用户接口

[users.js](D:/programProject/workproject/material_manage_platform/frontend/src/api/users.js)

- `listUsers()`
- `createUser(data)`
- `updateUser(id, data)`
- `resetPassword(id, password)`
- `deleteUser(id)`

## 后端接口约定

前端当前实际依赖的接口如下：

| 模块 | 方法 | 路径 | 说明 | 前端实际发送的参数 / 请求体 |
|------|------|------|------|------|
| 认证 | POST | `/api/auth/login` | 登录 | `{ username, password }` |
| 认证 | POST | `/api/auth/logout` | 登出 | - |
| 认证 | GET | `/api/auth/me` | 获取当前用户 | - |
| 文件夹 | GET | `/api/folders/tree` | 获取完整文件夹树 | - |
| 文件夹 | GET | `/api/folders` | 查询某一级子文件夹 | Query: `parentId` |
| 文件夹 | POST | `/api/folders` | 创建文件夹 | `{ name, parentId }` |
| 文件夹 | PUT | `/api/folders/{id}` | 更新文件夹 | `{ name, parentId }` |
| 文件夹 | DELETE | `/api/folders/{id}` | 删除文件夹 | - |
| 素材 | GET | `/api/assets` | 分页查询素材 | Query: `folderId`, `pageNum`, `pageSize`, `startDate`, `endDate`, `fileType`, `uploadedBy`, `fileName` |
| 素材 | GET | `/api/assets/{assetRef}` | 素材详情 | `assetRef` 优先使用 `publicId` |
| 素材 | GET | `/api/assets/{assetRef}/preview` | 预览 | `assetRef` 优先使用 `publicId` |
| 素材 | GET | `/api/assets/{assetRef}/download` | 下载 | `assetRef` 优先使用 `publicId` |
| 素材 | POST | `/api/assets/upload` | 上传单个素材 | Multipart: `file`, `folderId`, `relativePath`(可选) |
| 素材 | POST | `/api/assets/upload-directory` | 批量目录上传接口 | Multipart: `files`, `relativePaths`, `folderId` |
| 素材 | PUT | `/api/assets/{assetRef}` | 更新素材信息，例如移动文件夹 | `{ originalName?, folderId? }` |
| 素材 | DELETE | `/api/assets/{assetRef}` | 删除素材 | - |
| 素材 | POST | `/api/assets/{assetRef}/re-extract` | 重新提取元数据 | - |
| 素材 | POST | `/api/assets/batch-download` | 生成批量下载地址 | `{ assetRefs: [] }` |
| 用户 | GET | `/api/users` | 用户列表，仅管理员可用 | - |
| 用户 | POST | `/api/users` | 创建用户，仅管理员可用 | `{ username, displayName, password, role }` |
| 用户 | PUT | `/api/users/{id}` | 更新用户，仅管理员可用 | `{ displayName, role }` |
| 用户 | PUT | `/api/users/{id}/password` | 重置密码，仅管理员可用 | `{ password }` |
| 用户 | DELETE | `/api/users/{id}` | 删除用户，仅管理员可用 | - |

补充说明：

- 后端当前采用 Session 认证，前端依赖浏览器 Cookie 维持登录态
- `assetRef` 的规则是 `publicId || id`
- `POST /api/assets/batch-download` 当前兼容后端旧字段 `assetIds`，但前端统一发送 `assetRefs`

## 与根文档的关系

- 根目录 [README.md](D:/programProject/workproject/material_manage_platform/README.md) 说明的是整个系统。
- 本文件只描述前端实现和前端实际依赖的接口。
- 若根文档与本文件冲突，以当前代码实现为准，并优先核对 [App.vue](D:/programProject/workproject/material_manage_platform/frontend/src/App.vue) 和 `src/api` 下的封装。
