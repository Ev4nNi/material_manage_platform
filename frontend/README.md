# 素材管理平台 - 前端实现文档

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [安装依赖](#安装依赖)
- [页面布局方案](#页面布局方案)
- [核心代码实现](#核心代码实现)
  - [完整页面模板](#完整页面模板)
  - [文件夹树形组件](#文件夹树形组件)
  - [素材列表表格组件](#素材列表表格组件)
  - [文件上传组件](#文件上传组件)
  - [API 调用封装](#api-调用封装)
- [关键交互说明](#关键交互说明)
- [路由配置](#路由配置)
- [注意事项](#注意事项)

---

## 项目概述

素材管理平台前端采用 Vue 3 + Element Plus 构建，提供文件夹管理、素材上传、素材列表浏览、元数据预览等核心功能。整体采用经典的左右分栏布局，左侧为文件夹树形导航，右侧为素材操作区域。

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x | 核心框架 |
| Element Plus | 2.x | UI 组件库 |
| Axios | 1.x | HTTP 请求库 |
| @element-plus/icons-vue | 最新版 | 图标库 |

---

## 安装依赖

```bash
npm install vue element-plus axios @element-plus/icons-vue
```

---

## 页面布局方案

整体布局使用 Element Plus 的容器组件 `el-container` + `el-aside` + `el-main` 实现：

```
+-----------------------------------------------------------+
|                        Header (可选)                       |
+-------------------+---------------------------------------+
|                   |                                       |
|   el-aside        |            el-main                    |
|   (左侧边栏)      |   +-------------------------------+   |
|                   |   |  文件上传区域 (el-upload)     |   |
|   el-tree         |   +-------------------------------+   |
|   (文件夹树)      |   |                               |   |
|                   |   |  素材列表 (el-table)          |   |
|                   |   |  - 原始文件名                 |   |
|                   |   |  - 文件类型                   |   |
|                   |   |  - 文件大小                   |   |
|                   |   |  - 上传日期                   |   |
|                   |   |  - 元数据预览                 |   |
|                   |   |  - 操作列                     |   |
|                   |   |                               |   |
|                   |   +-------------------------------+   |
+-------------------+---------------------------------------+
```

### 布局特点

- **左侧固定宽度**：`el-aside` 宽度设为 280px，可配置是否可拖拽调整
- **右侧自适应**：`el-main` 占据剩余空间
- **上传区域**：固定在右侧顶部，支持拖拽上传
- **列表区域**：占据剩余高度，支持滚动和分页

---

## 核心代码实现

### 完整页面模板

```vue
<template>
  <el-container class="material-platform">
    <el-header v-if="showHeader" height="60px">
      <div class="header-content">
        <h2>素材管理平台</h2>
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- 左侧文件夹树 -->
      <el-aside width="280px" class="folder-aside">
        <div class="tree-header">
          <el-button type="primary" @click="createRootFolder">
            <el-icon><Plus /></el-icon>
            新建根文件夹
          </el-button>
        </div>
        <el-tree
          ref="treeRef"
          :data="folderTree"
          :props="treeProps"
          :load="loadNode"
          lazy
          highlight-current
          @node-click="handleNodeClick"
          @node-contextmenu="handleContextMenu"
        >
          <template #default="{ node, data }">
            <span class="custom-tree-node">
              <el-icon><Folder /></el-icon>
              <span>{{ node.label }}</span>
            </span>
          </template>
        </el-tree>
      </el-aside>

      <!-- 右侧主内容区 -->
      <el-main class="content-main">
        <!-- 文件上传区域 -->
        <div class="upload-section">
          <el-upload
            v-model:file-list="uploadFileList"
            class="upload-dragger"
            drag
            multiple
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="{ folderId: currentFolderId }"
            :accept="acceptTypes"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :on-progress="handleUploadProgress"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持图片/视频文件，单个文件不超过 500MB
              </div>
            </template>
          </el-upload>
        </div>

        <!-- 筛选工具栏 -->
        <div class="filter-toolbar">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleDateFilter"
          />
          <el-select
            v-model="filterFileType"
            placeholder="文件类型"
            clearable
            @change="handleTypeFilter"
          >
            <el-option label="图片" value="image" />
            <el-option label="视频" value="video" />
          </el-select>
          <el-button @click="resetFilters">重置筛选</el-button>
        </div>

        <!-- 素材列表 -->
        <el-table
          v-loading="tableLoading"
          :data="assetList"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />

          <el-table-column prop="original_name" label="文件名" min-width="180">
            <template #default="{ row }">
              <el-tooltip :content="row.original_name" placement="top">
                <span class="file-name">{{ row.original_name }}</span>
              </el-tooltip>
            </template>
          </el-table-column>

          <el-table-column prop="file_type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.file_type === 'image' ? 'success' : 'warning'">
                {{ row.file_type === 'image' ? '图片' : '视频' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="file_size" label="大小" width="100">
            <template #default="{ row }">
              {{ formatFileSize(row.file_size) }}
            </template>
          </el-table-column>

          <el-table-column prop="upload_date" label="上传日期" width="150">
            <template #default="{ row }">
              {{ formatDate(row.upload_date) }}
            </template>
          </el-table-column>

          <el-table-column label="元数据预览" width="150">
            <template #default="{ row }">
              <div v-if="row.file_type === 'image'" class="image-preview">
                <el-image
                  :src="row.thumbnail_url || row.url"
                  fit="cover"
                  style="width: 60px; height: 60px; border-radius: 4px"
                  :preview-src-list="[row.url]"
                />
                <span v-if="row.metadata?.width" class="meta-info">
                  {{ row.metadata.width }}x{{ row.metadata.height }}
                </span>
              </div>
              <div v-else-if="row.file_type === 'video'" class="video-preview">
                <el-icon size="40"><VideoCamera /></el-icon>
                <span v-if="row.metadata?.duration" class="meta-info">
                  {{ formatDuration(row.metadata.duration) }}
                </span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handlePreview(row)">
                预览
              </el-button>
              <el-button link type="primary" @click="handleMove(row)">
                移动
              </el-button>
              <el-popconfirm
                title="确定要删除该素材吗？"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="totalAssets"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-main>
    </el-container>

    <!-- 右键菜单 -->
    <div
      v-show="contextMenuVisible"
      class="context-menu"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
    >
      <el-card shadow="hover" class="menu-card">
        <div class="menu-item" @click="handleCreateFolder">
          <el-icon><FolderAdd /></el-icon>
          <span>新建文件夹</span>
        </div>
        <div class="menu-item" @click="handleRenameFolder">
          <el-icon><Edit /></el-icon>
          <span>重命名</span>
        </div>
        <el-divider class="menu-divider" />
        <div class="menu-item danger" @click="handleDeleteFolder">
          <el-icon><Delete /></el-icon>
          <span>删除文件夹</span>
        </div>
      </el-card>
    </div>

    <!-- 新建/重命名文件夹对话框 -->
    <el-dialog
      v-model="folderDialogVisible"
      :title="folderDialogTitle"
      width="400px"
    >
      <el-input
        v-model="folderForm.name"
        placeholder="请输入文件夹名称"
        @keyup.enter="submitFolderForm"
      />
      <template #footer>
        <el-button @click="folderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFolderForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 移动素材对话框 -->
    <el-dialog v-model="moveDialogVisible" title="移动到文件夹" width="400px">
      <el-tree-select
        v-model="moveTargetFolderId"
        :data="folderTree"
        :props="treeProps"
        placeholder="选择目标文件夹"
        check-strictly
        :render-after-expand="false"
      />
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMove">确定</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="素材预览" width="800px">
      <div v-if="previewAsset?.file_type === 'image'" class="preview-container">
        <el-image :src="previewAsset.url" fit="contain" style="width: 100%" />
      </div>
      <div v-else-if="previewAsset?.file_type === 'video'" class="preview-container">
        <video :src="previewAsset.url" controls style="width: 100%" />
      </div>
      <div class="preview-meta">
        <p>文件名：{{ previewAsset?.original_name }}</p>
        <p>文件大小：{{ formatFileSize(previewAsset?.file_size) }}</p>
        <p v-if="previewAsset?.metadata?.width">
          分辨率：{{ previewAsset.metadata.width }}x{{ previewAsset.metadata.height }}
        </p>
        <p v-if="previewAsset?.metadata?.duration">
          视频时长：{{ formatDuration(previewAsset.metadata.duration) }}
        </p>
      </div>
    </el-dialog>
  </el-container>
</template>
```

---

### 文件夹树形组件

```vue
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Folder, FolderAdd, Edit, Delete, UploadFilled, VideoCamera } from '@element-plus/icons-vue'
import folderApi from '@/api/folders'
import assetApi from '@/api/assets'

// ==================== 文件夹树相关 ====================

const treeRef = ref(null)
const folderTree = ref([])
const currentFolderId = ref(null)
const currentNode = ref(null)

const treeProps = {
  label: 'name',
  children: 'children',
  isLeaf: 'isLeaf'
}

// 懒加载树节点
const loadNode = async (node, resolve) => {
  if (node.level === 0) {
    // 加载根文件夹
    const { data } = await folderApi.getRootFolders()
    resolve(data.map(item => ({ ...item, isLeaf: false })))
  } else {
    // 加载子文件夹
    const { data } = await folderApi.getChildrenFolders(node.data.id)
    resolve(data.map(item => ({ ...item, isLeaf: false })))
  }
}

// 点击树节点
const handleNodeClick = (data, node) => {
  currentFolderId.value = data.id
  currentNode.value = node
  loadAssets()
}

// 右键菜单
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuNode = ref(null)

const handleContextMenu = (event, data, node) => {
  event.preventDefault()
  contextMenuVisible.value = true
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuNode.value = { data, node }
}

// 点击其他地方关闭右键菜单
const closeContextMenu = () => {
  contextMenuVisible.value = false
}

// 新建根文件夹
const createRootFolder = () => {
  folderDialogVisible.value = true
  folderDialogTitle.value = '新建根文件夹'
  folderForm.value = { name: '', parentId: null }
}

// 新建子文件夹
const handleCreateFolder = () => {
  folderDialogVisible.value = true
  folderDialogTitle.value = '新建文件夹'
  folderForm.value = {
    name: '',
    parentId: contextMenuNode.value.data.id
  }
  closeContextMenu()
}

// 重命名文件夹
const handleRenameFolder = () => {
  folderDialogVisible.value = true
  folderDialogTitle.value = '重命名文件夹'
  folderForm.value = {
    name: contextMenuNode.value.data.name,
    id: contextMenuNode.value.data.id,
    parentId: contextMenuNode.value.data.parent_id
  }
  closeContextMenu()
}

// 删除文件夹
const handleDeleteFolder = async () => {
  try {
    await ElMessageBox.confirm(
      '删除文件夹将同时删除其下所有素材，确定要删除吗？',
      '警告',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await folderApi.deleteFolder(contextMenuNode.value.data.id)
    ElMessage.success('删除成功')
    refreshTree()
  } catch {
    // 用户取消
  }
  closeContextMenu()
}

// 文件夹对话框
const folderDialogVisible = ref(false)
const folderDialogTitle = ref('')
const folderForm = reactive({
  id: null,
  name: '',
  parentId: null
})

// 提交文件夹表单
const submitFolderForm = async () => {
  if (!folderForm.name.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }

  try {
    if (folderForm.id) {
      // 更新
      await folderApi.updateFolder(folderForm.id, { name: folderForm.name })
      ElMessage.success('重命名成功')
    } else {
      // 创建
      await folderApi.createFolder({
        name: folderForm.name,
        parent_id: folderForm.parentId
      })
      ElMessage.success('创建成功')
    }
    folderDialogVisible.value = false
    refreshTree()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 刷新树
const refreshTree = () => {
  treeRef.value?.setCurrentKey(null)
  currentFolderId.value = null
  assetList.value = []
  // 重新加载根节点
  loadNode({ level: 0 }, (data) => {
    folderTree.value = data
  })
}

// 监听全局点击关闭右键菜单
onMounted(() => {
  document.addEventListener('click', closeContextMenu)
})
</script>
```

---

### 素材列表表格组件

```vue
<script setup>
// ==================== 素材列表相关 ====================

const assetList = ref([])
const tableLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalAssets = ref(0)
const selectedAssets = ref([])

// 筛选条件
const dateRange = ref([])
const filterFileType = ref('')

// 加载素材列表
const loadAssets = async () => {
  if (!currentFolderId.value) {
    assetList.value = []
    return
  }

  tableLoading.value = true
  try {
    const params = {
      page: currentPage.value,
      page_size: pageSize.value,
      folder_id: currentFolderId.value
    }

    // 日期筛选
    if (dateRange.value && dateRange.value.length === 2) {
      params.start_date = dateRange.value[0]
      params.end_date = dateRange.value[1]
    }

    // 类型筛选
    if (filterFileType.value) {
      params.file_type = filterFileType.value
    }

    const { data } = await assetApi.getAssets(params)
    assetList.value = data.items
    totalAssets.value = data.total
  } catch (error) {
    ElMessage.error('加载素材列表失败')
  } finally {
    tableLoading.value = false
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化视频时长
const formatDuration = (seconds) => {
  if (!seconds) return '-'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 日期筛选
const handleDateFilter = () => {
  currentPage.value = 1
  loadAssets()
}

// 类型筛选
const handleTypeFilter = () => {
  currentPage.value = 1
  loadAssets()
}

// 重置筛选
const resetFilters = () => {
  dateRange.value = []
  filterFileType.value = ''
  currentPage.value = 1
  loadAssets()
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadAssets()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadAssets()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedAssets.value = selection
}

// 预览素材
const previewDialogVisible = ref(false)
const previewAsset = ref(null)

const handlePreview = (row) => {
  previewAsset.value = row
  previewDialogVisible.value = true
}

// 删除素材
const handleDelete = async (row) => {
  try {
    await assetApi.deleteAsset(row.id)
    ElMessage.success('删除成功')
    loadAssets()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 移动素材
const moveDialogVisible = ref(false)
const moveTargetFolderId = ref(null)
const currentMoveAsset = ref(null)

const handleMove = (row) => {
  currentMoveAsset.value = row
  moveTargetFolderId.value = null
  moveDialogVisible.value = true
}

const submitMove = async () => {
  if (!moveTargetFolderId.value) {
    ElMessage.warning('请选择目标文件夹')
    return
  }
  try {
    await assetApi.moveAsset(currentMoveAsset.value.id, {
      folder_id: moveTargetFolderId.value
    })
    ElMessage.success('移动成功')
    moveDialogVisible.value = false
    loadAssets()
  } catch (error) {
    ElMessage.error('移动失败')
  }
}
</script>
```

---

### 文件上传组件

```vue
<script setup>
// ==================== 文件上传相关 ====================

const uploadFileList = ref([])
const uploadUrl = '/api/assets/upload'
const uploadHeaders = ref({
  Authorization: `Bearer ${localStorage.getItem('token')}`
})

// 允许上传的文件类型
const acceptTypes = 'image/*,video/*'

// 上传前校验
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isVideo = file.type.startsWith('video/')
  
  if (!isImage && !isVideo) {
    ElMessage.error('只能上传图片或视频文件！')
    return false
  }

  const maxSize = 500 * 1024 * 1024 // 500MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 500MB！')
    return false
  }

  if (!currentFolderId.value) {
    ElMessage.warning('请先选择目标文件夹')
    return false
  }

  return true
}

// 上传成功回调
const handleUploadSuccess = (response, file, fileList) => {
  ElMessage.success(`文件 ${file.name} 上传成功`)
  loadAssets()
}

// 上传失败回调
const handleUploadError = (error, file, fileList) => {
  ElMessage.error(`文件 ${file.name} 上传失败：${error.message || '未知错误'}`)
}

// 上传进度回调
const handleUploadProgress = (event, file, fileList) => {
  // 可用于自定义进度条展示
  const percent = Math.round(event.percent)
  console.log(`上传进度：${percent}%`)
}
</script>
```

---

### API 调用封装

#### Axios 实例配置

```javascript
// src/utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限执行此操作')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default service
```

#### 文件夹 API

```javascript
// src/api/folders.js
import request from '@/utils/request'

export default {
  // 获取根文件夹列表
  getRootFolders() {
    return request({
      url: '/folders/root',
      method: 'get'
    })
  },

  // 获取子文件夹列表
  getChildrenFolders(parentId) {
    return request({
      url: `/folders/${parentId}/children`,
      method: 'get'
    })
  },

  // 创建文件夹
  createFolder(data) {
    return request({
      url: '/folders',
      method: 'post',
      data
    })
  },

  // 更新文件夹
  updateFolder(id, data) {
    return request({
      url: `/folders/${id}`,
      method: 'put',
      data
    })
  },

  // 删除文件夹
  deleteFolder(id) {
    return request({
      url: `/folders/${id}`,
      method: 'delete'
    })
  },

  // 获取文件夹详情
  getFolderDetail(id) {
    return request({
      url: `/folders/${id}`,
      method: 'get'
    })
  }
}
```

#### 素材 API

```javascript
// src/api/assets.js
import request from '@/utils/request'

export default {
  // 获取素材列表
  getAssets(params) {
    return request({
      url: '/assets',
      method: 'get',
      params
    })
  },

  // 上传素材
  uploadAsset(formData, onUploadProgress) {
    return request({
      url: '/assets/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress
    })
  },

  // 删除素材
  deleteAsset(id) {
    return request({
      url: `/assets/${id}`,
      method: 'delete'
    })
  },

  // 移动素材
  moveAsset(id, data) {
    return request({
      url: `/assets/${id}/move`,
      method: 'put',
      data
    })
  },

  // 批量删除素材
  batchDeleteAssets(ids) {
    return request({
      url: '/assets/batch-delete',
      method: 'post',
      data: { ids }
    })
  },

  // 获取素材详情
  getAssetDetail(id) {
    return request({
      url: `/assets/${id}`,
      method: 'get'
    })
  }
}
```

---

## 关键交互说明

### 1. 点击树节点加载对应文件夹素材

```
用户点击文件夹树节点
  ↓
触发 @node-click 事件
  ↓
记录 currentFolderId
  ↓
调用 loadAssets() 函数
  ↓
向 API 发送请求：GET /api/assets?folder_id={id}&page=1&page_size=20
  ↓
更新 el-table 数据源
```

**实现要点**：
- 使用 `highlight-current` 属性高亮当前选中节点
- 切换文件夹时重置分页到第一页
- 未选择文件夹时显示提示信息

### 2. 拖拽上传素材

```
用户拖拽文件到上传区域
  ↓
el-upload 自动捕获文件
  ↓
触发 beforeUpload 校验
  ↓
校验通过则自动上传到 /api/assets/upload
  ↓
上传成功后触发 on-success 回调
  ↓
刷新当前文件夹的素材列表
```

**实现要点**：
- `drag` 属性启用拖拽模式
- `accept` 限制文件类型为 `image/*,video/*`
- `data` 属性传递当前文件夹 ID
- 上传前必须选择目标文件夹
- 支持多文件同时上传

### 3. 右键菜单（新建文件夹、重命名、删除）

```
用户在树节点上右键
  ↓
触发 @node-contextmenu 事件
  ↓
阻止默认右键菜单 (event.preventDefault)
  ↓
记录右键坐标 (clientX, clientY)
  ↓
显示自定义右键菜单
  ↓
用户选择操作：
  ├─ 新建文件夹 → 弹出对话框 → 调用 createFolder API
  ├─ 重命名 → 弹出对话框 → 调用 updateFolder API
  └─ 删除 → 确认对话框 → 调用 deleteFolder API
```

**实现要点**：
- 菜单位置根据鼠标坐标动态定位
- 点击页面其他区域自动关闭菜单
- 删除操作需要二次确认
- 操作完成后刷新树结构

### 4. 元数据展示

**图片元数据**：
- 显示缩略图预览（60x60 缩略图，支持点击放大）
- 显示分辨率信息（宽 x 高）
- 后端返回格式示例：
  ```json
  {
    "metadata": {
      "width": 1920,
      "height": 1080,
      "format": "jpeg"
    }
  }
  ```

**视频元数据**：
- 显示视频图标
- 显示视频时长（格式化为 MM:SS）
- 后端返回格式示例：
  ```json
  {
    "metadata": {
      "duration": 125.5,
      "codec": "h264",
      "resolution": "1920x1080"
    }
  }
  ```

### 5. 日期范围筛选功能

```
用户选择日期范围
  ↓
触发 @change 事件
  ↓
重置当前页码为 1
  ↓
调用 loadAssets() 并传递 start_date / end_date 参数
  ↓
API 返回过滤后的结果
  ↓
更新表格显示
```

**实现要点**：
- 使用 `el-date-picker` 的 `daterange` 类型
- 日期格式化为 ISO 8601 格式传递给后端
- 支持与其他筛选条件组合使用
- 提供重置筛选功能

---

## 路由配置

```javascript
// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/material',
    name: 'MaterialPlatform',
    component: () => import('@/views/MaterialPlatform.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

---

## 样式补充

```vue
<style scoped>
.material-platform {
  height: 100vh;
  background-color: #f5f7fa;
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.main-container {
  height: calc(100vh - 60px);
}

.folder-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  padding: 16px;
  overflow-y: auto;
}

.tree-header {
  margin-bottom: 16px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-main {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.upload-dragger {
  width: 100%;
}

.filter-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.context-menu {
  position: fixed;
  z-index: 9999;
}

.menu-card {
  padding: 8px 0;
  min-width: 160px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f5f7fa;
}

.menu-item.danger {
  color: #f56c6c;
}

.menu-divider {
  margin: 4px 0;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-preview,
.video-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.meta-info {
  font-size: 12px;
  color: #909399;
}

.preview-container {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.preview-meta {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.preview-meta p {
  margin: 8px 0;
  color: #606266;
}
</style>
```

---

## 注意事项

1. **权限验证**：所有 API 请求需携带 JWT Token，在 axios 拦截器中统一处理
2. **大文件上传**：超过 100MB 的文件建议实现分片上传逻辑
3. **树形组件性能**：使用懒加载模式（`lazy` + `load`），避免一次性加载全部数据
4. **图片预览**：使用 `el-image` 的 `preview-src-list` 实现点击放大功能
5. **响应式适配**：建议在小屏幕设备上将左右布局切换为上下布局
6. **错误处理**：所有异步操作必须包含 try-catch 并给出用户友好的错误提示
7. **内存管理**：组件卸载时移除全局事件监听器（如 `document.addEventListener`）

---

## 后端 API 接口约定

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取根文件夹 | GET | `/api/folders/root` | 返回根文件夹列表 |
| 获取子文件夹 | GET | `/api/folders/{id}/children` | 返回指定父文件夹的子文件夹 |
| 创建文件夹 | POST | `/api/folders` | Body: `{ name, parent_id }` |
| 更新文件夹 | PUT | `/api/folders/{id}` | Body: `{ name }` |
| 删除文件夹 | DELETE | `/api/folders/{id}` | 级联删除子文件夹和素材 |
| 获取素材列表 | GET | `/api/assets` | Query: `folder_id, page, page_size, file_type, start_date, end_date` |
| 上传素材 | POST | `/api/assets/upload` | Multipart: `file, folder_id` |
| 删除素材 | DELETE | `/api/assets/{id}` | - |
| 移动素材 | PUT | `/api/assets/{id}/move` | Body: `{ folder_id }` |
| 批量删除 | POST | `/api/assets/batch-delete` | Body: `{ ids: [] }` |

---

*本文档基于 Vue 3 Composition API + Element Plus 2.x 编写，适用于素材管理平台前端核心功能开发参考。*
