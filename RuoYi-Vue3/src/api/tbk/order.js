import request from '@/utils/request'

// 查询联盟订单列表
export function listTbkOrder(query) {
  return request({
    url: '/tbk/order/list',
    method: 'get',
    params: query
  })
}

// 联盟收益汇总
export function getTbkOrderStats(query) {
  return request({
    url: '/tbk/order/stats',
    method: 'get',
    params: query
  })
}

// 同步联盟订单（不传 days 只回看最近几小时）
export function syncTbkOrder(data) {
  return request({
    url: '/tbk/order/sync',
    method: 'post',
    // 补同步 7 天需要多次调用第三方接口，默认 10 秒会超时
    timeout: 300000,
    data: data || {}
  })
}
