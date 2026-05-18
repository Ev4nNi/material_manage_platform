import request from './request'

export default {
  getAssets(params) {
    return request({
      url: '/assets',
      method: 'get',
      params
    })
  },

  uploadAsset(formData, onUploadProgress, signal) {
    return request({
      url: '/assets/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
      signal
    })
  },

  uploadDirectory(formData, onUploadProgress) {
    return request({
      url: '/assets/upload-directory',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress
    })
  },

  deleteAsset(assetRef) {
    return request({
      url: `/assets/${assetRef}`,
      method: 'delete'
    })
  },

  updateAsset(assetRef, data) {
    return request({
      url: `/assets/${assetRef}`,
      method: 'put',
      data
    })
  },

  batchMove(assetRefs, folderId) {
    return request({
      url: '/assets/batch-move',
      method: 'put',
      data: { assetRefs, folderId }
    })
  },

  batchDelete(assetRefs) {
    return request({
      url: '/assets/batch',
      method: 'delete',
      data: { assetRefs }
    })
  },

  getAssetDetail(assetRef) {
    return request({
      url: `/assets/${assetRef}`,
      method: 'get'
    })
  },

  reExtractMetadata(assetRef) {
    return request({
      url: `/assets/${assetRef}/re-extract`,
      method: 'post'
    })
  },

  batchDownload(assetRefs) {
    return request({
      url: '/assets/batch-download',
      method: 'post',
      data: { assetRefs }
    })
  }
}
