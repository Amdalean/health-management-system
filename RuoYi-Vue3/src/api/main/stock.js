import request from '@/utils/request'

// 持仓概览
export function getOverview() {
  return request({ url: '/main/stock/overview', method: 'get' })
}

// 持仓列表
export function getHoldings() {
  return request({ url: '/main/stock/holdings', method: 'get' })
}

// 操作建议（分页）
export function getRecommendations(params) {
  return request({ url: '/main/stock/recommendations', method: 'get', params })
}

// 手动触发分析
export function runAnalysis() {
  return request({ url: '/main/stock/analyze', method: 'post' })
}

// 标记建议已执行（可传实际成交单价/股数，缺省回退到建议值）
export function executeRecommendation(id, price, shares) {
  return request({ url: '/main/stock/execute/' + id, method: 'put', params: { price, shares } })
}

// 交易记录
export function getTradeLog() {
  return request({ url: '/main/stock/tradeLog', method: 'get' })
}

// 组合权益曲线
export function getPerformance() {
  return request({ url: '/main/stock/performance', method: 'get' })
}

// K 线数据（含 MA 信号）
export function getKlineData(symbol, year) {
  return request({ url: '/main/stock/kline/' + symbol, method: 'get', params: { year } })
}

// 回测任务状态（读任务日志，回测在定时任务管理手动执行）
export function getBacktestStatus() {
  return request({ url: '/main/stock/backtest/status', method: 'get' })
}

// 因子任务状态（最新 run / 运行中 / 数据新鲜度 / 结论计数）
export function getFactorStatus() {
  return request({ url: '/main/stock/factor/status', method: 'get' })
}

// 因子结论列表（runId 缺省取最新）
export function getFactorEvals(runId) {
  return request({ url: '/main/stock/factor/evals', method: 'get', params: { runId } })
}

// 因子报告载荷（kind 缺省 summary）
export function getFactorReport(runId, kind) {
  return request({ url: '/main/stock/factor/report', method: 'get', params: { runId, kind } })
}

// 因子运行历史
export function getFactorRuns(limit = 20) {
  return request({ url: '/main/stock/factor/runs', method: 'get', params: { limit } })
}

// 组合级回测结果（单一共享现金账户：初始10w → 期末总资产 + 净值曲线 + 期末持仓）
export function getBacktestPortfolio(runId) {
  return request({ url: '/main/stock/backtestPortfolio', method: 'get', params: { runId } })
}

// 组合级回测历史批次（每用户最近 10 次）
export function getBacktestPortfolioRuns() {
  return request({ url: '/main/stock/backtestPortfolio/runs', method: 'get' })
}

// 指定批次的组合逐笔成交（不传 runId 取最新一次）
export function getBacktestPortfolioTrades(runId) {
  return request({ url: '/main/stock/backtestPortfolio/trades', method: 'get', params: { runId } })
}

// 系统运行状态
export function getSystemStatus() {
  return request({ url: '/main/stock/systemStatus', method: 'get' })
}

// 新增持仓
export function addHolding(data) {
  return request({ url: '/main/stock/holding', method: 'post', params: data })
}

// 更新持仓
export function updateHolding(symbol, data) {
  return request({ url: '/main/stock/holding/' + symbol, method: 'put', params: data })
}

// 删除持仓
export function deleteHolding(symbol) {
  return request({ url: '/main/stock/holding/' + symbol, method: 'delete' })
}

// 搜索已下载标的（支持非持仓，基于 stock_daily_quote + stock_basic_cache）
export function searchSymbols(query, limit = 20) {
  return request({ url: '/main/stock/symbols/search', method: 'get', params: { q: query, limit } })
}

// 全 A 股代码/名称互查（新增持仓自动带出另一项）
export function lookupSymbols(query, limit = 10) {
  return request({ url: '/main/stock/symbols/lookup', method: 'get', params: { q: query, limit } })
}

// 手工调整现金：amount 正数=入金、负数=出金
export function adjustCash(amount, note) {
  return request({ url: '/main/stock/cash/adjust', method: 'post', params: { amount, note } })
}

// 现金流水（近 N 条）
export function getCashLog(limit = 50) {
  return request({ url: '/main/stock/cash/log', method: 'get', params: { limit } })
}

// 持仓顾问生效参数（读 veighNa/config.json，与 Python 顾问同源）
export function getAdvisorParams() {
  return request({ url: '/main/stock/advisorParams', method: 'get' })
}
