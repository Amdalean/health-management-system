import request from '@/utils/request'

// 登录方法
export function login(username, password, code, uuid) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    'url': '/login',
    headers: {
      isToken: false
    },
    'method': 'post',
    'data': data
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 微信一键登录（小程序 wx.login 拿 code 换 token，未绑定返回 needBind+openid）
// add by CYQ 2026-09-06 微信小程序一键登录
export function wxLogin(code) {
  return request({
    'url': '/mobile/wx/login/wxLogin',
    headers: {
      isToken: false
    },
    'method': 'post',
    'data': { code }
  })
}

// 微信绑定系统账号并登录（首次使用微信登录时调用）
// add by CYQ 2026-09-06 微信小程序一键登录
export function wxBindLogin(openid, username, password) {
  return request({
    'url': '/mobile/wx/login/bindLogin',
    headers: {
      isToken: false
    },
    'method': 'post',
    'data': { openid, username, password }
  })
}

// 静默检查当前微信是否已绑定（登录页控制一键登录按钮显隐，不签发token）
// add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
export function checkWxBound(code) {
  return request({
    'url': '/mobile/wx/login/checkBound',
    headers: {
      isToken: false
    },
    'method': 'post',
    'data': { code }
  })
}

// 设置页手动关联当前微信（需登录，openid 先调 code2session 换取）
// add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
export function bindWx(openid) {
  return request({
    'url': '/mobile/wx/login/bind',
    'method': 'post',
    'data': { openid }
  })
}

// 设置页手动解绑微信（需登录）
// add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
export function unbindWx() {
  return request({
    'url': '/mobile/wx/login/unbind',
    'method': 'post'
  })
}

// 查询当前登录用户微信绑定状态（设置页展示用，需登录）
// add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
export function getWxBindStatus() {
  return request({
    'url': '/mobile/wx/login/status',
    'method': 'post'
  })
}
// 获取用户详细信息
export function getInfo() {
  return request({
    'url': '/getInfo',
    'method': 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    'url': '/logout',
    'method': 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    'url': '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}
