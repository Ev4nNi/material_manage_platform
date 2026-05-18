import request from './request'

export default {
  getTree(params = {}) {
    return request({
      url: '/folders/tree',
      method: 'get',
      params
    })
  },

  getRootFolders() {
    return request({
      url: '/folders',
      method: 'get',
      params: { parentId: 0 }
    })
  },

  getChildrenFolders(parentId) {
    return request({
      url: '/folders',
      method: 'get',
      params: { parentId }
    })
  },

  createFolder(data) {
    return request({
      url: '/folders',
      method: 'post',
      data
    })
  },

  updateFolder(id, data) {
    return request({
      url: `/folders/${id}`,
      method: 'put',
      data
    })
  },

  deleteFolder(id) {
    return request({
      url: `/folders/${id}`,
      method: 'delete'
    })
  },

  getFolderDetail(id) {
    return request({
      url: `/folders/${id}`,
      method: 'get'
    })
  }
}
