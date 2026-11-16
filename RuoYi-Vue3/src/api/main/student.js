import request from '@/utils/request'

// 查询学生建档列表
export function listStudent(query) {
  return request({
    url: '/main/student/list',
    method: 'get',
    params: query
  })
}

// 查询学生建档详细
export function getStudent(studentId) {
  return request({
    url: '/main/student/' + studentId,
    method: 'get'
  })
}

// 新增学生建档
export function addStudent(data) {
  return request({
    url: '/main/student',
    method: 'post',
    data: data
  })
}

// 修改学生建档
export function updateStudent(data) {
  return request({
    url: '/main/student',
    method: 'put',
    data: data
  })
}

// 删除学生建档
export function delStudent(studentId) {
  return request({
    url: '/main/student/' + studentId,
    method: 'delete'
  })
}

// 为学生生成系统账号（单条）
export function generateStudentUser(studentId) {
  return request({
    url: '/main/student/generateUser/' + studentId,
    method: 'post'
  })
}

// 批量为学生生成系统账号
export function batchGenerateStudentUser(studentIds) {
  return request({
    url: '/main/student/generateUser/batch',
    method: 'post',
    data: studentIds
  })
}
