# 素材管理平台 API 接口文档

## 基础信息

- **基础URL**: `http://192.168.1.168:8081/api`
- **认证方式**: Session 登录认证
- **数据格式**: JSON

## 认证相关

### 登录

```
POST /api/auth/login
```

**请求体**:
```json
{
  "username": "admin",
  "password": "your_password"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "displayName": "系统管理员",
    "role": "ADMIN"
  }
}
```

### 获取当前用户

```
GET /api/auth/current
```

### 退出登录

```
POST /api/auth/logout
```

## 文件夹管理

### 获取文件夹树

```
GET /api/folders/tree
```

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "根文件夹",
      "parentId": 0,
      "createdAt": "2024-01-01T00:00:00",
      "children": []
    }
  ]
}
```

### 获取子文件夹列表

```
GET /api/folders?parentId=0
```

### 创建文件夹

```
POST /api/folders
```

**请求体**:
```json
{
  "name": "新文件夹",
  "parentId": 1
}
```

### 更新文件夹

```
PUT /api/folders/{folderId}
```

**请求体**:
```json
{
  "name": "新名称",
  "parentId": 1
}
```

### 删除文件夹

```
DELETE /api/folders/{folderId}
```

## 素材管理

### 获取素材列表

```
GET /api/assets
```

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| folderId | Long | 否 | 文件夹ID |
| startDate | String | 否 | 开始日期 (YYYY-MM-DD) |
| endDate | String | 否 | 结束日期 (YYYY-MM-DD) |
| fileType | String | 否 | 文件类型 (image/video) |
| uploadedBy | String | 否 | 上传者用户名 |
| fileName | String | 否 | 文件名搜索 |
| pageNum | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页数量，默认 20 |

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 100,
    "pageNum": 1,
    "pageSize": 20,
    "records": [
      {
        "id": 1,
        "publicId": "uuid-string",
        "folderId": 2,
        "originalName": "微博.png",
        "storageKey": "2024/01/01/uuid.png",
        "fileType": "image",
        "fileSize": 44523,
        "uploadDate": "2024-01-01",
        "metadata": "{\"width\":1080,\"height\":1920,\"format\":\"png\"}",
        "uploadedBy": "admin",
        "createdAt": "2024-01-01T12:00:00"
      }
    ]
  }
}
```

**重要说明**: 素材的分辨率和时长信息存储在 `metadata` JSON 字段中，需要解析后使用。

### 上传文件

```
POST /api/assets/upload
```

**Content-Type**: `multipart/form-data`

**表单字段**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 上传的文件 |
| folderId | Long | 是 | 目标文件夹ID |
| relativePath | String | 否 | 相对路径（用于目录上传保持结构） |

### 批量上传目录

```
POST /api/assets/upload-directory
```

**表单字段**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| files | File[] | 是 | 上传的文件列表 |
| relativePaths | String[] | 是 | 相对路径列表 |
| folderId | Long | 是 | 目标文件夹ID |

### 获取单个素材详情

```
GET /api/assets/{assetRef}
```

### 更新素材

```
PUT /api/assets/{assetRef}
```

**请求体**:
```json
{
  "originalName": "新文件名.png",
  "folderId": 3
}
```

### 删除素材

```
DELETE /api/assets/{assetRef}
```

### 重新提取元数据

```
POST /api/assets/{assetRef}/re-extract
```

### 预览素材

```
GET /api/assets/{assetRef}/preview
```

### 下载素材

```
GET /api/assets/{assetRef}/download
```

### 批量下载

```
POST /api/assets/batch-download
```

**请求体**:
```json
{
  "assetRefs": ["id1", "id2", "uuid1", "uuid2"]
}
```

## 素材数据结构

### Asset 对象字段

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 素材ID |
| publicId | String | 公共UUID标识 |
| folderId | Long | 所属文件夹ID |
| originalName | String | 原始文件名 |
| storageKey | String | 存储路径 |
| fileType | String | 文件类型 (image/video/other) |
| fileSize | Long | 文件大小（字节） |
| uploadDate | String | 上传日期 (YYYY-MM-DD) |
| metadata | String | **元数据 JSON 字符串** |
| uploadedBy | String | 上传者用户名 |
| createdAt | String | 创建时间 (ISO 8601) |

### Metadata JSON 结构

**图片元数据**:
```json
{
  "width": 1080,
  "height": 1920,
  "format": "png"
}
```

**视频元数据**:
```json
{
  "width": 1920,
  "height": 1080,
  "format": "mp4",
  "duration": 120.5
}
```

**注意**: 
- `duration` 字段单位为秒（浮点数）
- 如果元数据提取失败，metadata 为空对象 `{}`

## 用户管理（管理员）

### 获取用户列表

```
GET /api/users
```

### 创建用户

```
POST /api/users
```

### 更新用户

```
PUT /api/users/{userId}
```

### 删除用户

```
DELETE /api/users/{userId}
```

### 重置密码

```
POST /api/users/{userId}/reset-password
```

## Python 测试脚本使用示例

```python
import requests
import json

session = requests.Session()

# 登录
login_response = session.post(
    "http://192.168.1.168:8081/api/auth/login",
    json={"username": "admin", "password": "your_password"}
)

# 获取素材列表
assets_response = session.get(
    "http://192.168.1.168:8081/api/assets",
    params={"folderId": 2, "fileType": "image"}
)

# 解析元数据
for asset in assets_response.json()["data"]["records"]:
    metadata = json.loads(asset["metadata"]) if asset["metadata"] != "{}" else {}
    width = metadata.get("width", 0)
    height = metadata.get("height", 0)
    duration = metadata.get("duration", 0)  # 仅视频
    
    print(f"文件名: {asset['originalName']}")
    print(f"分辨率: {width}x{height}")
    print(f"上传时间: {asset['uploadDate']}")
    if duration:
        print(f"时长: {duration}秒")
```
