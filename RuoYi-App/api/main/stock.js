import request from '@/utils/request'

// 查询持仓列表（GET /main/stock/holdings）
// add by CYQ 2026-08-16 移动端股票模块
export function getStockHoldings() {
  return request({
    url: '/main/stock/holdings',
    method: 'get'
  })
}

// 查询待处理建议（仅 pending，GET /main/stock/recommendations/pending）
export function getPendingRecommendations() {
  return request({
    url: '/main/stock/recommendations/pending',
    method: 'get'
  })
}

// 确认执行建议（PUT /main/stock/execute/{id}?price=&shares=）
// add by CYQ 2026-08-18 改为 query param 传参，匹配 Java @RequestParam
export function executeRecommendation(id, params) {
  return request({
    url: '/main/stock/execute/' + id,
    method: 'put',
    params: params || {}
  })
}

// 标记建议已过期（不执行交易，PUT /main/stock/expire/{id}）
export function expireRecommendation(id) {
  return request({
    url: '/main/stock/expire/' + id,
    method: 'put'
  })
}
