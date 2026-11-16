import request from '@/utils/request'

// 查询时间管理局列表
export function listTimeDiary(query) {
  return request({
    url: '/main/timeDiary/list',
    method: 'get',
    params: query
  })
}

// 查询时间管理局详细
export function getTimeDiary(diaryId) {
  return request({
    url: '/main/timeDiary/' + diaryId,
    method: 'get'
  })
}

// 新增时间管理局
export function addTimeDiary(data) {
  return request({
    url: '/main/timeDiary',
    method: 'post',
    data: data
  })
}

// 修改时间管理局
export function updateTimeDiary(data) {
  return request({
    url: '/main/timeDiary',
    method: 'put',
    data: data
  })
}

// 删除时间管理局
export function delTimeDiary(diaryId) {
  return request({
    url: '/main/timeDiary/' + diaryId,
    method: 'delete'
  })
}

// 查询统计（分类占比 + 每日趋势）
export function getTimeDiaryStats(query) {
  return request({
    url: '/main/timeDiary/stats',
    method: 'get',
    params: query
  })
}

// 查询某天时间轴
export function getTimeDiaryTimeline(query) {
  return request({
    url: '/main/timeDiary/timeline',
    method: 'get',
    params: query
  })
}
