/**
 * HMS 闪控球 — JS 端封装
 * add by CYQ 2026-06-22
 *
 * 鸿蒙系统级闪控球（floatingBall API + screenshot.capture）
 * 通过 UTS 插件实现，直接 import 导出函数
 */

// #ifdef APP-HARMONY
import {
  startFlashBall as utsStartFlashBall,
  stopFlashBall as utsStopFlashBall,
  isFlashBallShowing as utsIsFlashBallShowing,
  checkFlashBallClicked as utsCheckFlashBallClicked
} from '@/uni_modules/hms-flash-ball'
// #endif

export function startFlashBall() {
  // #ifdef APP-HARMONY
  return utsStartFlashBall()
  // #endif
  // #ifndef APP-HARMONY
  return Promise.reject({ code: -1, msg: '当前平台不支持闪控球' })
  // #endif
}

export function stopFlashBall() {
  // #ifdef APP-HARMONY
  return utsStopFlashBall()
  // #endif
  // #ifndef APP-HARMONY
  return Promise.resolve({ code: 0, msg: '无需停止' })
  // #endif
}

export function isFlashBallShowing() {
  // #ifdef APP-HARMONY
  try {
    const result = utsIsFlashBallShowing()
    return result && result.showing
  } catch (e) { return false }
  // #endif
  return false
}

/**
 * 检查闪控球是否被点击过（消费后清除）
 * 返回 { clicked: boolean, screenshotPath: string }
 * 如果有截图，screenshotPath 为全屏截图的本地路径
 */
export function checkFlashBallClicked() {
  // #ifdef APP-HARMONY
  try {
    return utsCheckFlashBallClicked()
  } catch (e) {
    return { clicked: false, screenshotPath: '' }
  }
  // #endif
  return { clicked: false, screenshotPath: '' }
}

export default { startFlashBall, stopFlashBall, isFlashBallShowing, checkFlashBallClicked }
