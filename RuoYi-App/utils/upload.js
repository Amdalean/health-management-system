import config from '@/config'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { toast, tansParams } from '@/utils/common'
import { handleSessionExpired, promptRelogin } from '@/utils/wxAuth'

let timeout = 300000 // 默认5分钟超时，支持大文件上传
const baseUrl = config.baseUrl

export default function upload(config) {
  // 是否需要设置 token
  const isToken = (config.headers || {}).isToken === false
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
    uni.uploadFile({
      timeout: config.timeout || timeout,
      url: baseUrl + config.url,
      filePath: config.filePath,
      name: config.name || 'file',
      header: config.header,
      formData: config.formData,
      success: (res) => {
        let result = JSON.parse(res.data)
        const code = result.code || 200
        const msg = errorCode[code] || result.msg || errorCode['default']
        if (code === 200) {
          resolve(result)
        } else if (code == 401) {
          const expiredMsg = '无效的会话，或者会话已过期，请重新登录。'
          // 会话过期：小程序端先尝试「已绑定微信」静默续登，成功则重传本文件
          // add by CYQ 2026-11-14 已绑定微信的会话过期自动续登
          if (isToken) {
            // 匿名上传 401 不弹框，交给调用方处理
            reject(expiredMsg)
            return
          }
          if (config._wxRetried) {
            promptRelogin()
            reject(expiredMsg)
            return
          }
          handleSessionExpired().then((renewed) => {
            if (!renewed) {
              reject(expiredMsg)
              return
            }
            const retryConfig = Object.assign({}, config, { params: undefined, _wxRetried: true })
            upload(retryConfig).then(resolve).catch(reject)
          })
        } else if (code === 500) {
          toast(msg)
          reject('500')
        } else if (code !== 200) {
          toast(msg)
          reject(code)
        }
      },
      fail: (error) => {
        let { message } = error
        if (message == 'Network Error') {
          message = '后端接口连接异常'
        } else if (message.includes('timeout')) {
          message = '系统接口请求超时'
        } else if (message.includes('Request failed with status code')) {
          message = '系统接口' + message.substr(message.length - 3) + '异常'
        }
        toast(message)
        reject(error)
      }
    })
  })
}


