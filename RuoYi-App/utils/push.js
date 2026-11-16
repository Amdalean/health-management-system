/**
 * 推送服务工具类 (UniPush 2.0)
 * add by CYQ 2026-05-17
 * modify by CYQ 2026-06-09 修复：点击通知不再重复写入消息中心
 */
import store from '@/store'
import { getToken } from './auth'
import config from '@/config'

const NOTIFICATION_STORAGE_KEY = 'hms_push_notifications'
const NOTIFICATION_MAX_COUNT = 100

const Push = {
  clientId: '',

  init() {
    // #ifdef APP-PLUS
    try {
      const _this = this
      console.log('[推送] 开始初始化推送服务...')

      if (typeof uni.onPushMessage === 'function') {
        uni.onPushMessage(function(res) {
          console.log('[推送] uni.onPushMessage:', JSON.stringify(res))
          _this._handlePushMessage(res)
        })
        console.log('[推送] uni.onPushMessage 已注册')
      }

      if (typeof plus !== 'undefined' && plus.push) {
        plus.push.addEventListener('click', function(msg) {
          console.log('[推送] plus.push click:', JSON.stringify(msg))
          _this._handlePushMessage({ type: 'click', data: msg })
        }, false)
        plus.push.addEventListener('receive', function(msg) {
          console.log('[推送] plus.push receive:', JSON.stringify(msg))
          _this._handlePushMessage({ type: 'receive', data: msg })
        }, false)
        console.log('[推送] plus.push 事件监听已注册')
      }

      this.getClientId()
    } catch (error) {
      console.error('[推送] 初始化推送失败:', error)
    }
    // #endif
  },

  _handlePushMessage(res) {
    const type = res.type || 'receive'
    const data = res.data || res

    const title = data.title || data.TITLE || data.alert || '推送消息'
    const content = data.content || data.CONTENT || data.body || data.alert || ''
    const payload = typeof data.payload === 'string' ? data.payload : JSON.stringify(data.payload || {})

    console.log('[推送] _handlePushMessage type=' + type + ' title=' + title)

    if (type === 'click') {
      this.onPushClick({ title, content, payload })
    } else {
      this.onPushReceive({ title, content, payload })
    }
  },

  // ========== 通知历史本地存储 ==========

  getNotifications() {
    try {
      const raw = uni.getStorageSync(NOTIFICATION_STORAGE_KEY)
      return raw ? JSON.parse(raw) : []
    } catch (e) {
      return []
    }
  },

  saveNotification(notification) {
    try {
      let list = this.getNotifications()
      list.unshift(notification)
      if (list.length > NOTIFICATION_MAX_COUNT) {
        list = list.slice(0, NOTIFICATION_MAX_COUNT)
      }
      uni.setStorageSync(NOTIFICATION_STORAGE_KEY, JSON.stringify(list))
      this.updateBadge()
    } catch (e) {
      console.error('[推送] 保存通知失败:', e)
    }
  },

  /**
   * 点击通知后，标记匹配的通知为已读
   */
  markAsReadByTitle(title) {
    try {
      let list = this.getNotifications()
      let changed = false
      list = list.map(item => {
        if (!item.read && item.title === title) {
          changed = true
          return { ...item, read: true }
        }
        return item
      })
      if (changed) {
        uni.setStorageSync(NOTIFICATION_STORAGE_KEY, JSON.stringify(list))
        this.updateBadge()
      }
    } catch (e) {}
  },

  markAllRead() {
    try {
      let list = this.getNotifications()
      list = list.map(item => ({ ...item, read: true }))
      uni.setStorageSync(NOTIFICATION_STORAGE_KEY, JSON.stringify(list))
    } catch (e) {}
  },

  clearAll() {
    uni.removeStorageSync(NOTIFICATION_STORAGE_KEY)
    this.updateBadge()
  },

  getUnreadCount() {
    const list = this.getNotifications()
    return list.filter(item => !item.read).length
  },

  updateBadge() {
    // #ifdef APP-PLUS
    try {
      const count = this.getUnreadCount()
      if (typeof plus !== 'undefined' && plus.runtime) {
        plus.runtime.setBadgeNumber(count)
      }
    } catch (e) {}
    // #endif
  },

  // ========== CID管理 ==========

  getClientId() {
    // #ifdef APP-PLUS
    try {
      const _this = this
      uni.getPushClientId({
        success: (res) => {
          if (res && res.cid) {
            _this.clientId = res.cid
            console.log('[推送] CID:', _this.clientId)
            _this.reportDevice()
          } else {
            _this._getClientIdFallback()
          }
        },
        fail: (err) => {
          console.warn('[推送] getPushClientId 失败:', JSON.stringify(err))
          _this._getClientIdFallback()
        }
      })
    } catch (error) {
      console.error('[推送] 获取CID异常:', error)
    }
    // #endif
  },

  _getClientIdFallback() {
    // #ifdef APP-PLUS
    const _this = this
    setTimeout(() => {
      try {
        plus.push.getClientInfo(function(info) {
          if (info && info.clientid) {
            _this.clientId = info.clientid
            console.log('[推送] [兜底] CID:', _this.clientId)
            _this.reportDevice()
          }
        }, function(err) {
          console.error('[推送] [兜底] 获取CID失败:', JSON.stringify(err))
        })
      } catch (error) {
        console.error('[推送] [兜底] 异常:', error)
      }
    }, 1500)
    // #endif
  },

  reportDevice() {
    if (!this.clientId) return
    if (!getToken()) {
      console.log('[推送] 未登录，暂不上报设备')
      return
    }

    uni.request({
      url: config.baseUrl + '/mobile/push/device',
      method: 'POST',
      header: {
        'Authorization': 'Bearer ' + getToken(),
        'Content-Type': 'application/json'
      },
      data: {
        clientId: this.clientId,
        platform: 'android',
        appVersion: config.appInfo.version || '1.3.2'
      },
      success: (res) => {
        if (res.data.code === 200) console.log('[推送] 设备上报成功')
      },
      fail: (err) => {
        console.error('[推送] 设备上报失败:', JSON.stringify(err))
      }
    })
  },

  // ========== 事件回调 ==========

  /**
   * 点击通知栏消息：只标记已读 + 跳转，不再写入新记录
   */
  onPushClick(msg) {
    console.log('[推送] 点击通知:', msg.title)

    // 标记对应通知为已读
    this.markAsReadByTitle(msg.title)

    // 跳转
    try {
      const p = msg.payload ? JSON.parse(msg.payload) : null
      if (p && p.page) {
        uni.navigateTo({ url: p.page })
      }
    } catch (e) {}
  },

  /**
   * 收到在线推送：写入消息中心
   */
  onPushReceive(msg) {
    console.log('[推送] 收到在线消息:', msg.title)
    this.saveNotification({
      id: Date.now(),
      title: msg.title || '推送消息',
      content: msg.content || '',
      payload: msg.payload || '',
      type: 'receive',
      read: false,
      time: new Date().toISOString()
    })
  }
}

export default Push
