/**
 * HMS 悬浮窗截屏识图 — JS 端封装
 * add by CYQ 2026-06-01
 *
 * 功能：
 * 1. 启动/停止系统级悬浮窗
 * 2. 双击悬浮按钮触发截屏
 * 3. 截屏后自动调用 AI 识图 API
 * 4. 识别成功后跳转随手记新增页并自动填充
 *
 * 平台差异：
 * - Android: 使用系统级悬浮窗 + MediaProjection 截屏（需原生插件）
 * - HarmonyOS: 使用独立的闪控球插件（flash-ball.js）
 */

import { aiRecognizeJotdown } from '@/api/main/jotdown'

const PLUGIN_ID = 'HMS-FloatWindow'

// ======================== 核心 API ========================

/**
 * 获取原生插件实例（仅 Android 使用）
 */
function getPlugin() {
  // #ifdef APP-PLUS
  try {
    const plugin = uni.requireNativePlugin(PLUGIN_ID)
    console.log('[FloatWindow] getPlugin result:', plugin ? 'OK' : 'NULL', 'type:', typeof plugin)
    if (plugin) {
      console.log('[FloatWindow] plugin keys:', Object.keys(plugin))
    }
    return plugin
  } catch (e) {
    console.warn('[FloatWindow] 原生插件未加载:', e)
    return null
  }
  // #endif
  return null
}

/**
 * 启动悬浮窗
 * @param {Object} options { autoRecognize?: boolean }
 * @returns {Promise<{code: number, msg: string}>}
 */
export function startFloatWindow(options = {}) {
  console.log('[FloatWindow] startFloatWindow called, options:', JSON.stringify(options))

  // #ifdef APP-HARMONY
  // 鸿蒙使用独立的闪控球插件，此处返回不支持
  return Promise.reject({ code: -1, msg: '鸿蒙请使用闪控球（flash-ball.js）' })
  // #endif

  // #ifndef APP-HARMONY
  return new Promise((resolve, reject) => {
    const plugin = getPlugin()
    console.log('[FloatWindow] startFloatWindow plugin:', plugin ? 'got instance' : 'NULL')
    if (!plugin) {
      reject({ code: -99, msg: '当前平台不支持悬浮窗' })
      return
    }

    // Android 原生模块使用 callback 模式
    plugin.start(
      { autoScreenshot: true, ...options },
      (result) => {
        if (result.code === 0) {
          resolve(result)
        } else {
          reject(result)
        }
      }
    )
  })
  // #endif
}

/**
 * 停止悬浮窗
 */
export function stopFloatWindow() {
  // #ifdef APP-HARMONY
  // 鸿蒙使用独立的闪控球插件
  return Promise.resolve({ code: 0, msg: '鸿蒙请使用闪控球' })
  // #endif

  // #ifndef APP-HARMONY
  return new Promise((resolve) => {
    const plugin = getPlugin()
    if (!plugin) {
      resolve({ code: 0, msg: '无需停止' })
      return
    }

    // Android 原生模块使用 callback 模式
    plugin.stop({}, (result) => {
      resolve(result)
    })
  })
  // #endif
}

/**
 * 检查悬浮窗是否正在显示
 */
export function isFloatWindowShowing() {
  // #ifdef APP-HARMONY
  return false
  // #endif

  // #ifndef APP-HARMONY
  const plugin = getPlugin()
  if (!plugin) return false
  const result = plugin.isShowing()
  return result && result.showing
  // #endif
}

/**
 * 检查悬浮窗权限
 */
export function checkFloatWindowPermission() {
  return new Promise((resolve) => {
    // #ifdef APP-HARMONY
    resolve({ granted: false })
    // #endif

    // #ifndef APP-HARMONY
    const plugin = getPlugin()
    if (!plugin) {
      resolve({ granted: false })
      return
    }
    const result = plugin.checkPermission()
    resolve(result)
    // #endif
  })
}

// ======================== 截屏识图流程 ========================

/**
 * 获取截屏结果
 * @returns {{code: number, imageBase64?: string, msg?: string}}
 */
export function getScreenshotResult() {
  const plugin = getPlugin()
  if (!plugin) return { code: -99, msg: '插件未加载' }
  return { code: -1, msg: '当前平台不支持此接口' }
}

// 截屏回调句柄
let screenshotCallbackId = null

/**
 * 注册截屏回调
 * @param {Function} onResult 结果回调 ({ success, data, error })
 */
export function registerScreenshotHandler(onResult) {
  // #ifdef APP-PLUS
  const plugin = getPlugin()
  if (!plugin) return
  startScreenshotPolling(onResult)
  // #endif
}

/**
 * 主动触发截屏
 */
export function captureScreenshot() {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    const plugin = getPlugin()
    if (!plugin) {
      reject({ code: -99, msg: '不支持' })
      return
    }
    reject({ code: -1, msg: '请切换到后台，双击悬浮按钮截屏' })
    // #endif

    // #ifdef APP-HARMONY
    reject({ code: -1, msg: '请使用闪控球截屏' })
    // #endif
  })
}

// ======================== 内部实现 ========================

let pollingTimer = null

function startScreenshotPolling(onResult) {
  stopScreenshotPolling()
  // #ifdef APP-PLUS
  if (typeof plus !== 'undefined' && plus.globalEvent) {
    // 原生可通过 plus.globalEvent 发送事件
  }
  // #endif
}

function stopScreenshotPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

/**
 * 处理截屏结果 — 调用 AI 识图
 * @param {string} imageBase64 截屏 base64
 * @returns {Promise<Object>} 识别结果
 */
export async function processScreenshot(imageBase64) {
  try {
    const result = await aiRecognizeJotdownBase64(imageBase64)
    return { success: true, data: result }
  } catch (e) {
    console.error('[FloatWindow] AI 识图失败:', e)
    return { success: false, error: e.message || '识图失败' }
  }
}

/**
 * 使用 base64 直接调用 AI 识图
 */
function aiRecognizeJotdownBase64(base64Data) {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    const bitmap = new plus.nativeObj.Bitmap('screenshot_temp')
    bitmap.loadBase64Data(base64Data, () => {
      const tempPath = '_doc/screenshot_temp.jpg'
      bitmap.save(tempPath, { format: 'jpg', quality: 85 }, async () => {
        bitmap.clear()
        try {
          const filePath = plus.io.convertLocalFileSystemURL(tempPath)
          const result = await aiRecognizeJotdown(filePath)
          resolve(result)
        } catch (e) {
          reject(e)
        }
      }, (err) => {
        bitmap.clear()
        reject(new Error('保存截图失败: ' + JSON.stringify(err)))
      })
    }, (err) => {
      reject(new Error('加载截图失败: ' + JSON.stringify(err)))
    })
    // #endif
    // #ifndef APP-PLUS
    reject(new Error('平台不支持'))
    // #endif
  })
}

/**
 * 跳转随手记新增页并填充识别结果
 * @param {Object} data AI 识图返回的数据
 */
export function navigateToJotdownWithData(data) {
  const formData = encodeURIComponent(JSON.stringify(data))
  uni.navigateTo({
    url: `/pages/jotdown/add?aiData=${formData}`
  })
}

export default {
  startFloatWindow,
  stopFloatWindow,
  isFloatWindowShowing,
  checkFloatWindowPermission,
  getScreenshotResult,
  registerScreenshotHandler,
  captureScreenshot,
  processScreenshot,
  navigateToJotdownWithData
}
