import request from '@/utils/request'

// 查询课程建档列表
export function listCourse(query) {
  return request({
    url: '/main/course/list',
    method: 'get',
    params: query
  })
}

// 课程下拉选项
export function courseOptions() {
  return request({
    url: '/main/course/options',
    method: 'get'
  })
}

// 查询课程建档详细
export function getCourse(courseId) {
  return request({
    url: '/main/course/' + courseId,
    method: 'get'
  })
}

// 新增课程建档
export function addCourse(data) {
  return request({
    url: '/main/course',
    method: 'post',
    data: data
  })
}

// 修改课程建档
export function updateCourse(data) {
  return request({
    url: '/main/course',
    method: 'put',
    data: data
  })
}

// 删除课程建档
export function delCourse(courseId) {
  return request({
    url: '/main/course/' + courseId,
    method: 'delete'
  })
}

// 查询课程已选学生
export function listCourseStudents(courseId) {
  return request({
    url: '/main/course/' + courseId + '/students',
    method: 'get'
  })
}

// 查询课程选课候选学生
export function listCandidates(courseId) {
  return request({
    url: '/main/course/' + courseId + '/candidates',
    method: 'get'
  })
}

// 批量选课
export function assignStudents(courseId, studentIds) {
  return request({
    url: '/main/course/' + courseId + '/students',
    method: 'post',
    data: studentIds
  })
}

// 批量退课
export function removeStudents(courseId, studentIds) {
  return request({
    url: '/main/course/' + courseId + '/students/' + studentIds,
    method: 'delete'
  })
}
