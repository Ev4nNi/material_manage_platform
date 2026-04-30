import request from './request'

export default {
  getAssets(params) {
    return request({
      url: '/assets',
      method: 'get',
      params
    })
  },

  uploadAsset(formData, onUploadProgress) {
    return request({
      url: '/assets/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress
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

  deleteAsset(id) {
    return request({
      url: `/assets/${id}`,
      method: 'delete'
    })
  },

  updateAsset(id, data) {
    return request({
      url: `/assets/${id}`,
      method: 'put',
      data
    })
  },

  getAssetDetail(id) {
    return request({
      url: `/assets/${id}`,
      method: 'get'
    })
  },

  reExtractMetadata(id) {
    return request({
      url: `/assets/${id}/re-extract`,
      method: 'post'
    })
  },

  batchDownload(assetIds) {
    return request({
      url: '/assets/batch-download',
      method: 'post',
      data: { assetIds }
    })
  }
}
