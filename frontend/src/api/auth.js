import request from './request'

export default {
  login(data) {
    return request({
      url: '/auth/login',
      method: 'post',
      data,
      skipErrorMessage: true,
      skipAuthEvent: true
    })
  },

  logout() {
    return request({
      url: '/auth/logout',
      method: 'post',
      skipErrorMessage: true,
      skipAuthEvent: true
    })
  },

  getCurrentUser() {
    return request({
      url: '/auth/me',
      method: 'get',
      skipErrorMessage: true,
      skipAuthEvent: true
    })
  }
}
