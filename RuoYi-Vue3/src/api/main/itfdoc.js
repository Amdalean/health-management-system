import request from '@/utils/request'

// 查询接口档案列表
export function listItfdoc(query) {
  return request({
    url: '/main/itfdoc/list',
    method: 'get',
    params: query
  })
}

// 查询接口档案详细
export function getItfdoc(pkItfdoc) {
  return request({
    url: '/main/itfdoc/' + pkItfdoc,
    method: 'get'
  })
}

// 新增接口档案
export function addItfdoc(data) {
  return request({
    url: '/main/itfdoc',
    method: 'post',
    data: data
  })
}

// 修改接口档案
export function updateItfdoc(data) {
  return request({
    url: '/main/itfdoc',
    method: 'put',
    data: data
  })
}

// 删除接口档案
export function delItfdoc(pkItfdoc) {
  return request({
    url: '/main/itfdoc/' + pkItfdoc,
    method: 'delete'
  })
}
