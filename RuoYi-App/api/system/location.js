import request from '@/utils/request'

// 上报当前所在城市（App 启动/回到前台时静默调用）
export function reportLocation(data) {
  return request({
    url: '/mobile/location/report',
    method: 'post',
    data: data
  })
}

// 查询当前登录用户最近一次上报的城市
export function getCurrentLocation() {
  return request({
    url: '/mobile/location/current',
    method: 'get'
  })
}
