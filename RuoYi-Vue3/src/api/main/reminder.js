import request from '@/utils/request'

// 查询提醒管理列表
export function listReminder(query) {
  return request({
    url: '/main/reminder/list',
    method: 'get',
    params: query
  })
}

// 查询提醒管理详细
export function getReminder(id) {
  return request({
    url: '/main/reminder/' + id,
    method: 'get'
  })
}

// 新增提醒管理
export function addReminder(data) {
  return request({
    url: '/main/reminder',
    method: 'post',
    data: data
  })
}

// 修改提醒管理
export function updateReminder(data) {
  return request({
    url: '/main/reminder',
    method: 'put',
    data: data
  })
}

// 删除提醒管理
export function delReminder(id) {
  return request({
    url: '/main/reminder/' + id,
    method: 'delete'
  })
}
