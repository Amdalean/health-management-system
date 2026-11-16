/**
 * 推送相关API
 * add by CYQ 2026-06-08
 */
import request from '@/utils/request'

// ========== 设备管理 ==========

// 上报推送设备信息 (CID绑定)
export function reportPushDevice(data) {
  return request({
    url: '/mobile/push/device',
    method: 'post',
    data: data
  })
}

// 根据clientId获取设备信息
export function getDeviceByClientId(clientId) {
  return request({
    url: '/mobile/push/device/client/' + clientId,
    method: 'get'
  })
}

// ========== 消息推送 ==========

// 向指定设备发送推送
export function pushToSingle(data) {
  return request({
    url: '/mobile/push/message/single',
    method: 'post',
    data: data
  })
}

// 向所有设备发送推送 (群推)
export function pushToAll(data) {
  return request({
    url: '/mobile/push/message/all',
    method: 'post',
    data: data
  })
}

// 向指定用户的所有设备推送
export function pushToUser(data) {
  return request({
    url: '/mobile/push/message/user',
    method: 'post',
    data: data
  })
}
