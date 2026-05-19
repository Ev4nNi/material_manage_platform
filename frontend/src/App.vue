<template>
  <div class="app-shell">
    <div v-if="!appReady" class="boot-screen">
      <el-card class="boot-card" shadow="never">
        <el-skeleton :rows="5" animated />
      </el-card>
    </div>

    <div v-else-if="!loginUser" class="login-screen">
      <div class="login-layout">
        <section class="brand-panel">
          <p class="panel-kicker">Material Command Center</p>
          <h1>素材管理平台</h1>
          <p class="brand-copy">
            用更清晰的目录结构管理图片和视频素材，支持整目录导入、批量移动和管理员用户管理。
          </p>
          <div class="brand-badges">
            <span>目录上传</span>
            <span>素材预览</span>
            <span>用户管理</span>
          </div>
        </section>

        <el-card class="login-card" shadow="hover">
          <template #header>
            <div class="login-card-header">
              <span>用户登录</span>
              <small>进入控制台</small>
            </div>
          </template>

          <el-form label-position="top" @submit.prevent="handleLogin">
            <el-form-item label="用户名">
              <el-input
                v-model="loginForm.username"
                size="large"
                placeholder="请输入用户名"
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="loginForm.password"
                size="large"
                type="password"
                show-password
                placeholder="请输入密码"
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-button type="primary" size="large" class="stretch-button" :loading="loginLoading" @click="handleLogin">
              登录
            </el-button>
          </el-form>
        </el-card>
      </div>
    </div>

    <div v-else class="console-shell">
      <header class="topbar">
        <div>
          <p class="panel-kicker topbar-kicker">Asset Workspace</p>
          <h2>素材管理平台</h2>
        </div>
        <div class="topbar-actions">
          <div v-if="isAdmin" class="view-switcher">
            <button :class="['switch-chip', { active: activeView === 'assets' }]" @click="switchView('assets')">
              素材中心
            </button>
            <button :class="['switch-chip', { active: activeView === 'users' }]" @click="switchView('users')">
              用户管理
            </button>
          </div>
          <el-tag class="user-chip" effect="dark">{{ loginUser.displayName }} / {{ roleLabel(loginUser.role) }}</el-tag>
          <el-button @click="refreshCurrentView">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="danger" plain @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </header>

      <div v-if="activeView === 'assets'" class="asset-layout">
        <aside class="folder-panel">
          <div class="folder-panel-header">
            <div>
              <p class="small-label">Folder Tree</p>
              <h3>{{ folderPanelTitle }}</h3>
            </div>
            <div class="folder-panel-actions">
              <el-select v-model="folderSortBy" class="folder-sort-select" @change="handleFolderSortChange">
                <el-option :label="folderSortNameLabel" value="name" />
                <el-option :label="folderSortCreatedAtLabel" value="createdAt" />
                <el-option :label="folderSortUpdatedAtLabel" value="updatedAt" />
              </el-select>
              <el-button class="folder-sort-toggle" @click="toggleFolderSortOrder">
                <el-icon>
                  <component :is="folderSortOrder === 'asc' ? SortUp : SortDown" />
                </el-icon>
              </el-button>
              <el-button type="primary" @click="createRootFolder">
                <el-icon><Plus /></el-icon>
                {{ createFolderButtonLabel }}
              </el-button>
            </div>
          </div>

          <el-empty v-if="folderTree.length === 0" description="暂无文件夹">
            <el-button type="primary" @click="createRootFolder">创建第一个文件夹</el-button>
          </el-empty>

          <el-tree
            v-else
            ref="treeRef"
            :data="folderTree"
            :props="treeProps"
            node-key="id"
            highlight-current
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
            @node-contextmenu="handleContextMenu"
            @node-expand="handleFolderNodeExpand"
            @node-collapse="handleFolderNodeCollapse"
          >
            <template #default="{ node }">
              <div class="folder-node">
                <el-icon><Folder /></el-icon>
                <span>{{ node.label }}</span>
              </div>
            </template>
          </el-tree>
        </aside>

        <main class="asset-main">
          <section class="hero-card">
            <div class="hero-copy">
              <p class="small-label">Current Workspace</p>
              <h3>{{ currentFolderName || '请选择文件夹' }}</h3>
              <p>
                目录上传会保留原始层级结构，系统会在当前文件夹下自动创建对应子目录。
              </p>
              <div class="hero-stats">
                <div class="stat-box">
                  <span>当前素材</span>
                  <strong>{{ assetList.length }}</strong>
                </div>
                <div class="stat-box">
                  <span>已选项目</span>
                  <strong>{{ selectedAssets.length }}</strong>
                </div>
                <div class="stat-box">
                  <span>上传限制</span>
                  <strong>500MB / 文件</strong>
                </div>
              </div>
              <p v-if="uploadProgressText" class="upload-progress">{{ uploadProgressText }}</p>
            </div>
            <div class="hero-actions">
              <el-button type="primary" size="large" :disabled="!currentFolderId || uploading" @click="pickFiles">
                <el-icon><UploadFilled /></el-icon>
                上传文件
              </el-button>
              <el-button size="large" :disabled="!currentFolderId || uploading" @click="pickDirectory">
                <el-icon><FolderOpened /></el-icon>
                上传文件夹
              </el-button>
            </div>
          </section>

          <section v-if="selectedAssets.length > 0" class="selection-card">
            <span>已选择 {{ selectedAssets.length }} 项素材</span>
            <div class="selection-actions">
              <el-button type="primary" @click="batchDownload">
                <el-icon><Download /></el-icon>
                批量下载
              </el-button>
              <el-button type="primary" @click="batchMove">
                <el-icon><Rank /></el-icon>
                批量移动
              </el-button>
              <el-button type="danger" @click="batchDelete">
                <el-icon><Delete /></el-icon>
                批量删除
              </el-button>
              <el-button @click="clearSelection">清空选择</el-button>
            </div>
          </section>

          <section class="toolbar-card">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="loadAssets"
            />
            <el-select v-model="filterFileType" placeholder="文件类型" clearable style="width: 140px" @change="loadAssets">
              <el-option label="图片" value="image" />
              <el-option label="视频" value="video" />
            </el-select>
            <el-select v-model="filterUploadedBy" placeholder="上传者" clearable style="width: 140px" @change="loadAssets">
              <el-option
                v-for="user in allUsers"
                :key="user.username"
                :label="user.displayName"
                :value="user.username"
              />
            </el-select>
            <el-input
              v-model="filterFileName"
              placeholder="搜索文件名"
              clearable
              style="width: 180px"
              @keyup.enter="loadAssets"
              @clear="loadAssets"
            />
            <el-button @click="resetFilters">重置筛选</el-button>
          </section>

          <section class="table-card">
            <el-table
              ref="tableRef"
              v-loading="tableLoading"
              :data="assetList"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="52" />
              <el-table-column label="预览" width="120">
                <template #default="{ row }">
                  <div
                    class="asset-preview-thumb"
                    role="button"
                    tabindex="0"
                    @click="handlePreview(row)"
                    @keyup.enter="handlePreview(row)"
                  >
                    <img
                      v-if="isImage(row.fileType) && visiblePreviewRefs.has(row.id)"
                      :src="previewUrl(row)"
                      :alt="row.originalName"
                      class="asset-preview-media"
                      loading="lazy"
                    />
                    <div
                      v-else-if="isImage(row.fileType) && !visiblePreviewRefs.has(row.id)"
                      class="asset-preview-thumb"
                    >
                      <span class="asset-preview-placeholder">加载中...</span>
                    </div>
                    <video
                      v-else-if="isVideo(row.fileType)"
                      :src="previewUrl(row)"
                      class="asset-preview-media"
                      muted
                      playsinline
                      preload="metadata"
                    />
                    <span v-else class="asset-preview-placeholder">{{ fileTypeLabel(row.fileType) }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="originalName" label="文件名" min-width="220">
                <template #default="{ row }">
                  <el-tooltip :content="row.originalName" placement="top">
                    <span class="ellipsis-text">{{ row.originalName }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="fileType" label="类型" width="110">
                <template #default="{ row }">
                  <el-tag :type="fileTypeTag(row.fileType)" effect="plain">{{ fileTypeLabel(row.fileType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="fileSize" label="大小" width="130">
                <template #default="{ row }">
                  {{ formatFileSize(row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column prop="uploadDate" label="上传日期" width="120" />
              <el-table-column prop="uploadedBy" label="上传者" width="120">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain">{{ getUploaderDisplayName(row.uploadedBy) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="元数据" min-width="200">
                <template #default="{ row }">
                  <span class="meta-text">{{ parseMetadata(row.metadata) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
                  <el-button link type="primary" @click="handleMove(row)">移动</el-button>
                  <el-popconfirm title="确定删除该素材吗？" @confirm="handleDelete(row)">
                    <template #reference>
                      <el-button link type="danger">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="pagination.pageNum"
                v-model:page-size="pagination.pageSize"
                :page-sizes="[10, 20, 50, 100, 200, 500]"
                :total="pagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handlePageSizeChange"
                @current-change="handlePageChange"
              />
            </div>
          </section>
        </main>
      </div>

      <div v-else class="user-page">
        <section class="user-hero">
          <div>
            <p class="small-label">Administration</p>
            <h3>用户管理</h3>
            <p>管理员可以新增用户、分配角色、重置密码和删除账号。</p>
          </div>
          <div class="hero-stats">
            <div class="stat-box slim">
              <span>用户总数</span>
              <strong>{{ userList.length }}</strong>
            </div>
            <div class="stat-box slim">
              <span>管理员</span>
              <strong>{{ adminCount }}</strong>
            </div>
            <el-button type="primary" size="large" @click="openCreateUserDialog">
              <el-icon><UserFilled /></el-icon>
              新建用户
            </el-button>
          </div>
        </section>

        <section class="table-card user-table-card">
          <el-table v-loading="usersLoading" :data="userList" height="100%">
            <el-table-column prop="username" label="用户名" min-width="180" />
            <el-table-column prop="displayName" label="显示名称" min-width="180" />
            <el-table-column prop="role" label="角色" width="120">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" effect="plain">
                  {{ roleLabel(row.role) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEditUserDialog(row)">编辑</el-button>
                <el-button link type="primary" @click="openResetPasswordDialog(row)">重置密码</el-button>
                <el-popconfirm title="确定删除该用户吗？" @confirm="handleDeleteUser(row)">
                  <template #reference>
                    <el-button link type="danger" :disabled="row.id === loginUser.id">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>

      <div
        v-show="contextMenuVisible"
        class="context-menu"
        :style="{ left: `${contextMenuX}px`, top: `${contextMenuY}px` }"
      >
        <el-card class="menu-card" shadow="hover">
          <div class="menu-item" @click="handleCreateSubFolder">
            <el-icon><FolderAdd /></el-icon>
            <span>新建子文件夹</span>
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

      <el-dialog v-model="folderDialogVisible" :title="folderDialogTitle" width="420px">
        <el-input
          v-model="folderForm.name"
          maxlength="50"
          show-word-limit
          placeholder="请输入文件夹名称"
          @keyup.enter="submitFolderForm"
        />
        <template #footer>
          <el-button @click="folderDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitFolderForm">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="moveDialogVisible" title="移动到文件夹" width="460px">
        <p class="dialog-tip">可直接选择任意层级目录作为目标位置。</p>
        <div class="move-tree-box">
          <el-tree
            ref="moveTreeRef"
            :data="folderTree"
            :props="treeProps"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
            @node-click="handleMoveTreeNodeClick"
          >
            <template #default="{ node, data }">
              <div class="folder-node" :class="{ disabled: isMoveTargetDisabled(data.id) }">
                <el-icon><Folder /></el-icon>
                <span>{{ node.label }}</span>
              </div>
            </template>
          </el-tree>
        </div>
        <div v-if="moveTargetFolderId" class="selected-note">
          目标文件夹：{{ getFolderName(moveTargetFolderId) }}
        </div>
        <template #footer>
          <el-button @click="moveDialogVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!moveTargetFolderId" @click="submitMove">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="previewDialogVisible" title="素材预览" width="860px">
        <div v-if="previewAsset" class="preview-panel">
          <img v-if="isImage(previewAsset.fileType)" :src="previewUrl(previewAsset)" alt="preview" class="preview-image" />
          <video v-else :src="previewUrl(previewAsset)" controls class="preview-video" />
        </div>
        <div v-if="previewAsset" class="preview-meta">
          <p><strong>文件名：</strong>{{ previewAsset.originalName }}</p>
          <p><strong>文件大小：</strong>{{ formatFileSize(previewAsset.fileSize) }}</p>
          <p><strong>上传日期：</strong>{{ previewAsset.uploadDate }}</p>
          <p><strong>元数据：</strong>{{ parseMetadata(previewAsset.metadata) }}</p>
        </div>
      </el-dialog>

      <el-dialog v-model="createUserDialogVisible" title="新建用户" width="440px">
        <el-form label-position="top">
          <el-form-item label="用户名">
            <el-input v-model="createUserForm.username" placeholder="例如：editor01" />
          </el-form-item>
          <el-form-item label="显示名称">
            <el-input v-model="createUserForm.displayName" placeholder="例如：素材编辑" />
          </el-form-item>
          <el-form-item label="初始密码">
            <el-input v-model="createUserForm.password" type="password" show-password placeholder="请输入初始密码" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="createUserForm.role" style="width: 100%">
              <el-option label="普通用户" value="USER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="createUserDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateUser">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="editUserDialogVisible" title="编辑用户" width="440px">
        <el-form label-position="top">
          <el-form-item label="用户名">
            <el-input :model-value="editUserTarget?.username || ''" disabled />
          </el-form-item>
          <el-form-item label="显示名称">
            <el-input v-model="editUserForm.displayName" placeholder="请输入显示名称" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="editUserForm.role" style="width: 100%">
              <el-option label="普通用户" value="USER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editUserDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdateUser">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="resetPasswordDialogVisible" title="重置密码" width="420px">
        <el-form label-position="top">
          <el-form-item label="用户">
            <el-input :model-value="resetPasswordTarget?.displayName || ''" disabled />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="resetPasswordForm.password" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleResetPassword">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { SortUp, SortDown } from '@element-plus/icons-vue'
import authApi from './api/auth'
import assetApi from './api/assets'
import folderApi from './api/folders'
import usersApi from './api/users'

const MAX_UPLOAD_SIZE = 500 * 1024 * 1024
const SUPPORTED_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'mp4', 'avi', 'mov', 'mkv', 'webm'])
const UPLOAD_CONCURRENCY = 3
const PREVIEW_INITIAL_BATCH_SIZE = 8
const PREVIEW_BATCH_SIZE = 6
const PREVIEW_BATCH_DELAY = 220
const folderPanelTitle = '\u6587\u4ef6\u5939'
const folderSortNameLabel = '\u540d\u79f0'
const folderSortCreatedAtLabel = '\u521b\u5efa\u65f6\u95f4'
const folderSortUpdatedAtLabel = '\u6700\u8fd1\u66f4\u65b0\u65f6\u95f4'
const createFolderButtonLabel = '\u65b0\u5efa'

const treeRef = ref(null)
const tableRef = ref(null)
const moveTreeRef = ref(null)

const appReady = ref(false)
const loginLoading = ref(false)
const loginUser = ref(null)
const activeView = ref('assets')

const loginForm = reactive({
  username: '',
  password: ''
})

const folderTree = ref([])
const currentFolderId = ref(null)
const folderSortBy = ref('name')
const folderSortOrder = ref('desc')
const expandedFolderIds = ref(new Set())
const assetList = ref([])
const tableLoading = ref(false)
const selectedAssets = ref([])
const uploading = ref(false)
const uploadProgressText = ref('')
const visiblePreviewRefs = ref(new Set())
let previewQueue = []
let previewQueueTimer = null

const dateRange = ref([])
const filterFileType = ref('')
const filterUploadedBy = ref('')
const filterFileName = ref('')
const allUsers = ref([])
const pagination = ref({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuNode = ref(null)

const folderDialogVisible = ref(false)
const folderDialogTitle = ref('')
const folderForm = reactive({
  id: null,
  name: '',
  parentId: 0
})

const moveDialogVisible = ref(false)
const moveTargetFolderId = ref(null)
const currentMoveAssets = ref([])

const previewDialogVisible = ref(false)
const previewAsset = ref(null)

const usersLoading = ref(false)
const userList = ref([])
const createUserDialogVisible = ref(false)
const createUserForm = reactive({
  username: '',
  displayName: '',
  password: '',
  role: 'USER'
})
const editUserDialogVisible = ref(false)
const editUserTarget = ref(null)
const editUserForm = reactive({
  displayName: '',
  role: 'USER'
})
const resetPasswordDialogVisible = ref(false)
const resetPasswordTarget = ref(null)
const resetPasswordForm = reactive({
  password: ''
})

const treeProps = {
  label: 'name',
  children: 'children'
}

const isAdmin = computed(() => loginUser.value?.role === 'ADMIN')
const currentFolderName = computed(() => getFolderName(currentFolderId.value))
const adminCount = computed(() => userList.value.filter(user => user.role === 'ADMIN').length)
const moveDisabledFolderIds = computed(() => new Set(currentMoveAssets.value.map(asset => asset.folderId)))

onMounted(async () => {
  document.addEventListener('click', closeContextMenu)
  window.addEventListener('auth-expired', handleAuthExpired)
  window.addEventListener('beforeunload', handleBeforeUnload)
  window.addEventListener('pagehide', handlePageUnload)
  await initializeSession()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeContextMenu)
  window.removeEventListener('auth-expired', handleAuthExpired)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('pagehide', handlePageUnload)
  handlePageUnload()
})

async function initializeSession() {
  try {
    const res = await authApi.getCurrentUser()
    loginUser.value = res.data
    await loadFolderTree()
    if (isAdmin.value) {
      await loadUsers()
    } else {
      await loadAllUsersForFilter()
    }
  } catch {
    resetWorkspace()
  } finally {
    appReady.value = true
  }
}

async function handleLogin() {
  if (!loginForm.username.trim() || !loginForm.password.trim()) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loginLoading.value = true
  try {
    const res = await authApi.login({
      username: loginForm.username.trim(),
      password: loginForm.password
    })
    loginUser.value = res.data
    activeView.value = 'assets'
    await loadFolderTree()
    if (isAdmin.value) {
      await loadUsers()
    }
    ElMessage.success('登录成功')
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loginLoading.value = false
  }
}

async function handleLogout() {
  try {
    await authApi.logout()
  } finally {
    loginUser.value = null
    resetWorkspace()
  }
}

function handleAuthExpired() {
  if (loginUser.value) {
    ElMessage.warning('登录状态已失效，请重新登录')
  }
  loginUser.value = null
  resetWorkspace()
}

function resetWorkspace() {
  activeView.value = 'assets'
  folderTree.value = []
  currentFolderId.value = null
  folderSortBy.value = 'name'
  folderSortOrder.value = 'desc'
  expandedFolderIds.value = new Set()
  assetList.value = []
  clearPreviewLoading()
  selectedAssets.value = []
  userList.value = []
  uploadProgressText.value = ''
  contextMenuVisible.value = false
  moveDialogVisible.value = false
  previewDialogVisible.value = false
}

async function switchView(view) {
  activeView.value = view
  if (view === 'users' && isAdmin.value) {
    await loadUsers()
  }
}

async function refreshCurrentView() {
  if (activeView.value === 'users' && isAdmin.value) {
    await loadUsers()
  } else {
    await loadFolderTree()
  }
  ElMessage.success('已刷新')
}

async function loadFolderTree() {
  const res = await folderApi.getTree({ sortBy: folderSortBy.value, sortOrder: folderSortOrder.value })
  folderTree.value = res.data || []

  if (folderTree.value.length === 0) {
    currentFolderId.value = null
    assetList.value = []
    return
  }

  if (!findFolderById(folderTree.value, currentFolderId.value)) {
    currentFolderId.value = folderTree.value[0]?.id ?? null
  }

  await nextTick()
  restoreFolderTreeState()
  if (treeRef.value && currentFolderId.value) {
    treeRef.value.setCurrentKey(currentFolderId.value)
  }
  await loadAssets()
}

async function loadAssets() {
  if (!currentFolderId.value) {
    assetList.value = []
    clearPreviewLoading()
    return
  }

  tableLoading.value = true
  try {
    const params = {
      folderId: currentFolderId.value,
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize
    }
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (filterFileType.value) {
      params.fileType = filterFileType.value
    }
    if (filterUploadedBy.value) {
      params.uploadedBy = filterUploadedBy.value
    }
    if (filterFileName.value) {
      params.fileName = filterFileName.value
    }
    const res = await assetApi.getAssets(params)
    if (res.data) {
      assetList.value = res.data.records || []
      pagination.value.total = res.data.total || 0
    } else {
      assetList.value = []
      pagination.value.total = 0
    }
    queuePreviewLoading()
  } finally {
    tableLoading.value = false
  }
}

async function loadUsers() {
  if (!isAdmin.value) {
    return
  }
  usersLoading.value = true
  try {
    const res = await usersApi.listUsers()
    userList.value = res.data || []
    allUsers.value = [...userList.value]
  } finally {
    usersLoading.value = false
  }
}

async function loadAllUsersForFilter() {
  try {
    const res = await usersApi.listUsers()
    allUsers.value = res.data || []
  } catch {
    allUsers.value = []
  }
}

function handleNodeClick(data) {
  currentFolderId.value = data.id
  pagination.value.pageNum = 1
  loadAssets()
}

function handleFolderNodeExpand(data) {
  if (!data?.id) {
    return
  }
  expandedFolderIds.value = new Set(expandedFolderIds.value).add(data.id)
}

function handleFolderNodeCollapse(data) {
  if (!data?.id) {
    return
  }
  const nextExpanded = new Set(expandedFolderIds.value)
  nextExpanded.delete(data.id)
  expandedFolderIds.value = nextExpanded
}

function handleFolderSortChange() {
  pagination.value.pageNum = 1
  loadFolderTree()
}

function toggleFolderSortOrder() {
  folderSortOrder.value = folderSortOrder.value === 'asc' ? 'desc' : 'asc'
  pagination.value.pageNum = 1
  loadFolderTree()
}

function restoreFolderTreeState() {
  if (!treeRef.value) {
    return
  }
  for (const folderId of expandedFolderIds.value) {
    const node = treeRef.value.getNode(folderId)
    node?.expand?.()
  }
}

function handleContextMenu(event, data) {
  event.preventDefault()
  contextMenuVisible.value = true
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuNode.value = data
}

function closeContextMenu() {
  contextMenuVisible.value = false
}

function createRootFolder() {
  folderDialogTitle.value = '新建根文件夹'
  folderDialogVisible.value = true
  folderForm.id = null
  folderForm.name = ''
  folderForm.parentId = 0
}

function handleCreateSubFolder() {
  if (!contextMenuNode.value) {
    return
  }
  folderDialogTitle.value = '新建子文件夹'
  folderDialogVisible.value = true
  folderForm.id = null
  folderForm.name = ''
  folderForm.parentId = contextMenuNode.value.id
  closeContextMenu()
}

function handleRenameFolder() {
  if (!contextMenuNode.value) {
    return
  }
  folderDialogTitle.value = '重命名文件夹'
  folderDialogVisible.value = true
  folderForm.id = contextMenuNode.value.id
  folderForm.name = contextMenuNode.value.name
  folderForm.parentId = contextMenuNode.value.parentId
  closeContextMenu()
}

async function handleDeleteFolder() {
  if (!contextMenuNode.value) {
    return
  }

  try {
    await ElMessageBox.confirm('确定删除该文件夹及其所有子文件夹和素材吗？删除后不可恢复。', '删除文件夹', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await folderApi.deleteFolder(contextMenuNode.value.id)
    if (currentFolderId.value === contextMenuNode.value.id) {
      currentFolderId.value = null
    }
    await loadFolderTree()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    closeContextMenu()
  }
}

async function submitFolderForm() {
  if (!folderForm.name.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }

  try {
    if (folderForm.id) {
      await folderApi.updateFolder(folderForm.id, {
        name: folderForm.name.trim(),
        parentId: folderForm.parentId
      })
      ElMessage.success('重命名成功')
    } else {
      const res = await folderApi.createFolder({
        name: folderForm.name.trim(),
        parentId: folderForm.parentId
      })
      currentFolderId.value = res.data?.id ?? currentFolderId.value
      ElMessage.success('创建成功')
    }
    folderDialogVisible.value = false
    await loadFolderTree()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

function resetFilters() {
  dateRange.value = []
  filterFileType.value = ''
  filterUploadedBy.value = ''
  filterFileName.value = ''
  pagination.value.pageNum = 1
  loadAssets()
}

function handlePageChange(pageNum) {
  pagination.value.pageNum = pageNum
  loadAssets()
}

function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  pagination.value.pageNum = 1
  loadAssets()
}

function handleSelectionChange(selection) {
  selectedAssets.value = selection
}

function clearSelection() {
  tableRef.value?.clearSelection()
  selectedAssets.value = []
}

function handlePreview(asset) {
  previewAsset.value = asset
  previewDialogVisible.value = true
}

function handleDownload(asset) {
  const token = localStorage.getItem('token')
  const url = `/api/assets/${assetRef(asset)}/download?token=${token}`
  const link = document.createElement('a')
  link.href = url
  link.download = asset.originalName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

async function handleDelete(asset) {
  try {
    await assetApi.deleteAsset(assetRef(asset))
    ElMessage.success('删除成功')
    await loadAssets()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function handleMove(asset) {
  currentMoveAssets.value = [asset]
  moveTargetFolderId.value = null
  moveDialogVisible.value = true
}

function batchMove() {
  if (selectedAssets.value.length === 0) {
    ElMessage.warning('请先选择要移动的素材')
    return
  }
  currentMoveAssets.value = [...selectedAssets.value]
  moveTargetFolderId.value = null
  moveDialogVisible.value = true
}

function handleMoveTreeNodeClick(data) {
  if (isMoveTargetDisabled(data.id)) {
    ElMessage.warning('不能选择素材当前所在的文件夹')
    return
  }
  moveTargetFolderId.value = data.id
}

function isMoveTargetDisabled(folderId) {
  return moveDisabledFolderIds.value.has(folderId)
}

async function submitMove() {
  if (!moveTargetFolderId.value) {
    ElMessage.warning('请选择目标文件夹')
    return
  }

  const assetsToMove = currentMoveAssets.value.filter(asset => asset.folderId !== moveTargetFolderId.value)
  if (assetsToMove.length === 0) {
    ElMessage.warning('所选素材已经在目标文件夹中')
    return
  }

  try {
    await assetApi.batchMove(assetsToMove.map(asset => assetRef(asset)), moveTargetFolderId.value)
    moveDialogVisible.value = false
    clearSelection()
    await loadAssets()
    ElMessage.success(`成功移动 ${assetsToMove.length} 个文件`)
  } catch (error) {
    ElMessage.error(error.message || '移动失败')
  }
}

async function batchDelete() {
  if (selectedAssets.value.length === 0) {
    ElMessage.warning('请先选择要删除的素材')
    return
  }

  try {
    await ElMessageBox.confirm(`确定删除已选择的 ${selectedAssets.value.length} 个素材吗？`, '批量删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await assetApi.batchDelete(selectedAssets.value.map(asset => assetRef(asset)))
    clearSelection()
    await loadAssets()
    ElMessage.success('批量删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function batchDownload() {
  if (selectedAssets.value.length === 0) {
    ElMessage.warning('请先选择要下载的素材')
    return
  }

  try {
    const assetRefs = selectedAssets.value.map(asset => assetRef(asset))
    const res = await assetApi.batchDownload(assetRefs)
    if (res && res.data) {
      const urls = res.data
      const token = localStorage.getItem('token') || ''
      
      for (const url of urls) {
        const link = document.createElement('a')
        link.href = `${url}?token=${token}`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
      }
      
      ElMessage.success(`已开始下载 ${urls.length} 个文件`)
    }
  } catch (error) {
    ElMessage.error(error.message || '批量下载失败')
  }
}

function pickFiles() {
  if (!ensureFolderSelected()) {
    return
  }
  openFilePicker({ directory: false }, uploadPickedFiles)
}

function pickDirectory() {
  if (!ensureFolderSelected()) {
    return
  }
  openFilePicker({ directory: true }, uploadPickedDirectory)
}

function openFilePicker(options, handler) {
  const input = document.createElement('input')
  input.type = 'file'
  input.multiple = true
  input.accept = 'image/*,video/*'
  if (options.directory) {
    input.setAttribute('webkitdirectory', '')
    input.setAttribute('directory', '')
  }
  input.style.display = 'none'
  input.addEventListener('change', async event => {
    const files = Array.from(event.target.files || [])
    document.body.removeChild(input)
    await handler(files)
  }, { once: true })
  document.body.appendChild(input)
  input.click()
}

async function uploadPickedFiles(files) {
  const validFiles = collectValidFiles(files)
  if (validFiles.length === 0) {
    return
  }

  await uploadFilesWithConcurrency(validFiles, file => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folderId', String(currentFolderId.value))
    return formData
  }, (file, progress) => `上传 ${file.name}`, {
    successText: count => `成功上传 ${count} 个文件`,
    refresh: loadAssets
  })
}

async function uploadPickedDirectory(files) {
  const validFiles = collectValidFiles(files)
  if (validFiles.length === 0) {
    return
  }

  await uploadFilesWithConcurrency(validFiles, file => {
    const relativePath = file.webkitRelativePath || file.name
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folderId', String(currentFolderId.value))
    formData.append('relativePath', relativePath)
    return formData
  }, (file, progress) => `目录上传 ${file.webkitRelativePath || file.name}`, {
    successText: count => `目录上传完成，共处理 ${count} 个文件`,
    refresh: loadFolderTree
  })
}

async function legacyUploadPickedFiles(files) {
  const validFiles = collectValidFiles(files)
  if (validFiles.length === 0) {
    return
  }

  const folderId = currentFolderId.value
  await uploadFilesWithConcurrency(validFiles, file => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folderId', String(folderId))
    return formData
  }, (file, progress) => `上传 ${file.name} ${progress}%`, {
    successText: count => `成功上传 ${count} 个文件`,
    refresh: loadAssets
  })
}

async function legacyUploadPickedDirectory(files) {
  const validFiles = collectValidFiles(files)
  if (validFiles.length === 0) {
    return
  }

  const folderId = currentFolderId.value
  await uploadFilesWithConcurrency(validFiles, file => {
    const relativePath = file.webkitRelativePath || file.name
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folderId', String(folderId))
    formData.append('relativePath', relativePath)
    return formData
  }, (file, progress) => `目录上传 ${file.webkitRelativePath || file.name} ${progress}%`, {
    successText: count => `目录上传完成，共处理 ${count} 个文件`,
    refresh: loadFolderTree
  })
}

async function uploadFilesWithConcurrency(files, buildFormData, progressLabel, options = {}) {
  uploading.value = true
  uploadAbortController = new AbortController()
  const refresh = options.refresh || loadAssets
  const successText = options.successText || (count => `成功处理 ${count} 个文件`)
  const concurrency = Math.min(UPLOAD_CONCURRENCY, files.length)
  let nextIndex = 0
  let completed = 0
  let failed = 0
  const errors = []

  try {
    const workers = Array.from({ length: concurrency }, async () => {
      while (true) {
        const currentIndex = nextIndex
        nextIndex += 1
        if (currentIndex >= files.length) {
          break
        }

        if (uploadAbortController.signal.aborted) {
          break
        }

        const file = files[currentIndex]
        try {
          const formData = buildFormData(file)
          await assetApi.uploadAsset(formData, event => {
            const percent = Math.round((event.progress || 0) * 100)
            uploadProgressText.value = `${progressLabel(file, percent)} ${completed + failed + 1}/${files.length}`
          }, uploadAbortController.signal)
          completed += 1
        } catch (error) {
          if (error.name === 'AbortError' || error.name === 'CanceledError') {
            break
          }
          failed += 1
          errors.push(error)
        }
      }
    })

    await Promise.all(workers)

    if (uploadAbortController.signal.aborted) {
      ElMessage.info('上传已取消')
      return
    }

    uploadProgressText.value = ''
    await refresh()

    if (completed > 0) {
      ElMessage.success(successText(completed))
    }
    if (failed > 0) {
      ElMessage.warning(`有 ${failed} 个文件上传失败`)
    }
    if (completed === 0 && failed > 0) {
      throw errors[0] || new Error('上传失败')
    }
  } catch (error) {
    if (error.name === 'AbortError' || error.name === 'CanceledError') {
      ElMessage.info('上传已取消')
      return
    }
    ElMessage.error(error.message || '文件上传失败')
  } finally {
    uploading.value = false
    uploadProgressText.value = ''
    uploadAbortController = null
  }
}

function handleBeforeUnload(event) {
  if (!uploading.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

function ensureFolderSelected() {
  if (!currentFolderId.value) {
    ElMessage.warning('请先选择目标文件夹')
    return false
  }
  return true
}

function collectValidFiles(files) {
  const validFiles = []
  let invalidCount = 0
  for (const file of files) {
    if (fileValidationMessage(file)) {
      invalidCount += 1
      continue
    }
    validFiles.push(file)
  }
  if (invalidCount > 0) {
    ElMessage.warning(`已跳过 ${invalidCount} 个不支持或超限文件`)
  }
  return validFiles
}

function fileValidationMessage(file) {
  if (!file) {
    return 'empty'
  }
  if (file.size > MAX_UPLOAD_SIZE) {
    return 'size'
  }
  if (file.type?.startsWith('image/') || file.type?.startsWith('video/')) {
    return ''
  }
  const extension = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : ''
  return SUPPORTED_EXTENSIONS.has(extension) ? '' : 'type'
}

function openCreateUserDialog() {
  createUserForm.username = ''
  createUserForm.displayName = ''
  createUserForm.password = ''
  createUserForm.role = 'USER'
  createUserDialogVisible.value = true
}

async function handleCreateUser() {
  if (!createUserForm.username.trim() || !createUserForm.displayName.trim() || !createUserForm.password.trim()) {
    ElMessage.warning('请完整填写用户信息')
    return
  }

  try {
    await usersApi.createUser({
      username: createUserForm.username.trim(),
      displayName: createUserForm.displayName.trim(),
      password: createUserForm.password,
      role: createUserForm.role
    })
    createUserDialogVisible.value = false
    await loadUsers()
    ElMessage.success('用户创建成功')
  } catch (error) {
    ElMessage.error(error.message || '创建用户失败')
  }
}

function openEditUserDialog(user) {
  editUserTarget.value = user
  editUserForm.displayName = user.displayName
  editUserForm.role = user.role
  editUserDialogVisible.value = true
}

async function handleUpdateUser() {
  if (!editUserTarget.value) {
    return
  }
  if (!editUserForm.displayName.trim()) {
    ElMessage.warning('请输入显示名称')
    return
  }

  try {
    const res = await usersApi.updateUser(editUserTarget.value.id, {
      displayName: editUserForm.displayName.trim(),
      role: editUserForm.role
    })
    editUserDialogVisible.value = false
    await loadUsers()
    syncCurrentUserInfo(res.data)
    ElMessage.success('用户信息已更新')
  } catch (error) {
    ElMessage.error(error.message || '更新用户失败')
  }
}

function openResetPasswordDialog(user) {
  resetPasswordTarget.value = user
  resetPasswordForm.password = ''
  resetPasswordDialogVisible.value = true
}

async function handleResetPassword() {
  if (!resetPasswordTarget.value) {
    return
  }
  if (!resetPasswordForm.password.trim()) {
    ElMessage.warning('请输入新密码')
    return
  }

  try {
    await usersApi.resetPassword(resetPasswordTarget.value.id, resetPasswordForm.password)
    resetPasswordDialogVisible.value = false
    ElMessage.success('密码已重置')
  } catch (error) {
    ElMessage.error(error.message || '重置密码失败')
  }
}

async function handleDeleteUser(user) {
  try {
    await usersApi.deleteUser(user.id)
    await loadUsers()
    ElMessage.success('用户已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除用户失败')
  }
}

function syncCurrentUserInfo(updatedUser) {
  if (!updatedUser || !loginUser.value || updatedUser.id !== loginUser.value.id) {
    return
  }

  loginUser.value = {
    ...loginUser.value,
    displayName: updatedUser.displayName,
    role: updatedUser.role
  }

  if (updatedUser.role !== 'ADMIN') {
    activeView.value = 'assets'
  }
}

function findFolderById(nodes, folderId) {
  if (!folderId) {
    return null
  }
  for (const node of nodes) {
    if (node.id === folderId) {
      return node
    }
    if (node.children?.length) {
      const matchedNode = findFolderById(node.children, folderId)
      if (matchedNode) {
        return matchedNode
      }
    }
  }
  return null
}

function getFolderName(folderId) {
  return findFolderById(folderTree.value, folderId)?.name || ''
}

function formatFileSize(bytes) {
  if (!bytes) {
    return '0 B'
  }
  const units = ['B', 'KB', 'MB', 'GB']
  const base = 1024
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(base)), units.length - 1)
  return `${(bytes / (base ** index)).toFixed(2)} ${units[index]}`
}

function parseMetadata(metadata) {
  if (!metadata || metadata === '{}') {
    return '-'
  }
  try {
    const parsed = typeof metadata === 'string' ? JSON.parse(metadata) : metadata
    const parts = []
    if (parsed.width && parsed.height) {
      parts.push(`${parsed.width} × ${parsed.height}`)
      parts.push(`(${getAspectRatio(parsed.width, parsed.height)})`)
    }
    if (parsed.duration) {
      parts.push(`${Math.round(parsed.duration)} 秒`)
    }
    if (parsed.format) {
      parts.push(parsed.format)
    }
    return parts.join(' / ') || '-'
  } catch {
    return '-'
  }
}

function getAspectRatio(width, height) {
  const gcd = (a, b) => b === 0 ? a : gcd(b, a % b)
  const divisor = gcd(width, height)
  const ratioW = Math.round(width / divisor)
  const ratioH = Math.round(height / divisor)
  return `${ratioW}:${ratioH}`
}

function fileTypeLabel(type) {
  if (type === 'image') {
    return '图片'
  }
  if (type === 'video') {
    return '视频'
  }
  return '其他'
}

function fileTypeTag(type) {
  if (type === 'image') {
    return 'success'
  }
  if (type === 'video') {
    return 'warning'
  }
  return 'info'
}

function roleLabel(role) {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}

function isImage(type) {
  return type === 'image'
}

function isVideo(type) {
  return type === 'video'
}

function assetRef(asset) {
  return asset?.publicId || asset?.id
}

function previewUrl(asset) {
  return `/api/assets/${assetRef(asset)}/preview`
}

let uploadAbortController = null

function queuePreviewLoading() {
  clearPreviewLoading()

  if (!assetList.value || assetList.value.length === 0) {
    return
  }

  const imageAssets = assetList.value.filter(asset => isImage(asset.fileType))
  if (imageAssets.length === 0) {
    return
  }

  previewQueue = [...imageAssets]

  const processBatch = () => {
    if (previewQueue.length === 0) {
      return
    }

    const batch = previewQueue.splice(0, PREVIEW_BATCH_SIZE)
    batch.forEach(asset => {
      if (asset?.id) {
        visiblePreviewRefs.value.add(asset.id)
      }
    })

    if (previewQueue.length > 0) {
      previewQueueTimer = setTimeout(processBatch, PREVIEW_BATCH_DELAY)
    }
  }

  const initialBatch = previewQueue.splice(0, PREVIEW_INITIAL_BATCH_SIZE)
  initialBatch.forEach(asset => {
    if (asset?.id) {
      visiblePreviewRefs.value.add(asset.id)
    }
  })

  if (previewQueue.length > 0) {
    previewQueueTimer = setTimeout(processBatch, PREVIEW_BATCH_DELAY)
  }
}

function clearPreviewLoading() {
  if (previewQueueTimer) {
    clearTimeout(previewQueueTimer)
    previewQueueTimer = null
  }
  previewQueue = []
  visiblePreviewRefs.value = new Set()
}

function handlePageUnload() {
  if (uploadAbortController) {
    uploadAbortController.abort()
    uploadAbortController = null
  }
  clearPreviewLoading()
}

function getUploaderDisplayName(username) {
  if (!username) return '-'
  const user = allUsers.value.find(u => u.username === username)
  return user ? user.displayName : username
}
</script>

<style scoped>
:global(body) {
  margin: 0;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  background: #efe8de;
}

.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(244, 114, 54, 0.14), transparent 22%),
    radial-gradient(circle at right center, rgba(14, 165, 233, 0.12), transparent 25%),
    linear-gradient(135deg, #f6f0e8 0%, #eef3f1 100%);
}

.boot-screen,
.login-screen {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px;
}

.boot-card {
  width: min(560px, 100%);
  border-radius: 28px;
}

.login-layout {
  width: min(1140px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(360px, 420px);
  gap: 30px;
  align-items: stretch;
}

.brand-panel,
.login-card,
.folder-panel,
.hero-card,
.selection-card,
.toolbar-card,
.table-card,
.user-hero {
  border-radius: 28px;
  box-shadow: 0 18px 50px rgba(54, 46, 35, 0.08);
}

.brand-panel {
  padding: 44px;
  color: #fff7ed;
  background:
    linear-gradient(160deg, rgba(120, 53, 15, 0.96), rgba(22, 101, 52, 0.88)),
    #7c2d12;
}

.brand-panel h1 {
  margin: 16px 0;
  font-size: 48px;
  line-height: 1.05;
}

.brand-copy {
  max-width: 460px;
  line-height: 1.8;
  color: rgba(255, 247, 237, 0.84);
}

.brand-badges {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 28px;
}

.brand-badges span,
.brand-credentials {
  display: inline-flex;
  align-items: center;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 247, 237, 0.12);
}

.brand-credentials {
  margin-top: 20px;
}

.login-card {
  align-self: center;
}

.login-card-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-weight: 700;
}

.login-card-header small {
  color: #64748b;
}

.stretch-button {
  width: 100%;
}

.panel-kicker,
.small-label {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.topbar-kicker,
.small-label {
  color: #9a3412;
}

.panel-kicker {
  color: rgba(255, 247, 237, 0.7);
}

.console-shell {
  min-height: 100vh;
  padding: 18px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 18px 24px;
  margin-bottom: 18px;
  border-radius: 28px;
  background: rgba(255, 251, 245, 0.82);
  backdrop-filter: blur(18px);
  box-shadow: 0 16px 40px rgba(100, 116, 139, 0.12);
}

.topbar h2 {
  margin: 6px 0 0;
  color: #292524;
  font-size: 30px;
}

.topbar-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.view-switcher {
  display: inline-flex;
  padding: 4px;
  border-radius: 999px;
  background: #f5e7d6;
}

.switch-chip {
  border: 0;
  background: transparent;
  padding: 10px 18px;
  border-radius: 999px;
  cursor: pointer;
  color: #7c2d12;
  font-weight: 600;
}

.switch-chip.active {
  background: linear-gradient(135deg, #c2410c, #0f766e);
  color: white;
}

.user-chip {
  border: none;
}

.asset-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 18px;
  min-height: calc(100vh - 134px);
}

.folder-panel {
  padding: 20px;
  background: rgba(255, 251, 245, 0.9);
}

.folder-panel-header {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
  margin-bottom: 18px;
}

.folder-panel-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 38px auto;
  gap: 8px;
  align-items: center;
}

.folder-sort-select {
  min-width: 0;
}

.folder-sort-toggle {
  width: 38px;
  padding-left: 0;
  padding-right: 0;
}

.folder-panel-header h3,
.hero-copy h3,
.user-hero h3 {
  margin: 6px 0 0;
  font-size: 26px;
  color: #292524;
}

.asset-main,
.user-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.hero-card,
.selection-card,
.toolbar-card,
.table-card,
.user-hero {
  padding: 22px 24px;
  background: rgba(255, 251, 245, 0.9);
}

.hero-card,
.user-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
}

.hero-copy p,
.user-hero p {
  margin-bottom: 0;
  line-height: 1.75;
  color: #57534e;
}

.hero-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.stat-box {
  min-width: 120px;
  padding: 14px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fff7ed, #ecfeff);
}

.stat-box.slim {
  min-width: 132px;
}

.stat-box span {
  display: block;
  color: #78716c;
  font-size: 12px;
}

.stat-box strong {
  display: block;
  margin-top: 6px;
  font-size: 24px;
  color: #1c1917;
}

.hero-actions,
.selection-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.selection-card,
.toolbar-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.user-table-card {
  min-height: calc(100vh - 320px);
}

.pagination-container {
  padding: 12px 8px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: flex-end;
}

.table-card .el-table {
  flex: 1;
  min-height: 0;
}

.folder-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
}

.folder-node.disabled {
  color: #a8a29e;
  cursor: not-allowed;
}

.asset-preview-thumb {
  width: 88px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #e7e5e4;
  border-radius: 8px;
  background: #fafaf9;
  cursor: pointer;
}

.asset-preview-thumb:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.asset-preview-media {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.asset-preview-placeholder {
  color: #78716c;
  font-size: 12px;
  font-weight: 700;
}

.ellipsis-text,
.meta-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-text {
  color: #6b7280;
}

.upload-progress {
  margin-top: 14px;
  font-weight: 700;
  color: #0f766e;
}

.context-menu {
  position: fixed;
  z-index: 4000;
}

.menu-card {
  min-width: 180px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 12px;
  cursor: pointer;
}

.menu-item:hover {
  background: #f5f5f4;
}

.menu-item.danger {
  color: #dc2626;
}

.menu-divider {
  margin: 6px 0;
}

.dialog-tip {
  margin-top: 0;
  color: #6b7280;
}

.move-tree-box {
  max-height: 360px;
  overflow: auto;
  padding: 10px 12px;
  border-radius: 18px;
  border: 1px solid #e7e5e4;
}

.selected-note {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f5f5f4;
  color: #44403c;
}

.preview-panel {
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.preview-image,
.preview-video {
  max-width: 100%;
  max-height: 500px;
  border-radius: 24px;
}

.preview-meta {
  padding: 18px;
  border-radius: 20px;
  background: #fafaf9;
}

@media (max-width: 1100px) {
  .login-layout,
  .asset-layout {
    grid-template-columns: 1fr;
  }

  .topbar,
  .hero-card,
  .user-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .console-shell {
    padding: 12px;
  }

  .topbar {
    padding: 18px;
  }

  .folder-panel,
  .hero-card,
  .selection-card,
  .toolbar-card,
  .table-card,
  .user-hero {
    padding: 18px;
  }

  .brand-panel {
    padding: 28px;
  }

  .brand-panel h1 {
    font-size: 36px;
  }
}
</style>
