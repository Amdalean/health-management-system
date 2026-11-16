import request from '@/utils/request'

// 查询推送设备列表
export function listPushDevice(query) {
  return request({
    url: '/mobile/push/device/list',
    method: 'get',
    params: query
  })
}

// 查询推送设备详细
export function getPushDevice(id) {
  return request({
    url: '/mobile/push/device/' + id,
    method: 'get'
  })
}

// 修改推送设备
export function updatePushDevice(data) {
  return request({
    url: '/mobile/push/device',
    method: 'put',
    data: data
  })
}

// 删除推送设备
export function delPushDevice(ids) {
  return request({
    url: '/mobile/push/device/' + ids,
    method: 'delete'
  })
}

// 向指定设备发送推送
export function pushToSingle(data) {
  return request({
    url: '/mobile/push/message/single',
    method: 'post',
    data: data
  })
}

// 向所有设备发送推送
export function pushToAll(data) {
  return request({
    url: '/mobile/push/message/all',
    method: 'post',
    data: data
  })
}

// 向指定用户所有设备发送推送
export function pushToUser(data) {
  return request({
    url: '/mobile/push/message/user',
    method: 'post',
    data: data
  })
}
