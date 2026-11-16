import request from '@/utils/request'

// 查询办公地点列表
export function listOfficeLocation(query) {
  return request({
    url: '/mobile/officeLocation/list',
    method: 'get',
    params: query
  })
}

// 查询办公地点详情
export function getOfficeLocation(id) {
  return request({
    url: '/mobile/officeLocation/' + id,
    method: 'get'
  })
}

// 新增办公地点
export function addOfficeLocation(data) {
  return request({
    url: '/mobile/officeLocation',
    method: 'post',
    data: data
  })
}

// 修改办公地点
export function updateOfficeLocation(data) {
  return request({
    url: '/mobile/officeLocation',
    method: 'put',
    data: data
  })
}

// 删除办公地点
export function delOfficeLocation(ids) {
  return request({
    url: '/mobile/officeLocation/' + ids,
    method: 'delete'
  })
}
