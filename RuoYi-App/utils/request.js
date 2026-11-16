import config from '@/config'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { toast, tansParams } from '@/utils/common'
import { handleSessionExpired, promptRelogin } from '@/utils/wxAuth'

let timeout = 10000
const baseUrl = config.baseUrl

const request = config => {
  // 是否需要设置 token
  const isToken = (config.headers || {}).isToken === false
  // silent: true 时业务自行处理失败（如转链失败回退原链接），不弹全局错误提示
  const silent = config.silent === true
  config.header = config.header || {}
  if (getToken() && !isToken) {
    config.header['Authorization'] = 'Bearer ' + getToken()
  }
  // get请求映射params参数
  if (config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.url = url
  }
  return new Promise((resolve, reject) => {
    uni.request({
        method: config.method || 'get',
        timeout: config.timeout ||  timeout,
        url: config.baseUrl || baseUrl + config.url,
        data: config.data,
        header: config.header,
        dataType: 'json'
      }).then(response => {
        // vue3 不再返回数组结构，失败直接进 catch
        let res = response
        const code = res.data.code || 200
        // 仅报错时打印（正常 200 静默，避免刷屏）
        if (code !== 200) {
          const _raw = (() => { try { return JSON.stringify(res.data) } catch(e) { return String(res.data) } })()
          let _summary = _raw
          if (_raw.length > 2000) {
            if (res.data && Array.isArray(res.data.rows)) {
              const sample = res.data.rows[0] ? JSON.stringify(res.data.rows[0]).slice(0, 300) : ''
              _summary = `{code:${res.data.code},msg:"${res.data.msg}",total:${res.data.total},rows:${res.data.rows.length} items,${_raw.length} chars} sample:${sample}...`
            } else {
              _summary = _raw.slice(0, 2000) + `...(${_raw.length} chars 已截断)`
            }
          }
          console.error('[request] 报错:', config.url, 'code:', code, 'data:', _summary)
        }
        const msg = errorCode[code] || res.data.msg || errorCode['default']
        if (code === 401) {
          // 匿名请求(isToken:false，如登录页的 checkBound)在未登录态调通，无需弹“过期”框
          // 否则登录页静默探测失败会误伤用户；直接 reject 由各调用方静默处理
          // add by CYQ 2026-09-09 修复登录页静默请求 401 误弹确认框
          const expiredMsg = '无效的会话，或者会话已过期，请重新登录。'
          if (isToken) {
            reject(new Error(expiredMsg))
            return
          }
          // 已登录请求 401：小程序端先尝试用「已绑定微信」静默换发新 token
          // 成功则重放本次请求，用户无感知；失败/非小程序才弹「登录状态已过期」
          // add by CYQ 2026-11-14 已绑定微信的会话过期自动续登
          if (config.url === '/logout' || config._wxRetried) {
            promptRelogin()
            reject(new Error(expiredMsg))
            return
          }
          handleSessionExpired().then((renewed) => {
            if (!renewed) {
              reject(new Error(expiredMsg))
              return
            }
            console.log('[request] 静默续登成功，重放请求:', config.url)
            // params 已在首轮拼进 url，重放时清掉避免重复拼接
            const retryConfig = Object.assign({}, config, { params: undefined, _wxRetried: true })
            request(retryConfig).then(resolve).catch(reject)
          })
          return
        } else if (code === 500) {
          if (!silent) toast(msg)
          reject(new Error(msg || '系统错误'))
        } else if (code === 403) {
          if (!silent) toast(msg)
          reject(new Error(msg || '没有权限访问此资源'))
        } else if (code !== 200) {
          if (!silent) toast(msg)
          reject(new Error(msg || `请求失败(${code})`))
        }
        resolve(res.data)
      })
      .catch(error => {
        console.error('[request] 网络异常:', config.url, error)
        let { message } = error
        if (message === 'Network Error') {
          message = '后端接口连接异常'
        } else if (message.includes('timeout')) {
          message = '系统接口请求超时'
        } else if (message.includes('Request failed with status code')) {
          message = '系统接口' + message.substr(message.length - 3) + '异常'
        }
        if (!silent) toast(message)
        reject(error)
      })
  })
}

export default request
