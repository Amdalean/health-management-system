import upload from '@/utils/upload'
import request from '@/utils/request'

// 用户密码重置
export function updateUserPwd(oldPassword, newPassword) {
  const data = {
    oldPassword,
    newPassword
  }
  return request({
    url: '/system/user/profile/updatePwd',
    method: 'put',
    params: data
  })
}

// 查询用户个人信息
export function getUserProfile() {
  return request({
    url: '/system/user/profile',
    method: 'get'
  })
}

// 修改用户个人信息
export function updateUserProfile(data) {
  return request({
    url: '/system/user/profile',
    method: 'put',
    data: data
  })
}

// 订阅量化通知（维护邮箱 + 授予 quant 角色）add by CYQ 2026-09-02
export function subscribeQuant(email) {
  return request({
    url: '/system/user/profile/subscribeQuant',
    method: 'post',
    data: { email }
  })
}
export function getSubscribeQuantStatus() {
  return request({
    url: '/system/user/profile/subscribeQuant/status',
    method: 'get'
  })
}

// 用户头像上传
export function uploadAvatar(data) {
  return upload({
    url: '/system/user/profile/avatar',
    name: data.name,
    filePath: data.filePath
  })
}
