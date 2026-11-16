<template>
  <view class="stock-container" :class="themeClass">
    <!-- 顶部 segmented 切换：持仓 / 建议 + 右上角订阅按钮 -->
    <view class="seg-bar">
      <view class="seg-item" :class="{ active: activeTab === 'holdings' }" @click="switchTab('holdings')">
        <text>持仓</text>
      </view>
      <view class="seg-item" :class="{ active: activeTab === 'recs' }" @click="switchTab('recs')">
        <text>建议</text>
      </view>
      <view class="subscribe-btn" @click="openSubscribe" :class="{ subscribed: subscribeStatus.subscribed }">
        <uni-icons :type="subscribeStatus.subscribed ? 'checkmarkempty' : 'email'" size="14" color="#fff"></uni-icons>
        <text class="subscribe-text">{{ subscribeStatus.subscribed ? '已订阅' : '订阅通知' }}</text>
      </view>
    </view>

    <!-- ============ 持仓 Tab ============ -->
    <view v-if="activeTab === 'holdings'" class="tab-panel">
      <view class="summary-card">
        <view class="summary-row">
          <text class="summary-label">持仓市值</text>
          <text class="summary-value">{{ totalMarketValue }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">累计盈亏</text>
          <text class="summary-value" :class="totalPnl >= 0 ? 'up' : 'down'">
            {{ totalPnl >= 0 ? '+' : '' }}{{ fmtMoney(totalPnl) }}
          </text>
        </view>
      </view>

      <view class="list-body">
        <view v-if="holdingsLoading" class="loading-tip">加载中...</view>
        <view v-else-if="holdings.length === 0" class="empty-tip">
          <uni-icons type="wallet-filled" size="40" color="#ccc"></uni-icons>
          <text class="empty-text">暂无持仓</text>
        </view>
        <view v-for="item in holdings" :key="item.symbol" class="holding-card" @click="viewRecs(item)">
          <view class="card-header">
            <text class="stock-name">{{ item.name }}</text>
            <text class="stock-symbol">{{ item.symbol }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">持仓</text>
            <text class="row-value">{{ item.shares }} 股</text>
          </view>
          <view class="card-row">
            <text class="row-label">成本价</text>
            <text class="row-value">{{ fmtPrice(item.costPrice) }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">现价</text>
            <text class="row-value">{{ fmtPrice(item.currentPrice) }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">市值</text>
            <text class="row-value">{{ fmtMoney(item.marketValue) }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">盈亏</text>
            <text class="row-value" :class="(item.profitLoss || 0) >= 0 ? 'up' : 'down'">
              {{ fmtSignedMoney(item.profitLoss) }}（{{ fmtSignedPct(item.profitLossPct) }}）
            </text>
          </view>
          <view class="card-footer">
            <text v-if="item.assignedStrategy" class="strategy-tag">{{ item.assignedStrategy }}</text>
            <text v-if="item.regime" class="regime-tag">{{ item.regime }}</text>
            <text class="view-recs">查看建议 ›</text>
          </view>
        </view>
      </view>
    </view>

    <!-- ============ 建议 Tab ============ -->
    <view v-else-if="activeTab === 'recs'" class="tab-panel">
      <view v-if="recsLoading" class="loading-tip">加载中...</view>
      <view v-else-if="filteredRecs.length === 0" class="empty-tip">
        <uni-icons type="paperclip" size="40" color="#ccc"></uni-icons>
        <text class="empty-text">暂无可执行建议</text>
      </view>

      <view v-for="item in filteredRecs" :key="item.id" class="rec-card">
        <view class="card-header">
          <text class="stock-name">{{ item.name || item.symbol }}</text>
          <text class="stock-symbol">{{ item.symbol }}</text>
          <text class="action-tag" :class="item.action">{{ item.action }}</text>
        </view>

        <view class="card-row">
          <text class="row-label">策略</text>
          <text class="row-value">{{ item.strategyName || '--' }}</text>
        </view>
        <view class="card-row">
          <text class="row-label">建议价</text>
          <text class="row-value">{{ fmtPrice(item.priceHint) }}</text>
        </view>
        <view class="card-row">
          <text class="row-label">建议股数</text>
          <text class="row-value">{{ item.suggestedShares || '--' }} 股</text>
        </view>
        <view class="card-row">
          <text class="row-label">建议金额</text>
          <text class="row-value">{{ fmtMoney(item.suggestedAmount) }}</text>
        </view>
        <view class="card-row">
          <text class="row-label">信心度</text>
          <text class="row-value">{{ confidenceText(item.confidence) }}</text>
        </view>
        <view v-if="item.reason" class="reason-box">
          <text class="reason-text">{{ item.reason }}</text>
        </view>

        <view class="card-footer">
          <text class="rec-time">{{ fmtTime(item.createdTime) }}</text>
          <view class="btn-group">
            <button class="expire-btn" size="mini" type="default" @click="expireRec(item)">忽略</button>
            <button class="execute-btn" size="mini" type="primary" @click="executeRec(item)">确认执行</button>
          </view>
        </view>
      </view>
    </view>

    <!-- ============ 执行确认弹窗 ============ -->
    <view v-if="showExecModal" class="modal-mask" @tap="cancelExec" @click="cancelExec">
      <view class="modal-box" @tap.stop @click.stop>
        <view class="modal-title">确认执行交易</view>
        <view class="modal-subtitle">
          {{ execItem.action }} {{ execItem.name || execItem.symbol }}（{{ execItem.symbol }}）
        </view>

        <view class="modal-field">
          <text class="field-label">实际成交价</text>
          <input v-model="execForm.price" type="digit" class="field-input" placeholder="输入实际价格" placeholder-style="color:#C9CDD4" :adjust-position="false" cursor-spacing="20" hold-keyboard />
        </view>

        <view class="modal-field">
          <text class="field-label">实际成交数量</text>
          <input v-model="execForm.shares" type="number" class="field-input" placeholder="输入股数" placeholder-style="color:#C9CDD4" :adjust-position="false" cursor-spacing="20" hold-keyboard />
        </view>

        <view class="modal-hint">
          预估金额：¥{{ execEstAmount }}
        </view>

        <view class="modal-actions">
          <button class="modal-btn cancel" @click="cancelExec">取消</button>
          <button class="modal-btn confirm" @click="confirmExec">确认</button>
        </view>
      </view>
    </view>

    <!-- ============ 订阅通知弹窗 ============ -->
    <view v-if="showSubscribeModal" class="modal-mask" @tap="closeSubscribe" @click="closeSubscribe">
      <view class="modal-box" @tap.stop @click.stop>
        <view class="modal-title">订阅邮件通知</view>
        <view class="modal-subtitle">量化交易信号将通过邮件推送，需绑定邮箱</view>
        <view v-if="subscribeStatus.subscribed" class="subscribe-badge">
          <uni-icons type="checkmarkempty" size="12" color="#1aad19"></uni-icons>
          <text class="badge-text">已订阅量化通知</text>
        </view>
        <view class="modal-field">
          <text class="field-label">邮箱地址 <text style="color:#C9CDD4;font-weight:400">（必填）</text></text>
          <input v-model="subscribeForm.email" type="text" class="field-input" placeholder="请输入邮箱，如 user@163.com" placeholder-style="color:#C9CDD4" :adjust-position="false" cursor-spacing="20" hold-keyboard />
        </view>
        <view class="modal-hint">提交后将自动为您分配「量化交易用户」角色，持仓分析与全市场扫描结果将发送至该邮箱</view>
        <view class="modal-actions">
          <button class="modal-btn cancel" @click="closeSubscribe">取消</button>
          <button class="modal-btn confirm" @click="confirmSubscribe" :loading="subscribeLoading">确认订阅</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getStockHoldings, getPendingRecommendations, executeRecommendation, expireRecommendation } from '@/api/main/stock'
import { getUserProfile, subscribeQuant, getSubscribeQuantStatus } from '@/api/system/user'
import themeMixin from '@/mixins/theme'

export default {
  mixins: [themeMixin],
  data() {
    return {
      activeTab: 'holdings',
      holdings: [],
      holdingsLoading: false,
      recs: [],
      recsLoading: false,
      symbolFilter: '',
      // 执行确认弹窗
      showExecModal: false,
      execItem: {},
      execForm: { price: '', shares: '' },
      // 订阅通知弹窗 add by CYQ 2026-09-02
      showSubscribeModal: false,
      subscribeForm: { email: '' },
      subscribeStatus: { email: '', subscribed: false },
      subscribeLoading: false
    }
  },
  computed: {
    totalMarketValue() {
      return this.fmtMoney(this.holdings.reduce((s, it) => s + (it.marketValue || 0), 0))
    },
    totalPnl() {
      return this.holdings.reduce((s, it) => s + (it.profitLoss || 0), 0)
    },
    filteredRecs() {
      if (!this.symbolFilter) return this.recs
      return this.recs.filter(r => r.symbol === this.symbolFilter)
    },
    execEstAmount() {
      const p = parseFloat(this.execForm.price)
      const s = parseInt(this.execForm.shares)
      if (isNaN(p) || isNaN(s) || p <= 0 || s <= 0) return '--'
      return Number(p * s).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }
  },
  onLoad(options) {
    // 支持外部直接进入建议 tab（?tab=recs&symbol=xxx）
    if (options && options.tab === 'recs') {
      this.activeTab = 'recs'
    }
    if (options && options.symbol) {
      this.symbolFilter = options.symbol
    }
  },
  onShow() {
    this.loadHoldings()
    this.loadRecs()
    this.loadSubscribeStatus()
  },
  onPullDownRefresh() {
    const done = () => uni.stopPullDownRefresh()
    if (this.activeTab === 'holdings') {
      this.loadHoldings(done)
    } else {
      this.loadRecs(done)
    }
  },
  methods: {
    switchTab(tab) {
      this.activeTab = tab
    },
    /** 持仓页点击某只股票 → 切到建议 tab 并按 symbol 预筛选 */
    viewRecs(item) {
      this.symbolFilter = item.symbol
      this.activeTab = 'recs'
    },
    loadHoldings(done) {
      this.holdingsLoading = true
      getStockHoldings()
        .then(res => {
          this.holdings = (res && res.data) || []
        })
        .finally(() => {
          this.holdingsLoading = false
          if (done) done()
        })
    },
    loadRecs(done) {
      this.recsLoading = true
      getPendingRecommendations()
        .then(res => {
          this.recs = (res && res.data) || []
        })
        .finally(() => {
          this.recsLoading = false
          if (done) done()
        })
    },
    /** 打开执行确认弹窗，预填建议值 */
    executeRec(item) {
      this.execItem = item
      this.execForm = {
        price: item.priceHint ? String(item.priceHint) : '',
        shares: item.suggestedShares ? String(item.suggestedShares) : ''
      }
      this.showExecModal = true
    },
    cancelExec() {
      this.showExecModal = false
      this.execItem = {}
    },
    /** 确认执行，传入实际价格/数量 */
    confirmExec() {
      const price = parseFloat(this.execForm.price)
      const shares = parseInt(this.execForm.shares)
      if (isNaN(price) || price <= 0) {
        uni.showToast({ title: '请输入有效价格', icon: 'none' })
        return
      }
      if (isNaN(shares) || shares <= 0) {
        uni.showToast({ title: '请输入有效股数', icon: 'none' })
        return
      }
      const id = this.execItem.id
      this.showExecModal = false
      executeRecommendation(id, { price, shares })
        .then(() => {
          uni.showToast({ title: '已执行', icon: 'success' })
          this.loadRecs()
          this.loadHoldings()
        })
        .catch(() => {
          uni.showToast({ title: '执行失败', icon: 'none' })
        })
    },
    expireRec(item) {
      uni.showModal({
        title: '忽略建议',
        content: '确定忽略这条建议吗？（不执行交易）',
        confirmText: '忽略',
        success: (res) => {
          if (res.confirm) {
            expireRecommendation(item.id).then(() => {
              uni.showToast({ title: '已忽略', icon: 'success' })
              this.loadRecs()
            })
          }
        }
      })
    },
    // ============ 订阅通知 ============ add by CYQ 2026-09-02
    loadSubscribeStatus() {
      getSubscribeQuantStatus().then(res => {
        // 后端 AjaxResult: {code,msg,email,subscribed,quantRoleId}  或 {data:{email,subscribed}}
        const d = res.data && typeof res.data === 'object' && !Array.isArray(res.data) && (res.data.email !== undefined || res.data.subscribed !== undefined) ? res.data : res
        this.subscribeStatus = {
          email: d.email || res.email || '',
          subscribed: !!(d.subscribed ?? res.subscribed)
        }
      }).catch(() => {})
    },
    openSubscribe() {
      // 带出个人信息邮箱，新用户通常为空
      const fill = (email) => {
        this.subscribeForm.email = email || ''
        this.showSubscribeModal = true
      }
      if (this.subscribeStatus.email) {
        fill(this.subscribeStatus.email)
      } else {
        getUserProfile().then(res => {
          const email = (res.data && res.data.email) || (res.user && res.user.email) || ''
          this.subscribeStatus.email = email
          fill(email)
        }).catch(() => fill(''))
      }
    },
    closeSubscribe() {
      this.showSubscribeModal = false
    },
    confirmSubscribe() {
      const email = (this.subscribeForm.email || '').trim()
      const emailReg = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
      if (!email) {
        uni.showToast({ title: '请输入邮箱', icon: 'none' })
        return
      }
      if (!emailReg.test(email)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
        return
      }
      this.subscribeLoading = true
      subscribeQuant(email).then(res => {
        uni.showToast({ title: res.msg || '订阅成功', icon: 'success' })
        this.subscribeStatus.email = email
        this.subscribeStatus.subscribed = true
        this.showSubscribeModal = false
      }).catch(err => {
        const msg = (err && err.msg) || (err && err.message) || '订阅失败'
        uni.showToast({ title: msg, icon: 'none' })
      }).finally(() => {
        this.subscribeLoading = false
      })
    },
    confidenceText(c) {
      const map = { high: '高', medium: '中', low: '低' }
      return map[c] || c || '--'
    },
    fmtPrice(v) {
      return v === null || v === undefined ? '--' : Number(v).toFixed(2)
    },
    fmtMoney(v) {
      if (v === null || v === undefined) return '--'
      return '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    fmtSignedMoney(v) {
      if (v === null || v === undefined) return '--'
      const sign = v > 0 ? '+' : ''
      return sign + '¥' + Number(Math.abs(v)).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    fmtSignedPct(v) {
      if (v === null || v === undefined) return ''
      const sign = v > 0 ? '+' : ''
      return sign + Number(v).toFixed(2) + '%'
    },
    fmtTime(t) {
      if (!t) return ''
      return String(t).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
.stock-container {
  background: var(--bg-start);
  min-height: 100vh;
}
.seg-bar {
  display: flex;
  background: var(--card-bg);
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 1rpx solid var(--card-border);
  padding-right: 180rpx;
}
.seg-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: var(--color-text-secondary);
  position: relative;
}
.seg-item.active {
  color: var(--color-primary);
  font-weight: bold;
}
.seg-item.active::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 0;
  width: 60rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: var(--color-primary);
}
.subscribe-btn {
  position: absolute;
  right: 16rpx;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 6rpx;
  background: var(--color-primary);
  color: #fff;
  padding: 10rpx 18rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  line-height: 1;
}
.subscribe-btn.subscribed {
  background: #1aad19;
}
.subscribe-text {
  font-size: 22rpx;
  color: #fff;
}
.tab-panel {
  padding: 16rpx;
}
.summary-card {
  background: var(--card-bg);
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  display: flex;
  justify-content: space-around;
}
.summary-row {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.summary-label {
  font-size: 24rpx;
  color: var(--color-text-secondary);
}
.summary-value {
  font-size: 34rpx;
  font-weight: bold;
  margin-top: 8rpx;
}
.up {
  color: #e64a4a;
}
.down {
  color: #1aad19;
}
.list-body {
  padding-bottom: 40rpx;
}
.loading-tip {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 60rpx 0;
}
.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
}
.empty-text {
  color: var(--color-text-secondary);
  margin-top: 16rpx;
}
.holding-card,
.rec-card {
  background: var(--card-bg);
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.card-header {
  display: flex;
  align-items: baseline;
  margin-bottom: 12rpx;
}
.stock-name {
  font-size: 32rpx;
  font-weight: bold;
}
.stock-symbol {
  margin-left: 12rpx;
  font-size: 24rpx;
  color: var(--color-text-secondary);
}
.action-tag {
  margin-left: auto;
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}
.action-tag.buy {
  color: #e64a4a;
  background: #fdecec;
}
.action-tag.sell {
  color: #1aad19;
  background: #e8f8e8;
}
.action-tag.reduce {
  color: #b26a00;
  background: #fff7e6;
}
.card-row {
  display: flex;
  justify-content: space-between;
  padding: 6rpx 0;
}
.row-label {
  color: var(--color-text-secondary);
  font-size: 26rpx;
}
.row-value {
  font-size: 26rpx;
}
.reason-box {
  margin-top: 12rpx;
  background: var(--skeleton-1);
  border-radius: 12rpx;
  padding: 12rpx 16rpx;
}
.reason-text {
  font-size: 24rpx;
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.card-footer {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid var(--card-border);
}
.strategy-tag {
  font-size: 20rpx;
  color: var(--color-primary);
  background: var(--skeleton-2);
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
  margin-right: 8rpx;
}
.regime-tag {
  font-size: 20rpx;
  color: #b26a00;
  background: #fff7e6;
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
  margin-right: 8rpx;
}
.view-recs {
  margin-left: auto;
  color: var(--color-primary);
  font-size: 24rpx;
}
.rec-time {
  font-size: 22rpx;
  color: var(--color-text-secondary);
}
.btn-group {
  margin-left: auto;
  display: flex;
}
.expire-btn {
  margin-right: 16rpx;
  font-size: 24rpx;
}
.execute-btn {
  font-size: 24rpx;
}

/* 执行确认弹窗 — 修复：触达尺寸/对比度/可见性 */
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4rpx);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 32rpx;
}
.modal-box {
  width: 640rpx;
  max-width: 90vw;
  background: var(--card-bg);
  border-radius: 24rpx;
  padding: 40rpx;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.14);
  border: 1rpx solid var(--card-border);
}
.modal-title {
  font-size: 34rpx;
  font-weight: 700;
  text-align: center;
  color: var(--color-text);
  line-height: 1.4;
}
.modal-subtitle {
  font-size: 26rpx;
  color: var(--color-text-secondary);
  text-align: center;
  margin-top: 8rpx;
  margin-bottom: 32rpx;
  line-height: 1.5;
  word-break: break-all;
}
.modal-field {
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
}
.field-label {
  display: block;
  font-size: 26rpx;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 12rpx;
  line-height: 1.4;
}
.field-input {
  width: 100%;
  height: 88rpx;
  min-height: 88rpx;
  border: 1.5rpx solid var(--card-border);
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  font-weight: 500;
  color: var(--color-text);
  background: #FFFFFF;
  box-sizing: border-box;
  line-height: 88rpx;
}
/* 深色主题下输入框仍保持白底深字，确保 4.5:1 对比度 */
.theme-kuromi .field-input {
  background: #FFFFFF;
  color: #1D2129;
  border-color: #E5E6EB;
}
.field-input:focus {
  border-color: var(--color-primary);
}
.modal-hint {
  font-size: 24rpx;
  color: var(--color-text-secondary);
  text-align: right;
  margin-bottom: 24rpx;
  line-height: 1.4;
}
.modal-actions {
  display: flex;
  gap: 20rpx;
}
.modal-btn {
  flex: 1;
  height: 88rpx;
  min-height: 88rpx;
  border-radius: 12rpx;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: opacity 200ms, transform 200ms;
  box-sizing: border-box;
}
.modal-btn:active {
  opacity: 0.9;
  transform: scale(0.98);
}
.modal-btn.cancel {
  background: var(--skeleton-1);
  color: var(--color-text-secondary);
  border: 1rpx solid var(--card-border);
}
.theme-kuromi .modal-btn.cancel {
  background: #F0F2F6;
  color: #4E5969;
}
.modal-btn.confirm {
  background: var(--color-primary);
  color: #fff;
  border: 1rpx solid var(--color-primary);
}
.subscribe-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  background: #e8f8e8;
  color: #1aad19;
  border-radius: 12rpx;
  padding: 10rpx 16rpx;
  margin-bottom: 20rpx;
  font-size: 24rpx;
}
.badge-text {
  font-size: 24rpx;
}
</style>
