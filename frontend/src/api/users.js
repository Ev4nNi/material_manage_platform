import request from './request'

export default {
  listUsers() {
    return request({
      url: '/users',
      method: 'get'
    })
  },

  createUser(data) {
    return request({
      url: '/users',
      method: 'post',
      data
    })
  },

  updateUser(id, data) {
    return request({
      url: `/users/${id}`,
      method: 'put',
      data
    })
  },

  resetPassword(id, password) {
    return request({
      url: `/users/${id}/password`,
      method: 'put',
      data: { password }
    })
  },

  deleteUser(id) {
    return request({
      url: `/users/${id}`,
      method: 'delete'
    })
  }
}
