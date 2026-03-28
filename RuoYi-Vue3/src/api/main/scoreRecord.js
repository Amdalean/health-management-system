import request from '@/utils/request'

// 查询成绩分析记录列表
export function listScoreRecord(query) {
  return request({
    url: '/main/scoreRecord/list',
    method: 'get',
    params: query
  })
}

// 查询成绩分析记录详细
export function getScoreRecord(id) {
  return request({
    url: '/main/scoreRecord/' + id,
    method: 'get'
  })
}

// 新增成绩分析记录
export function addScoreRecord(data) {
  return request({
    url: '/main/scoreRecord',
    method: 'post',
    data: data
  })
}

// 修改成绩分析记录
export function updateScoreRecord(data) {
  return request({
    url: '/main/scoreRecord',
    method: 'put',
    data: data
  })
}

// 删除成绩分析记录
export function delScoreRecord(id) {
  return request({
    url: '/main/scoreRecord/' + id,
    method: 'delete'
  })
}

// 提交视频进行 AI 分析
export function submitForAnalysis(data) {
  return request({
    url: '/main/scoreRecord/submit',
    method: 'post',
    data: data
  })
}

// 查询 AI 分析任务状态
export function getTaskStatus(taskId) {
  return request({
    url: '/main/scoreRecord/task/' + taskId,
    method: 'get'
  })
}
