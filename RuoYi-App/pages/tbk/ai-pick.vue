<template>
  <view class="pick-page">
    <view class="hero">
      <view class="hero-badge">AI</view>
      <view class="hero-title">AI 帮你挑 · 高性价比</view>
      <view class="hero-sub">说一句需求，跨淘宝 / 拼多多比价打分</view>
    </view>

    <view class="card">
      <textarea
        v-model="content"
        class="input"
        placeholder="例如：200元以内的降噪蓝牙耳机，通勤用，续航要久一点"
        placeholder-class="input-ph"
        maxlength="300"
        :show-confirm-bar="false"
      />
      <view class="budget-row">
        <text class="budget-label">预算上限</text>
        <input
          v-model="budget"
          class="budget-input"
          type="digit"
          placeholder="可不填，元"
          placeholder-class="input-ph"
        />
        <text class="budget-tip">不填则由 AI 从需求里识别</text>
      </view>
      <view class="row">
        <view class="btn ghost" @click="pasteFromClipboard">读取剪贴板</view>
        <view class="btn ghost" @click="clearInput">清空</view>
      </view>
      <view class="btn primary" :class="{ disabled: loading }" @click="pick">
        <text v-if="loading">正在比价打分…</text>
        <text v-else>AI 帮我挑</text>
      </view>
      <view v-if="loading" class="loading-tip">
        正在解析需求 → 跨平台搜索 → AI 打分，通常需要 10~20 秒
      </view>
    </view>

    <template v-if="result">
      <view class="card summary">
        <view class="summary-head">
          <text class="summary-title">AI 结论</text>
          <text class="summary-cost">{{ result.aiUsed ? 'AI 已打分' : '规则排序' }}</text>
        </view>
        <view class="summary-text">{{ result.summary || '已完成比价，结果如下' }}</view>
        <view class="chips">
          <text v-if="result.keyword" class="chip">搜索「{{ result.keyword }}」</text>
          <text v-if="result.budget" class="chip">预算 ¥{{ money(result.budget) }}</text>
          <text v-for="(p, i) in result.preferences || []" :key="i" class="chip">{{ p }}</text>
        </view>
        <view v-if="result.advice" class="advice">{{ result.advice }}</view>
        <view v-for="(w, i) in result.warnings || []" :key="i" class="warn-line">· {{ w }}</view>
      </view>

      <view class="list">
        <view
          v-for="(item, idx) in result.items"
          :key="idx"
          class="item-card"
          :class="{ best: result.bestIndex === idx + 1 }"
        >
          <view class="item-head">
            <text v-if="result.bestIndex === idx + 1" class="rank-badge best-badge">最值得买</text>
            <text v-else class="rank-badge">No.{{ idx + 1 }}</text>
            <text class="platform" :class="item.platform === 'pdd' ? 'pdd' : 'tb'">
              {{ item.platformName || (item.platform === 'pdd' ? '拼多多' : '淘宝') }}
            </text>
            <view class="score-wrap">
              <view class="score-bar">
                <view class="score-fill" :style="{ width: (item.score || 0) + '%' }"></view>
              </view>
              <text class="score-num">{{ item.score || '--' }}分</text>
            </view>
          </view>

          <view class="product">
            <image v-if="item.picUrl" class="thumb" :src="item.picUrl" mode="aspectFill" />
            <view v-else class="thumb placeholder">🛍</view>
            <view class="info">
              <view class="title">{{ item.title }}</view>
              <view class="price-line">
                <text class="price">¥{{ money(item.finalPrice) }}</text>
                <text v-if="item.originalPrice" class="origin">¥{{ money(item.originalPrice) }}</text>
                <text v-if="item.couponAmount" class="coupon">券 ¥{{ money(item.couponAmount) }}</text>
              </view>
              <view v-if="item.couponInfo" class="coupon-line">{{ item.couponInfo }}</view>
              <view v-if="rebateOf(item)" class="rebate-line">预估返利 ¥{{ money(rebateOf(item)) }}</view>
              <view class="meta">
                <text v-if="item.sales">销量 {{ item.sales }}</text>
                <text v-if="item.shopName" class="shop">{{ item.shopName }}</text>
              </view>
            </view>
          </view>

          <view v-if="item.reason" class="reason">{{ item.reason }}</view>
          <view v-if="(item.tags || []).length" class="tags">
            <text v-for="(tag, i) in item.tags" :key="i" class="tag">{{ tag }}</text>
          </view>
          <view v-if="item.caution" class="caution">⚠ {{ item.caution }}</view>

          <view class="actions">
            <view
              class="btn mini solid"
              :class="{ disabled: item._detailLoading }"
              @click="copyItem(item)"
            >
              {{ item._detailLoading ? '正在生成…' : '复制口令/链接' }}
            </view>
          </view>

          <view v-if="item._detail" class="detail">
            <view v-if="item._detail.tpwd" class="detail-row">
              <text class="detail-label">淘口令</text>
              <text class="detail-val">{{ item._detail.tpwd }}</text>
              <text class="detail-copy" @click="copy(item._detail.tpwd, '淘口令已复制')">复制</text>
            </view>
            <view v-if="item._detail.shortUrl" class="detail-row">
              <text class="detail-label">推广链接</text>
              <text class="detail-val">{{ item._detail.shortUrl }}</text>
              <text class="detail-copy" @click="copy(item._detail.shortUrl, '链接已复制')">复制</text>
            </view>
            <view v-if="item._detail.notice" class="detail-notice">{{ item._detail.notice }}</view>
          </view>
        </view>
      </view>

      <view class="notice">{{ result.notice }}</view>
    </template>

    <view v-if="!result && !loading" class="empty">
      <view class="empty-emoji">🛒</view>
      <view class="empty-title">把想买的东西告诉 AI</view>
      <view class="empty-sub">预算、用途、偏好都可以写进去，越具体越好挑</view>
      <view class="empty-examples">
        <text
          v-for="(example, i) in examples"
          :key="i"
          class="example"
          @click="useExample(example)"
        >{{ example }}</text>
      </view>
    </view>

    <view v-if="history.length" class="card">
      <view class="his-head">
        <text class="his-title-bar">最近需求</text>
        <text class="his-clear" @click="clearHistory">清空</text>
      </view>
      <view
        v-for="(item, idx) in history"
        :key="idx"
        class="his-item"
        @click="reuse(item)"
      >
        <view class="his-left">
          <text class="his-title">{{ item.content }}</text>
          <text class="his-time">{{ item.time }}</text>
        </view>
        <text class="his-price">{{ item.budget ? '¥' + item.budget : '' }}</text>
      </view>
    </view>

    <view class="footer">
      AI 评分基于平台公开的标题、价格、券和销量，仅供参考，下单前请核对规格与券后价；返利为预估金额，以平台实际结算为准
    </view>
    <view class="footer-link" @click="goConvert">已经看好了？去粘贴链接转链 →</view>
  </view>
</template>

<script>
import request from '@/utils/request'
import { ensurePrivacyAuthorized, isPrivacyScopeError, openPrivacyContract } from '@/utils/privacy'

const HISTORY_KEY = 'tbk_pick_history'

export default {
  data() {
    return {
      content: '',
      budget: '',
      loading: false,
      result: null,
      history: [],
      examples: [
        '200元以内的降噪蓝牙耳机，通勤用',
        '想买个适合送长辈的颈椎按摩仪，300左右',
        '办公室用的静音机械键盘，预算500'
      ]
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
    /**
     * 预估返利：后端已按 tbk.cashback-rate 算好（cashbackAmount），前端只展示不折算
     * 转链后取转链结果里的精确佣金口径，转链前取选品列表里的估算值
     * add by CYQ 2026-11-14 平台服务费，返现比例 90%
     */
    rebateOf(item) {
      if (!item) return null
      const detail = item._detail
      const value = detail && detail.cashbackAmount != null ? detail.cashbackAmount : item.cashbackAmount
      if (value === null || value === undefined) return null
      const num = Number(value)
      return num > 0 ? num : null
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
          if (isPrivacyScopeError(err)) {
            this.showClipboardScopeTip('写入')
            return
          }
          this.toast('复制失败，请长按手动复制')
        }
      })
    },
    readClipboard() {
      return new Promise(resolve => {
        uni.getClipboardData({
          success: res => resolve({ ok: true, data: (res && res.data) || '' }),
          fail: err => resolve({ ok: false, err })
        })
      })
    },
    async readClipboardOnH5() {
      if (typeof navigator === 'undefined' || !navigator.clipboard) return null
      if (typeof window === 'undefined' || !window.isSecureContext) return null
      try {
        return await navigator.clipboard.readText()
      } catch (e) {
        return null
      }
    },
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
      this.toast('已粘贴，点击 AI 帮我挑')
    },
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
      this.budget = ''
      this.result = null
    },
    useExample(text) {
      this.content = text
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
    async pick() {
      const content = (this.content || '').trim()
      if (!content) {
        this.toast('先说说你想买什么')
        return
      }
      if (this.loading) return
      this.loading = true
      try {
        const data = { content }
        const budget = Number(this.budget)
        if (this.budget && !isNaN(budget) && budget > 0) {
          data.budget = budget
        }
        const res = await request({
          url: '/tbk/api/pick',
          method: 'post',
          timeout: 60000,
          data: { ...data, userKey: this.anonKey() }
        })
        const result = res.data || {}
        result.items = (result.items || []).map(item => ({ ...item, _detail: null, _detailLoading: false }))
        this.result = result
        this.addHistory(content)
      } catch (e) {
        // 错误提示已由 request 统一 toast
      } finally {
        this.loading = false
      }
    },
    /**
     * 复制文案优先级：淘口令完整文案（口令 + m.tb.cn 短链 + 标题）> 纯口令 > 推广链接
     *
     * 复制口令后打开淘宝，淘宝会自动识别并跳到对应商品；完整文案里的短链
     * 发给微信/QQ 好友也能点开唤起淘宝。
     * add by CYQ 2026-11-10 选品复制带淘口令
     */
    copyTextOf(detail) {
      if (!detail) return ''
      return detail.tpwdModel || detail.tpwd || detail.shortUrl || ''
    },
    /**
     * 一键复制：能转链就复制淘口令/推广链接，转不了就直接复制商品原链接
     * 不再区分「复制链接」和「生成口令」两个按钮
     */
    async copyItem(item) {
      if (item._detailLoading) return
      const cached = item._detail
      const cachedText = this.copyTextOf(cached)
      if (cachedText) {
        this.copy(cachedText, cached.tpwd ? '淘口令已复制' : '链接已复制')
        return
      }
      item._detailLoading = true
      try {
        const res = await request({
          url: '/tbk/api/pick/convert',
          method: 'post',
          timeout: 30000,
          silent: true,
          data: {
            platform: item.platform,
            itemId: item.itemId,
            goodsSign: item.goodsSign,
            title: item.title,
            itemUrl: item.itemUrl,
            userKey: this.anonKey()
          }
        })
        const detail = res.data || {}
        item._detail = detail
        const text = this.copyTextOf(detail)
        if (text) {
          this.copy(text, detail.tpwd ? '淘口令已复制，打开淘宝自动跳转' : '链接已复制')
        } else {
          this.copyOriginal(item, detail.notice)
        }
      } catch (e) {
        // 转链失败不打扰用户，直接给原链接
        this.copyOriginal(item, e && e.message)
      } finally {
        item._detailLoading = false
      }
    },
    copyOriginal(item, reason) {
      const link = item.shortUrl || item.itemUrl
      if (!link) {
        this.toast(reason ? '推广链接生成失败：' + reason : '没有可复制的链接')
        return
      }
      this.copy(link, '已复制商品链接，可直接打开')
    },
    addHistory(content) {
      const list = (this.history || []).filter(item => item.content !== content)
      list.unshift({
        content,
        budget: this.budget,
        time: this.formatTime(new Date())
      })
      this.history = list.slice(0, 10)
      uni.setStorageSync(HISTORY_KEY, this.history)
    },
    reuse(item) {
      this.content = item.content
      this.budget = item.budget || ''
      this.pick()
    },
    clearHistory() {
      this.history = []
      uni.removeStorageSync(HISTORY_KEY)
      this.toast('已清空')
    },
    formatTime(date) {
      const pad = n => (n < 10 ? '0' + n : '' + n)
      return `${date.getMonth() + 1}-${date.getDate()} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    },
    goConvert() {
      uni.navigateTo({ url: '/pages/tbk/index' })
    }
  }
}
</script>

<style scoped>
.pick-page {
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
  background: linear-gradient(135deg, #7b61ff, #4b2bff);
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  line-height: 44px;
  text-align: center;
  margin-bottom: 14px;
  box-shadow: 0 8px 18px rgba(75, 43, 255, 0.25);
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

.budget-row {
  display: flex;
  align-items: center;
  margin-top: 12px;
}

.budget-label {
  font-size: 13px;
  color: #5b6370;
  margin-right: 10px;
}

.budget-input {
  width: 110px;
  height: 34px;
  border: 1px solid #eef0f3;
  border-radius: 9px;
  padding: 0 10px;
  font-size: 13.5px;
  background: #fafbfc;
  box-sizing: border-box;
}

.budget-tip {
  flex: 1;
  font-size: 11.5px;
  color: #b0b6bf;
  margin-left: 10px;
  text-align: right;
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
  background: linear-gradient(135deg, #7b61ff, #4b2bff);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(75, 43, 255, 0.22);
}

.btn.primary.disabled {
  opacity: 0.6;
}

.loading-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #8a9099;
  text-align: center;
}

.summary-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.summary-title {
  font-size: 13px;
  font-weight: 600;
  color: #4b2bff;
}

.summary-cost {
  font-size: 11.5px;
  color: #8a9099;
}

.summary-text {
  font-size: 15px;
  line-height: 1.5;
  color: #1f2329;
  font-weight: 600;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  margin-top: 10px;
}

.chip {
  background: #f3f0ff;
  color: #5a3ff0;
  font-size: 11.5px;
  padding: 3px 9px;
  border-radius: 7px;
  margin: 0 8px 8px 0;
}

.advice {
  margin-top: 4px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #faf9ff;
  color: #4e5360;
  font-size: 12.5px;
  line-height: 1.6;
}

.warn-line {
  margin-top: 6px;
  font-size: 11.5px;
  color: #b07b1f;
  line-height: 1.5;
}

.list {
  margin-bottom: 4px;
}

.item-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 10px rgba(20, 25, 40, 0.04);
  border: 1px solid transparent;
}

.item-card.best {
  border-color: #ffd0bd;
  box-shadow: 0 6px 18px rgba(255, 90, 26, 0.1);
}

.item-head {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.rank-badge {
  font-size: 11px;
  color: #8a9099;
  background: #f4f6f8;
  padding: 2px 8px;
  border-radius: 6px;
  margin-right: 8px;
}

.best-badge {
  color: #fff;
  background: linear-gradient(135deg, #ff7a3d, #ff3d00);
}

.platform {
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 6px;
  margin-right: 8px;
}

.platform.tb {
  color: #ff5000;
  background: #fff2ec;
}

.platform.pdd {
  color: #e02e24;
  background: #fff0ef;
}

.score-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.score-bar {
  width: 64px;
  height: 5px;
  border-radius: 3px;
  background: #f0f1f4;
  overflow: hidden;
  margin-right: 6px;
}

.score-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #ffb27a, #ff5a1a);
}

.score-num {
  font-size: 12px;
  color: #ff5000;
  font-weight: 600;
  min-width: 38px;
  text-align: right;
}

.product {
  display: flex;
}

.thumb {
  width: 82px;
  height: 82px;
  border-radius: 12px;
  flex-shrink: 0;
  background: #f2f4f7;
  margin-right: 12px;
}

.thumb.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}

.info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 13.5px;
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
  font-size: 22px;
  font-weight: 700;
}

.origin {
  color: #a8aeb8;
  font-size: 12.5px;
  text-decoration: line-through;
  margin-left: 7px;
}

.coupon {
  background: #fff2ec;
  color: #ff5000;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 6px;
  margin-left: 7px;
}

.coupon-line {
  margin-top: 5px;
  font-size: 11.5px;
  color: #ff5000;
}

.rebate-line {
  margin-top: 4px;
  font-size: 11.5px;
  color: #12b76a;
}

.meta {
  display: flex;
  margin-top: 6px;
  font-size: 11.5px;
  color: #a8aeb8;
}

.shop {
  margin-left: 10px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.reason {
  margin-top: 12px;
  font-size: 12.5px;
  line-height: 1.6;
  color: #4e5360;
  background: #fafbfc;
  border-radius: 10px;
  padding: 9px 11px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: 8px;
}

.tag {
  font-size: 11px;
  color: #5a3ff0;
  background: #f3f0ff;
  padding: 2px 8px;
  border-radius: 6px;
  margin: 0 7px 6px 0;
}

.caution {
  margin-top: 4px;
  font-size: 11.5px;
  color: #b07b1f;
  line-height: 1.5;
}

.actions {
  display: flex;
  margin-top: 12px;
}

.btn.mini {
  flex: 1;
  padding: 10px 0;
  font-size: 13px;
  font-weight: 600;
  background: #f4f6f8;
  color: #5b6370;
  margin-right: 10px;
}

.btn.mini:last-child {
  margin-right: 0;
}

.btn.mini.solid {
  background: #fff3ee;
  color: #ff5000;
}

.btn.mini.disabled {
  opacity: 0.6;
}

.detail {
  margin-top: 10px;
  padding: 10px 11px;
  border-radius: 10px;
  background: #fafbfc;
}

.detail-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.detail-row:last-child {
  margin-bottom: 0;
}

.detail-label {
  font-size: 11.5px;
  color: #8a9099;
  width: 60px;
  flex-shrink: 0;
}

.detail-val {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  color: #3a4048;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.detail-copy {
  font-size: 12px;
  color: #ff5000;
  margin-left: 10px;
}

.detail-notice {
  font-size: 11.5px;
  color: #b07b1f;
  line-height: 1.5;
}

.notice {
  margin: 2px 2px 14px;
  font-size: 11.5px;
  color: #b0b6bf;
  line-height: 1.6;
  text-align: center;
}

.empty {
  text-align: center;
  padding: 34px 10px 20px;
}

.empty-emoji {
  font-size: 40px;
  margin-bottom: 12px;
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #3a4048;
  margin-bottom: 6px;
}

.empty-sub {
  font-size: 12.5px;
  color: #a8aeb8;
  margin-bottom: 18px;
}

.empty-examples {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.example {
  font-size: 12.5px;
  color: #5a3ff0;
  background: #fff;
  border: 1px solid #ebe6ff;
  border-radius: 10px;
  padding: 9px 14px;
  margin-bottom: 10px;
  max-width: 100%;
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
  font-size: 12.5px;
  color: #5a3ff0;
  font-weight: 600;
}

.footer {
  text-align: center;
  font-size: 11.5px;
  color: #b0b6bf;
  line-height: 1.7;
  padding-top: 8px;
}

.footer-link {
  text-align: center;
  font-size: 12.5px;
  color: #5a3ff0;
  padding-top: 10px;
}
</style>
