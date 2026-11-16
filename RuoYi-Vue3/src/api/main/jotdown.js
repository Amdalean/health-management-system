import request from '@/utils/request'

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

// AI图片识别录入
export function aiRecognizeJotdown(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/main/jotdown/ai-recognize',
    method: 'post',
    data: formData,
    // AI 多模态识别较慢（实测 70~120s，与图片大小无关），超时需远大于识别耗时
    timeout: 300000,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
