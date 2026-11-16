/**
 * 微信小程序隐私协议授权工具
 * add by CYQ 2026-10-16 统一处理小程序隐私接口调用前的隐私授权
 *
 * 背景：自 2023-10-17 起，调用隐私接口（chooseImage / chooseMedia / chooseVideo /
 * getLocation / startRecord / getClipboardData 等）前，必须由用户同意
 * 《小程序用户隐私保护指引》。
 *  - 若指引中【未声明】对应信息类型，接口会直接失败并返回
 *    errno 112：「api scope is not declared in the privacy agreement」。
 *  - 若已声明但用户【尚未同意】，则触发 onNeedPrivacyAuthorization（或官方隐私弹窗）。
 *
 * 常见信息类型对照：
 *  - chooseImage / chooseMedia / chooseVideo → 「收集你选中的照片或视频信息」
 *  - getLocation / authorize(userLocation)   → 「收集你的位置信息」
 */

/** 当前运行环境（微信小程序）是否支持隐私接口 */
export function supportsPrivacyApi() {
  return typeof wx !== 'undefined' && typeof wx.requirePrivacyAuthorize === 'function'
}

/**
 * 确保已获得隐私授权，可在调用任意隐私接口前 await
 * @returns {Promise<{ ok: boolean, supported: boolean, err?: any }>}
 */
export function ensurePrivacyAuthorized() {
  return new Promise((resolve) => {
    if (!supportsPrivacyApi()) {
      // 非微信小程序，或基础库过低：直接放行
      resolve({ ok: true, supported: false })
      return
    }
    wx.requirePrivacyAuthorize({
      success: () => resolve({ ok: true, supported: true }),
      fail: (err) => resolve({ ok: false, supported: true, err })
    })
  })
}

/**
 * 判断是否为「接口未在隐私保护指引中声明」错误（errno 112）
 * 该错误只能通过小程序后台补充声明解决，代码无法绕过
 */
export function isPrivacyScopeError(err) {
  if (!err) return false
  const msg = err.errMsg || err.message || ''
  return err.errno === 112 || msg.indexOf('privacy agreement') > -1
}

/** 打开《小程序用户隐私保护指引》页面（仅微信小程序有效） */
export function openPrivacyContract() {
  if (typeof wx !== 'undefined' && typeof wx.openPrivacyContract === 'function') {
    wx.openPrivacyContract({})
  }
}
