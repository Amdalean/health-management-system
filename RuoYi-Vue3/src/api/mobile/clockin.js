import request from '@/utils/request'

// 查询打卡记录列表
export function listClockinRecord(query) {
  return request({
    url: '/mobile/clockin/admin/list',
    method: 'get',
    params: query
  })
}

// 查询打卡记录详情
export function getClockinRecord(id) {
  return request({
    url: '/mobile/clockin/admin/' + id,
    method: 'get'
  })
}

// 删除打卡记录
export function delClockinRecord(ids) {
  return request({
    url: '/mobile/clockin/admin/' + ids,
    method: 'delete'
  })
}

// 查询打卡统计
export function listClockinStatistics(params) {
  return request({
    url: '/mobile/clockin/admin/statistics',
    method: 'get',
    params: params
  })
}

// 查询指定日期打卡明细（已打卡/未打卡用户 + 工作日状态）
export function getDailyDetail(date) {
  return request({
    url: '/mobile/clockin/admin/dailyDetail',
    method: 'get',
    params: { date }
  })
}

// 查询指定用户指定日期的全部打卡记录（展开明细）
export function getUserRecords(userId, date) {
  return request({
    url: '/mobile/clockin/admin/userRecords',
    method: 'get',
    params: { userId, date }
  })
}
