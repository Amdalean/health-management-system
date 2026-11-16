import request from '@/utils/request'

// 打卡上报
export function clockin(data) {
  return request({
    url: '/mobile/clockin',
    method: 'post',
    data: data
  })
}

// 获取打卡记录列表
export function getClockinList() {
  return request({
    url: '/mobile/clockin/list',
    method: 'get'
  })
}

// 获取打卡记录详情
export function getClockinDetail(id) {
  return request({
    url: '/mobile/clockin/' + id,
    method: 'get'
  })
}

// 获取启用的办公地点列表
export function getOfficeLocations() {
  return request({
    url: '/mobile/clockin/officeLocations',
    method: 'get'
  })
}

// 小程序登录：code 换 openid
export function code2session(code) {
  return request({
    url: '/mobile/wx/subscribe/code2session',
    method: 'post',
    data: { code }
  })
}

// 绑定 openid 到当前用户
export function bindOpenid(openid) {
  return request({
    url: '/mobile/wx/subscribe/bindOpenid',
    method: 'post',
    data: { openid }
  })
}
