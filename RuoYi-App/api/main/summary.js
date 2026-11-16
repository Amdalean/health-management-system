import request from '@/utils/request'

// 查询财务汇总列表
export function listSummary(query) {
  return request({
    url: '/main/summary/list',
    method: 'get',
    params: query
  })
}

// 查询财务汇总详细
export function getSummary(id) {
  return request({
    url: '/main/summary/' + id,
    method: 'get'
  })
}

// 获取初始化信息（下月年月 + 上月明细模板 + 月初存款）
export function initSummary() {
  return request({
    url: '/main/summary/init',
    method: 'get'
  })
}

// 新增财务汇总
export function addSummary(data) {
  return request({
    url: '/main/summary',
    method: 'post',
    data: data
  })
}

// 修改财务汇总
export function updateSummary(data) {
  return request({
    url: '/main/summary',
    method: 'put',
    data: data
  })
}

// 删除财务汇总
export function delSummary(id) {
  return request({
    url: '/main/summary/' + id,
    method: 'delete'
  })
}

// 查询字典数据
export function getDictData(dictType) {
  return request({
    url: '/system/dict/data/type/' + dictType,
    method: 'get'
  })
}

// 查询财务汇总统计 add by CYQ 2026-09-01
export function getSummaryStat(query) {
  return request({
    url: '/main/summary/stat',
    method: 'get',
    params: query
  })
}
