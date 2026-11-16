import request from '@/utils/request'

// 查询系统集成日志列表
export function listIntegrationLog(query) {
  return request({
    url: '/monitor/integrationlog/list',
    method: 'get',
    params: query
  })
}

// 查询系统集成日志详细
export function getIntegrationLog(logId) {
  return request({
    url: '/monitor/integrationlog/' + logId,
    method: 'get'
  })
}

// 删除系统集成日志
export function delIntegrationLog(logIds) {
  return request({
    url: '/monitor/integrationlog/' + logIds,
    method: 'delete'
  })
}

// 清空系统集成日志
export function cleanIntegrationLog() {
  return request({
    url: '/monitor/integrationlog/clean',
    method: 'delete'
  })
}
