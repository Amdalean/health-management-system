import request from '@/utils/request'

// 查询APP版本列表
export function listAppVersion(query) {
  return request({
    url: '/mobile/appVersion/list',
    method: 'get',
    params: query
  })
}

// 查询APP版本详细
export function getAppVersion(id) {
  return request({
    url: '/mobile/appVersion/' + id,
    method: 'get'
  })
}

// 新增APP版本
export function addAppVersion(data) {
  return request({
    url: '/mobile/appVersion',
    method: 'post',
    data: data
  })
}

// 修改APP版本
export function updateAppVersion(data) {
  return request({
    url: '/mobile/appVersion',
    method: 'put',
    data: data
  })
}

// 删除APP版本
export function delAppVersion(ids) {
  return request({
    url: '/mobile/appVersion/' + ids,
    method: 'delete'
  })
}
