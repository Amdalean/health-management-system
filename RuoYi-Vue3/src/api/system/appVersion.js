import request from '@/utils/request'

// 查询APP版本列表
export function listAppVersion(query) {
  return request({
    url: '/system/appVersion/list',
    method: 'get',
    params: query
  })
}

// 查询APP版本详细
export function getAppVersion(id) {
  return request({
    url: '/system/appVersion/' + id,
    method: 'get'
  })
}

// 新增APP版本
export function addAppVersion(data) {
  return request({
    url: '/system/appVersion',
    method: 'post',
    data: data
  })
}

// 修改APP版本
export function updateAppVersion(data) {
  return request({
    url: '/system/appVersion',
    method: 'put',
    data: data
  })
}

// 删除APP版本
export function delAppVersion(id) {
  return request({
    url: '/system/appVersion/' + id,
    method: 'delete'
  })
}

// 上传APP安装包
export function uploadAppFile(data) {
  return request({
    url: '/system/appVersion/upload',
    method: 'post',
    data: data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
