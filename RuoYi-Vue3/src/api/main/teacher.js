import request from '@/utils/request'

// 查询教师建档列表
export function listTeacher(query) {
  return request({
    url: '/main/teacher/list',
    method: 'get',
    params: query
  })
}

// 查询教师建档详细
export function getTeacher(teacherId) {
  return request({
    url: '/main/teacher/' + teacherId,
    method: 'get'
  })
}

// 新增教师建档
export function addTeacher(data) {
  return request({
    url: '/main/teacher',
    method: 'post',
    data: data
  })
}

// 修改教师建档
export function updateTeacher(data) {
  return request({
    url: '/main/teacher',
    method: 'put',
    data: data
  })
}

// 删除教师建档
export function delTeacher(teacherId) {
  return request({
    url: '/main/teacher/' + teacherId,
    method: 'delete'
  })
}

// 为教师生成系统账号（单条）
export function generateTeacherUser(teacherId) {
  return request({
    url: '/main/teacher/generateUser/' + teacherId,
    method: 'post'
  })
}

// 批量为教师生成系统账号
export function batchGenerateTeacherUser(teacherIds) {
  return request({
    url: '/main/teacher/generateUser/batch',
    method: 'post',
    data: teacherIds
  })
}
