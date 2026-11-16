/**
 * APP 版本更新管理器
 * add by CYQ 2026-06-06 说明: 统一管理版本检查、应用内下载+进度条、安装
 */
import request from '@/utils/request'
import config from '@/config'
import { reactive } from 'vue'

// 下载保存路径（plus.io 应用私有目录）
const DOWNLOAD_FILENAME = '_downloads/hms_update.apk'
// 标记是否正在检查更新，防止重复触发
let checkingUpdate = false

/**
 * 全局更新状态（Vue 3 reactive 响应式，App.vue 绑定到模板）
 */
const updateState = reactive({
  visible: false,
  downloading: false,
  progress: 0,
  forceUpdate: false,
  version: '',
  error: ''
})

export { updateState }

/**
 * 获取更新状态（兼容旧调用）
 */
export function getUpdateState() {
  return updateState
}

/**
 * 版本号比较: 返回 1(v1>v2) / -1(v1<v2) / 0(相等)
 */
export function compareVersion(v1, v2) {
  const arr1 = v1.split('.').map(Number)
  const arr2 = v2.split('.').map(Number)
  for (let i = 0; i < Math.max(arr1.length, arr2.length); i++) {
    const n1 = arr1[i] || 0
    const n2 = arr2[i] || 0
    if (n1 > n2) return 1
    if (n1 < n2) return -1
  }
  return 0
}

/**
 * 检查更新
 * @param {Object} options
 * @param {boolean} options.showNoUpdateTip - 无更新时是否提示
 * @param {boolean} options.manualCheck    - 是否手动检查（手动检查时不静默）
 * @returns {Promise<{hasUpdate: boolean, latest?: object, forceUpdate?: boolean}>}
 */
export async function checkUpdate(options = {}) {
  const { showNoUpdateTip = false } = options

  // 防止重复检查
  if (checkingUpdate) return { hasUpdate: false }
  checkingUpdate = true

  const currentVersion = getApp().globalData.config.appInfo.version
  const sysInfo = uni.getDeviceInfo()
  const platform = sysInfo.platform === 'android' ? 'android' : (sysInfo.platform === 'ios' ? 'ios' : 'android')

  try {
    const res = await request({
      url: '/mobile/appVersion/latest',
      method: 'get',
      params: { platform },
      headers: { isToken: false }
    })

    if (res.code === 200 && res.data) {
      const latest = res.data
      if (compareVersion(latest.version, currentVersion) > 0) {
        if (latest.forceUpdate === 1) {
          showForceUpdateDialog(latest)
        } else {
          showNormalUpdateDialog(latest)
        }
        checkingUpdate = false
        return { hasUpdate: true, latest, forceUpdate: latest.forceUpdate === 1 }
      }
      if (showNoUpdateTip) {
        uni.showToast({ title: '已是最新版本', icon: 'none' })
      }
    }
  } catch (e) {
    console.error('检查更新失败:', e)
    if (showNoUpdateTip) {
      uni.showToast({ title: '检查更新失败', icon: 'none' })
    }
  }

  checkingUpdate = false
  return { hasUpdate: false }
}

/**
 * 强制更新弹窗（无取消按钮，按返回键则重新弹出）
 */
function showForceUpdateDialog(latest) {
  uni.showModal({
    title: '发现新版本 v' + latest.version,
    content: latest.releaseNotes || '此版本为强制更新，请立即下载安装',
    confirmText: '立即更新',
    cancelText: '',
    success: (res) => {
      if (res.confirm) {
        startDownload(latest)
      } else {
        // 用户通过返回键/点击蒙层关闭，重新弹出
        setTimeout(() => showForceUpdateDialog(latest), 200)
      }
    }
  })
}

/**
 * 普通更新弹窗
 */
function showNormalUpdateDialog(latest) {
  uni.showModal({
    title: '发现新版本 v' + latest.version,
    content: latest.releaseNotes || '已优化部分功能，建议更新',
    confirmText: '立即更新',
    cancelText: '稍后',
    success: (res) => {
      if (res.confirm) {
        startDownload(latest)
      }
    }
  })
}

/**
 * 开始下载APK（应用内下载 + 原生进度弹窗 + 自动安装）
 */
function startDownload(latest) {
  // 非 APP 环境不支持下载安装
  if (typeof plus === 'undefined') {
    uni.showToast({ title: '请在App中更新', icon: 'none' })
    return
  }

  updateState.visible = true
  updateState.downloading = true
  updateState.progress = 0
  updateState.forceUpdate = latest.forceUpdate === 1
  updateState.version = latest.version
  updateState.error = ''

  // 原生进度弹窗
  const waiting = plus.nativeUI.showWaiting('正在下载 0%', {
    modal: true,
    back: 'none'
  })

  const downloadUrl = config.baseUrl + latest.filePath
  console.log('[UpdateManager] 开始下载:', downloadUrl)

  const downloadTask = plus.downloader.createDownload(downloadUrl, {
    filename: DOWNLOAD_FILENAME
  }, (d, status) => {
    console.log('[UpdateManager] 下载完成, status:', status)
    updateState.downloading = false
    waiting.close()

    if (status === 200) {
      updateState.progress = 100
      updateState.visible = false
      installApk(d.filename, latest)
    } else {
      updateState.visible = false
      updateState.error = '下载失败(' + status + ')'
      uni.showToast({ title: '下载失败，请重试', icon: 'none' })
      if (latest.forceUpdate === 1) {
        setTimeout(() => showForceUpdateDialog(latest), 1000)
      }
    }
  })

  // 监听下载进度
  downloadTask.addEventListener('statechanged', (task) => {
    if (task.state === 3 && task.totalSize > 0) {
      const pct = Math.round(task.downloadedSize / task.totalSize * 100)
      updateState.progress = pct
      waiting.setTitle('正在下载 ' + pct + '%')
    }
  })

  downloadTask.start()
}

/**
 * 安装APK
 */
function installApk(filePath, latest) {
  console.log('[UpdateManager] 开始安装:', filePath)
  plus.runtime.install(filePath, {}, () => {
    console.log('[UpdateManager] 安装界面已打开')
  }, (err) => {
    console.error('[UpdateManager] 安装失败:', err)
    uni.showToast({ title: '安装失败，请手动安装', icon: 'none' })
    // 强制更新失败后重新弹出更新弹窗
    if (latest && latest.forceUpdate === 1) {
      setTimeout(() => showForceUpdateDialog(latest), 1000)
    }
  })
}

/**
 * 取消下载
 */
export function cancelDownload() {
  // 注意：plus.downloader 没有全局取消所有下载的简单方法
  // 当前方案通过关闭进度UI来隐藏，但后台下载可能继续
  updateState.visible = false
  updateState.downloading = false

  // 枚举所有下载任务并中止
  const allTasks = plus.downloader.getDownloads()
  for (let i = 0; i < allTasks.length; i++) {
    const task = allTasks[i]
    if (task.state === 3 || task.state === 2) {
      task.abort()
    }
  }
}
