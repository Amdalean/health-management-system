import { getToken } from '@/utils/auth'

// 登录页面
const loginPage = "/pages/login"
  
// 页面白名单（未登录可访问：登录/注册/微信绑定）
const whiteList = [
  '/pages/login', '/pages/register', '/pages/wxbind', '/pages/common/webview/index',
  // 淘宝优惠查询：面向小程序用户的公开工具页，无需登录
  '/pages/tbk/index'
]

// 检查地址白名单
function checkWhite(url) {
  const path = url.split('?')[0]
  return whiteList.indexOf(path) !== -1
}

// 页面跳转验证拦截器
let list = ["navigateTo", "redirectTo", "reLaunch", "switchTab"]
list.forEach(item => {
  uni.addInterceptor(item, {
    invoke(to) {
      const token = getToken()
      console.log('[permission] 拦截', item, '→', to.url, 'token:', token ? '有值' : '为空')
      if (token) {
        if (to.url === loginPage) {
          uni.reLaunch({ url: "/" })
        }
        return true
      } else {
        if (checkWhite(to.url)) {
          return true
        }
        console.log('[permission] token为空，跳转登录页，来源:', to.url)
        uni.reLaunch({ url: loginPage })
        return false
      }
    },
    fail(err) {
      console.log(err)
    }
  })
})
