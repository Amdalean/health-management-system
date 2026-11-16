import store from '@/store'
import { getToken, removeToken } from '@/utils/auth'
import { showConfirm } from '@/utils/common'

/**
 * 微信小程序静默续登工具
 *
 * 场景：token 过期（后端返回 401）时，如果当前微信已经绑定过系统账号，
 *       直接 wx.login 换 code 让后端按 openid 换发新 token，用户无感知，
 *       不再弹「登录状态已过期」确认框。
 *
 * 约束：
 * 1. 仅在 MP-WEIXIN 且本地仍残留旧 token 时生效（主动退出登录后不自动登回来）
 * 2. 并发去重：多个请求同时 401 只发起一次静默登录
 * 3. 失败后 60 秒内不再重试，避免网络异常时反复 wx.login + 弹框
 *
 * add by CYQ 2026-11-14 已绑定微信的会话过期自动续登
 */

// 并发去重用的进行中任务
let pendingTask = null
// 上次静默续登失败时间（毫秒），用于失败冷却
let lastFailAt = 0
// 冷却时长：60 秒
const FAIL_COOLDOWN = 60 * 1000
// 「重新登录」确认框是否正在显示，避免多个 401 叠加弹框
let promptShowing = false

/**
 * 当前环境是否允许静默微信续登
 * @returns {Boolean}
 */
export function canSilentWxLogin() {
  // #ifdef MP-WEIXIN
  // 有旧 token 说明是「会话过期」而不是「主动退出」，只有这种情况才自动续登
  return !!getToken() && (Date.now() - lastFailAt > FAIL_COOLDOWN)
  // #endif
  // #ifndef MP-WEIXIN
  return false
  // #endif
}

/**
 * 静默微信续登：wx.login → 后端按 openid 查绑定用户发 token
 * @returns {Promise<Object>} 成功时 resolve 后端返回（含新 token）
 */
export function silentWxLogin() {
  if (!canSilentWxLogin()) {
    return Promise.reject(new Error('当前不满足静默续登条件'))
  }
  if (pendingTask) {
    return pendingTask
  }
  const task = new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.login({
      success: (loginRes) => {
        if (!loginRes.code) {
          reject(new Error('获取微信登录凭证失败'))
          return
        }
        store.dispatch('SilentWxLogin', loginRes.code).then(resolve).catch(reject)
      },
      fail: () => reject(new Error('唤起微信登录失败'))
    })
    // #endif
    // #ifndef MP-WEIXIN
    reject(new Error('当前平台不支持微信静默登录'))
    // #endif
  })
  pendingTask = task.then(
    (res) => {
      pendingTask = null
      lastFailAt = 0
      return res
    },
    (err) => {
      pendingTask = null
      lastFailAt = Date.now()
      throw err
    }
  )
  return pendingTask
}

/**
 * 会话过期统一处理：先尝试静默续登，失败再弹「重新登录」确认框
 * @returns {Promise<Boolean>} true=已续登成功（调用方可重放原请求），false=需要人工登录
 */
export function handleSessionExpired() {
  return silentWxLogin().then(
    () => {
      console.log('[wxAuth] 静默续登成功，已换发新 token')
      return true
    },
    (err) => {
      console.log('[wxAuth] 静默续登不可用，转为人工登录:', err && err.message)
      promptRelogin()
      return false
    }
  )
}

/**
 * 弹出「登录状态已过期」确认框（非小程序端 / 静默续登失败时的兜底）
 */
export function promptRelogin() {
  if (promptShowing) return
  promptShowing = true
  // release 在跳转/取消时统一释放，避免标志位卡死导致后续过期永不提示
  const release = () => {
    promptShowing = false
  }
  showConfirm('登录状态已过期，您可以继续留在该页面，或者重新登录?').then((res) => {
    if (!res.confirm) {
      release()
      return
    }
    const goLogin = () => {
      release()
      uni.reLaunch({ url: '/pages/login' })
    }
    // 保持 promptShowing=true 直到跳转完成，防止 /logout 自身 401 再叠一层弹框
    store.dispatch('LogOut').then(goLogin).catch(() => {
      // 退出接口本身也 401/断网时，本地也要清干净，避免停在半登录态
      store.commit('SET_TOKEN', '')
      removeToken()
      goLogin()
    })
  }).catch(release)
}
