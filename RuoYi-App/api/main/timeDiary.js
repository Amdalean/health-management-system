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

// 查询某天时间轴
export function getTimeDiaryTimeline(query) {
  return request({
    url: '/main/timeDiary/timeline',
    method: 'get',
    params: query
  })
}

/**
 * 查询时间管理局统计（分类分布+每日趋势+每日分类趋势），不受分页影响
 *
 * add by CYQ 2026年8月7日
 */
export function getTimeDiaryStats(query) {
  return request({
    url: '/main/timeDiary/stats',
    method: 'get',
    params: query
  })
}

/**
 * 查询字典数据（时间管理局分类选项来自字典 time_diary_category）
 */
export function getDictData(dictType) {
  return request({
    url: '/system/dict/data/type/' + dictType,
    method: 'get'
  })
}
