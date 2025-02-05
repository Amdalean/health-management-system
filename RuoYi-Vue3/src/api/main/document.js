import request from '@/utils/request'

// 查询资料库管理列表
export function listDocument(query) {
  return request({
    url: '/main/document/list',
    method: 'get',
    params: query
  })
}

// 查询资料库管理详细
export function getDocument(id) {
  return request({
    url: '/main/document/' + id,
    method: 'get'
  })
}

// 新增资料库管理
export function addDocument(data) {
  return request({
    url: '/main/document',
    method: 'post',
    data: data
  })
}

// 修改资料库管理
export function updateDocument(data) {
  return request({
    url: '/main/document',
    method: 'put',
    data: data
  })
}

// 删除资料库管理
export function delDocument(id) {
  return request({
    url: '/main/document/' + id,
    method: 'delete'
  })
}
