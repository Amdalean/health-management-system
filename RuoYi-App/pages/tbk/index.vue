<template>
  <view class="tbk-page">
    <view class="hero">
      <view class="hero-badge">省</view>
      <view class="hero-title">淘宝 / 拼多多优惠查询</view>
      <view class="hero-sub">粘贴商品分享文案，自动生成优惠链接</view>
    </view>

    <view class="card">
      <textarea
        v-model="content"
        class="input"
        placeholder="粘贴淘宝 / 拼多多分享的完整文案"
        placeholder-class="input-ph"
        maxlength="500"
        :show-confirm-bar="false"
      />
      <view class="row">
        <view class="btn ghost" @click="pasteFromClipboard">读取剪贴板</view>
        <view class="btn ghost" @click="clearInput">清空</view>
      </view>
      <view class="btn primary" :class="{ disabled: loading }" @click="convert">
        <text v-if="loading">正在查询…</text>
        <text v-else>立即查询</text>
      </view>
    </view>

    <view v-if="result" class="card">
      <view class="product">
        <image
          v-if="result.picUrl"
          class="thumb"
          :src="result.picUrl"
          mode="aspectFill"
        />
        <view v-else class="thumb placeholder">🛍</view>
        <view class="info">
          <view class="title">{{ result.title || '商品' }}</view>
          <view class="price-line">
            <text class="price">¥{{ money(result.finalPrice) }}</text>
            <text v-if="result.originalPrice" class="origin">¥{{ money(result.originalPrice) }}</text>
            <text v-if="result.couponAmount" class="coupon">券 ¥{{ money(result.couponAmount) }}</text>
          </view>
          <view v-if="result.couponInfo" class="coupon-line">{{ result.couponInfo }}</view>
          <view v-if="rebate" class="rebate-line">预估返利 ¥{{ money(rebate) }}</view>
        </view>
      </view>

      <view class="metrics">
        <view class="metric">
          <text class="m-val">{{ saved }}</text>
          <text class="m-label">可省(元)</text>
        </view>
        <view class="metric">
          <text class="m-val">{{ result.shortUrl ? '已生成' : '—' }}</text>
          <text class="m-label">推广链接</text>
        </view>
        <view class="metric">
          <text class="m-val">{{ lastId }}</text>
          <text class="m-label">商品编号</text>
        </view>
      </view>

      <view class="actions">
        <view v-if="result.tpwd" class="btn solid" @click="copy(result.tpwd, '淘口令已复制')">
          复制淘口令
        </view>
        <view v-if="result.shortUrl" class="btn line" @click="copy(result.shortUrl, '链接已复制')">
          复制链接
        </view>
        <view class="btn dark full" @click="copy(copyText, '文案已复制，可直接发送')">
          复制完整文案
        </view>
      </view>

      <view v-if="result.notice" class="notice">{{ result.notice }}</view>
    </view>

    <view v-if="history.length" class="card">
      <view class="his-head">
        <text class="his-title-bar">最近查询</text>
        <text class="his-clear" @click="clearHistory">清空</text>
      </view>
      <view
        v-for="(item, idx) in history"
        :key="idx"
        class="his-item"
        @click="reuse(item)"
      >
        <view class="his-left">
          <text class="his-title">{{ item.title }}</text>
          <text class="his-time">{{ item.time }}</text>
        </view>
        <text class="his-price">{{ item.price ? '¥' + item.price : '' }}</text>
      </view>
    </view>

    <view class="footer">支持淘宝 / 天猫 / 拼多多商品 · 优惠券数量有限，随时可能失效 · 返利为预估金额，以平台结算为准</view>
  </view>
</template>

<script>
import request from '@/utils/request'
import { ensurePrivacyAuthorized, isPrivacyScopeError, openPrivacyContract } from '@/utils/privacy'

const HISTORY_KEY = 'tbk_history'

export default {
  data() {
    return {
      content: '',
      loading: false,
      result: null,
      history: []
    }
  },
  computed: {
    saved() {
      if (!this.result || this.result.originalPrice == null || this.result.finalPrice == null) {
        return '--'
      }
      const diff = Number(this.result.originalPrice) - Number(this.result.finalPrice)
      return diff > 0 ? diff.toFixed(2).replace(/\.00$/, '') : '--'
    },
    lastId() {
      const id = this.result && this.result.itemId
      return id ? String(id).slice(-6) : '--'
    },
    /**
     * 预估返利：后端已按 tbk.cashback-rate 折算好（cashbackAmount），前端只展示
     * add by CYQ 2026-11-14 平台服务费，返现比例 90%
     */
    rebate() {
      const value = this.result && this.result.cashbackAmount
      if (value === null || value === undefined) return null
      const num = Number(value)
      return num > 0 ? num : null
    },
    copyText() {
      if (!this.result) return ''
      if (this.result.copyText) return this.result.copyText
      return [this.result.tpwd, this.result.shortUrl].filter(Boolean).join(' ')
    }
  },
  onLoad() {
    const saved = uni.getStorageSync(HISTORY_KEY)
    this.history = Array.isArray(saved) ? saved : []
  },
  methods: {
    money(value) {
      if (value === null || value === undefined || value === '') return '--'
      const num = Number(value)
      if (isNaN(num)) return String(value)
      return num.toFixed(2).replace(/\.00$/, '')
    },
    toast(title) {
      uni.showToast({ title, icon: 'none' })
    },
    copy(text, tip) {
      if (!text) {
        this.toast('没有可复制的内容')
        return
      }
      uni.setClipboardData({
        data: text,
        success: () => this.toast(tip || '已复制'),
        fail: err => {
          // 剪贴板同属隐私接口，未声明/未同意时返回 errno 112
          if (isPrivacyScopeError(err)) {
            this.showClipboardScopeTip('写入')
            return
          }
          this.toast('复制失败，请长按手动复制')
        }
      })
    },
    /**
     * add by CYQ 2026-11-04 包装 uni.getClipboardData 为 Promise，避免回调嵌套
     */
    readClipboard() {
      return new Promise(resolve => {
        uni.getClipboardData({
          success: res => resolve({ ok: true, data: (res && res.data) || '' }),
          fail: err => {
            console.warn('[tbk] getClipboardData fail:', err)
            resolve({ ok: false, err })
          }
        })
      })
    },
    /** H5：浏览器剪贴板读取（仅 https/localhost 且用户已授权时可用），小程序端返回 null */
    async readClipboardOnH5() {
      if (typeof navigator === 'undefined' || !navigator.clipboard) return null
      if (typeof window === 'undefined' || !window.isSecureContext) return null
      try {
        return await navigator.clipboard.readText()
      } catch (e) {
        return null
      }
    },
    /**
     * add by CYQ 2026-11-04 读取剪贴板前先过隐私授权，失败时给出可执行的提示
     * 微信小程序中 getClipboardData 属隐私接口：未同意协议 → 先 requirePrivacyAuthorize 再重试；
     * 未在《用户隐私保护指引》声明「剪贴板」→ errno 112，只能去小程序后台补充声明。
     */
    async pasteFromClipboard() {
      const h5Text = await this.readClipboardOnH5()
      if (h5Text !== null) {
        this.applyClipboard(h5Text)
        return
      }

      let ret = await this.readClipboard()
      if (!ret.ok) {
        const privacy = await ensurePrivacyAuthorized()
        if (privacy.ok) {
          ret = await this.readClipboard()
        } else if (isPrivacyScopeError(privacy.err) || isPrivacyScopeError(ret.err)) {
          this.showClipboardScopeTip('读取')
          return
        }
      }
      if (!ret.ok) {
        if (isPrivacyScopeError(ret.err)) {
          this.showClipboardScopeTip('读取')
          return
        }
        this.toast('读取剪贴板失败，请长按输入框粘贴')
        return
      }
      this.applyClipboard(ret.data)
    },
    applyClipboard(text) {
      const content = (text || '').trim()
      if (!content) {
        this.toast('剪贴板是空的')
        return
      }
      this.content = content
      this.toast('已粘贴，点击查询')
    },
    /**
     * errno 112：小程序后台《用户隐私保护指引》未声明「剪贴板」，
     * 该问题必须在微信公众平台后台补充声明，代码无法绕过。
     */
    showClipboardScopeTip(action) {
      uni.showModal({
        title: '剪贴板功能未开放',
        content: `小程序后台《用户隐私保护指引》未声明「剪贴板」，无法${action}剪贴板内容。请到「微信公众平台 → 设置 → 服务内容声明 → 用户隐私保护指引」勾选「剪贴板」并提交审核；也可先长按输入框手动粘贴。`,
        confirmText: '查看协议',
        cancelText: '手动粘贴',
        success: res => {
          if (res.confirm) openPrivacyContract()
        }
      })
    },
    clearInput() {
      this.content = ''
      this.result = null
    },
    /**
     * add by CYQ 2026-11-09 匿名用户稳定标识：登录态后端会忽略它，改用 token 里的用户ID
     */
    anonKey() {
      const key = 'tbk_user_key'
      let value = uni.getStorageSync(key)
      if (!value) {
        value = 'u' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
        uni.setStorageSync(key, value)
      }
      return value
    },
    async convert() {
      const content = (this.content || '').trim()
      if (!content) {
        this.toast('请先粘贴商品分享文案或链接')
        return
      }
      if (this.loading) return

      this.loading = true
      try {
        const res = await request({
          url: '/tbk/api/convert',
          method: 'post',
          timeout: 20000,
          data: { content, userKey: this.anonKey() }
        })
        this.result = res.data
        this.addHistory(content, res.data)
      } catch (e) {
        // 错误提示已由 request 统一 toast，这里只结束 loading
      } finally {
        this.loading = false
      }
    },
    addHistory(content, data) {
      const list = (this.history || []).filter(item => item.content !== content)
      list.unshift({
        content,
        title: (data && data.title) || '商品',
        price: data && data.finalPrice != null ? this.money(data.finalPrice) : '',
        time: this.formatTime(new Date())
      })
      this.history = list.slice(0, 10)
      uni.setStorageSync(HISTORY_KEY, this.history)
    },
    reuse(item) {
      this.content = item.content
      this.convert()
    },
    clearHistory() {
      this.history = []
      uni.removeStorageSync(HISTORY_KEY)
      this.toast('已清空')
    },
    formatTime(date) {
      const pad = n => (n < 10 ? '0' + n : '' + n)
      return `${date.getMonth() + 1}-${date.getDate()} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    }
  }
}
</script>

<style scoped>
.tbk-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding: 0 16px 40px;
  box-sizing: border-box;
}

.hero {
  padding: 32px 4px 20px;
}

.hero-badge {
  width: 44px;
  height: 44px;
  border-radius: 13px;
  background: linear-gradient(135deg, #ff7a3d, #ff3d00);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  line-height: 44px;
  text-align: center;
  margin-bottom: 14px;
  box-shadow: 0 8px 18px rgba(255, 61, 0, 0.25);
}

.hero-title {
  font-size: 23px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 6px;
}

.hero-sub {
  font-size: 13px;
  color: #8a9099;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 18px;
  margin-bottom: 14px;
  box-shadow: 0 2px 10px rgba(20, 25, 40, 0.04);
}

.input {
  width: 100%;
  min-height: 100px;
  border: 1px solid #eef0f3;
  border-radius: 12px;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
  background: #fafbfc;
  color: #1f2329;
  box-sizing: border-box;
}

.input-ph {
  color: #b6bcc5;
}

.row {
  display: flex;
  margin-top: 10px;
}

.btn {
  border-radius: 12px;
  text-align: center;
  font-size: 15px;
  font-weight: 500;
}

.btn.ghost {
  flex: 1;
  padding: 10px 0;
  background: #f4f6f8;
  color: #5b6370;
  font-size: 13.5px;
  margin-right: 10px;
}

.btn.ghost:last-child {
  margin-right: 0;
}

.btn.primary {
  width: 100%;
  margin-top: 14px;
  padding: 15px 0;
  background: linear-gradient(135deg, #ff6a2a, #ff3d00);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(255, 61, 0, 0.22);
}

.btn.primary.disabled {
  opacity: 0.6;
}

.product {
  display: flex;
}

.thumb {
  width: 88px;
  height: 88px;
  border-radius: 12px;
  flex-shrink: 0;
  background: #f2f4f7;
  margin-right: 12px;
}

.thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 14px;
  line-height: 1.45;
  color: #1f2329;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price-line {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
}

.price {
  color: #ff5000;
  font-size: 24px;
  font-weight: 700;
}

.origin {
  color: #a8aeb8;
  font-size: 13px;
  text-decoration: line-through;
  margin-left: 8px;
}

.coupon {
  background: #fff2ec;
  color: #ff5000;
  font-size: 11.5px;
  padding: 2px 7px;
  border-radius: 6px;
  margin-left: 8px;
}

.coupon-line {
  margin-top: 6px;
  font-size: 12px;
  color: #ff5000;
}

.rebate-line {
  margin-top: 5px;
  font-size: 12px;
  color: #12b76a;
}

.metrics {
  display: flex;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #eef0f3;
}

.metric {
  flex: 1;
  text-align: center;
}

.m-val {
  display: block;
  font-size: 16px;
  color: #2b3038;
  font-weight: 600;
}

.m-label {
  font-size: 11.5px;
  color: #8a9099;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  margin-top: 16px;
}

.btn.solid,
.btn.line {
  flex: 1;
  padding: 12px 0;
  margin-right: 10px;
  font-size: 14px;
  font-weight: 600;
}

.btn.solid {
  background: #fff3ee;
  color: #ff5000;
}

.btn.line {
  background: #fff;
  border: 1px solid #ffd2c2;
  color: #ff5000;
}

.btn.dark {
  padding: 12px 0;
  margin-top: 10px;
  background: #2b3038;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.btn.full {
  width: 100%;
}

.notice {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff8ed;
  color: #a2711f;
  font-size: 12px;
  line-height: 1.5;
}

.his-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.his-title-bar {
  font-size: 13px;
  color: #8a9099;
}

.his-clear {
  font-size: 12.5px;
  color: #b6bcc5;
}

.his-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 11px 0;
  border-bottom: 1px solid #eef0f3;
}

.his-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.his-left {
  flex: 1;
  min-width: 0;
  margin-right: 12px;
}

.his-title {
  font-size: 13px;
  color: #3a4048;
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.his-time {
  font-size: 11.5px;
  color: #b0b6bf;
}

.his-price {
  font-size: 13px;
  color: #ff5000;
  font-weight: 600;
}

.footer {
  text-align: center;
  font-size: 11.5px;
  color: #b0b6bf;
  line-height: 1.7;
  padding-top: 8px;
}
</style>
