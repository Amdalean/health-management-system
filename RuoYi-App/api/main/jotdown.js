import request from '@/utils/request'
import upload from '@/utils/upload'

// 查询随手记列表
export function listJotdown(query) {
  return request({
    url: '/main/jotdown/list',
    method: 'get',
    params: query
  })
}

// 查询随手记详细
export function getJotdown(id) {
  return request({
    url: '/main/jotdown/' + id,
    method: 'get'
  })
}

// 新增随手记
export function addJotdown(data) {
  return request({
    url: '/main/jotdown',
    method: 'post',
    data: data
  })
}

// 修改随手记
export function updateJotdown(data) {
  return request({
    url: '/main/jotdown',
    method: 'put',
    data: data
  })
}

// 删除随手记
export function delJotdown(id) {
  return request({
    url: '/main/jotdown/' + id,
    method: 'delete'
  })
}

/**
 * AI图片识别录入
 * 上传账单/收据图片，AI自动识别费用信息并返回JSON
 * @param {string} filePath - 图片临时路径
 * @returns {Promise} { description, amount, date, expenseType, remarks }
 */
export function aiRecognizeJotdown(filePath) {
  return upload({
    url: '/main/jotdown/ai-recognize',
    filePath: filePath,
    name: 'file',
    // AI 多模态识别较慢（实测 70~120s，与图片大小无关），超时需大于识别耗时
    timeout: 300000
  })
}

/**
 * 查询随手记统计（总金额、总条数、费用类型分布），不受分页影响
 *
 * add by CYQ 2026年7月2日
 */
export function jotdownStats(query) {
  return request({
    url: '/main/jotdown/stats',
    method: 'get',
    params: query
  })
}
