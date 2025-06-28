import request from '@/utils/request'

// 查询财务汇总主列表
export function listSummary(query) {
    return request({
        url: '/main/summary/list',
        method: 'get',
        params: query
    })
}

// 查询财务汇总主详细
export function getSummary(id) {
    return request({
        url: '/main/summary/' + id,
        method: 'get'
    })
}
//查询上月数据
export function initSummary() {
    return request({
        url: '/main/summary/init/',
        method: 'get'
    })
}
// 新增财务汇总主
export function addSummary(data) {
    return request({
        url: '/main/summary',
        method: 'post',
        data: data
    })
}

// 修改财务汇总主
export function updateSummary(data) {
    return request({
        url: '/main/summary',
        method: 'put',
        data: data
    })
}

// 删除财务汇总主
export function delSummary(id) {
    return request({
        url: '/main/summary/' + id,
        method: 'delete'
    })
}
// 财务报表弹窗
export function formsSummary(query) {
    return request({
        url: '/main/summary/checkExpense',
        method: 'get'
    })
}

// 存款计划执行情况
export function getDepositPlanData() {
    return request({
        url: '/main/summary/depositPlan',
        method: 'get'
    })
}

// 存款预测数据
export function getDepositPrediction() {
    return request({
        url: '/main/summary/depositPrediction',
        method: 'get'
    })
}
