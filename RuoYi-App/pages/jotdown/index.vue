<template>
  <view class="jotdown-page" :class="themeClass">
    <!-- 顶部搜索栏 -->
    <view class="search-header">
      <view class="search-box" :class="{ 'search-box--active': searchFocused }">
        <uni-icons type="search" size="18" color="#B0B8C4"></uni-icons>
        <input
          class="search-input"
          v-model="query.description"
          placeholder="搜索记录..."
          placeholder-style="color:#C0C8D4"
          confirm-type="search"
          @focus="searchFocused = true"
          @blur="searchFocused = false"
          @confirm="handleQuery"
        />
        <view v-if="query.description" class="search-clear" @click="resetQuery">
          <uni-icons type="clear" size="16" color="#C0C4CC"></uni-icons>
        </view>
      </view>
    </view>

    <!-- 月度汇总卡片 -->
    <view class="summary-card" v-if="list.length > 0 || stats.totalCount > 0">
      <view class="summary-top">
        <view class="month-nav">
          <view class="nav-arrow" :class="{ 'nav-arrow--disabled': isFirstMonth }" @click="prevMonth">
            <uni-icons type="arrowleft" size="20" :color="isFirstMonth ? '#C0C4CC' : '#606266'"></uni-icons>
          </view>
          <view class="summary-month">
            <text class="month-text">{{ summaryMonth }}</text>
            <text class="month-count">{{ stats.totalCount || list.length }} 笔支出</text>
          </view>
          <view class="nav-arrow" :class="{ 'nav-arrow--disabled': isLastMonth }" @click="nextMonth">
            <uni-icons type="arrowright" size="20" :color="isLastMonth ? '#C0C4CC' : '#606266'"></uni-icons>
          </view>
        </view>
        <view class="summary-total">
          <text class="total-label">总支出</text>
          <text class="total-amount">¥{{ totalAmount }}</text>
        </view>
      </view>
      <!-- 类型分布条 -->
      <view class="type-bar" v-if="typeDistribution.length > 0">
        <view
          v-for="t in typeDistribution"
          :key="t.type"
          class="type-bar-segment"
          :style="{ width: t.percent + '%', backgroundColor: t.color }"
        ></view>
      </view>
      <view class="type-legend" v-if="typeDistribution.length > 0">
        <view
          v-for="t in typeDistribution.slice(0, 4)"
          :key="t.type"
          class="legend-item"
        >
          <view class="legend-dot" :style="{ backgroundColor: t.color }"></view>
          <text class="legend-text">{{ t.label }}</text>
          <text class="legend-amount">¥{{ t.total }}</text>
        </view>
      </view>
    </view>

    <!-- 骨架屏 -->
    <view v-if="loading && list.length === 0" class="skeleton-list">
      <view v-for="i in 4" :key="i" class="skeleton-card">
        <view class="skeleton-icon"></view>
        <view class="skeleton-content">
          <view class="skeleton-line skeleton-line--title"></view>
          <view class="skeleton-line skeleton-line--sub"></view>
        </view>
        <view class="skeleton-amount"></view>
      </view>
    </view>

    <!-- 列表区域 -->
    <view class="list-area" v-if="!loading || list.length > 0">
      <template v-if="groupedList.length > 0">
        <view
          v-for="group in groupedList"
          :key="group.date"
          class="date-group"
        >
          <!-- 日期头 -->
          <view class="date-header">
            <view class="date-dot"></view>
            <text class="date-text">{{ group.dateLabel }}</text>
            <text class="date-amount">-¥{{ group.dayTotal }}</text>
          </view>
          <!-- 卡片列表 -->
          <view class="group-cards">
            <view
              v-for="item in group.items"
              :key="item.id"
              class="jot-card"
              @click="goEdit(item)"
              @longpress="doDelete(item)"
            >
              <view class="jot-card__icon" :style="{ backgroundColor: getTypeBg(item.expenseType) }">
                <uni-icons :type="getTypeIcon(item.expenseType)" size="20" :color="getTypeColor(item.expenseType)"></uni-icons>
              </view>
              <view class="jot-card__body">
                <view class="jot-card__top">
                  <text class="jot-card__desc">{{ item.description }}</text>
                  <text class="jot-card__amount">-¥{{ formatAmount(item.amount) }}</text>
                </view>
                <view class="jot-card__bottom">
                  <text class="jot-card__type" :style="{ color: getTypeColor(item.expenseType) }">
                    {{ getTypeLabel(item.expenseType) }}
                  </text>
                  <text class="jot-card__remark" v-if="item.remarks">{{ item.remarks }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 加载更多 -->
        <view class="load-more-wrap" v-if="list.length > 0">
          <uni-load-more :status="loadMoreStatus" :iconSize="16" color="#909399" />
        </view>
      </template>

      <!-- 空状态 -->
      <view v-else-if="!loading" class="empty-state">
        <view class="empty-illustration">
          <view class="empty-circle">
            <uni-icons type="wallet" size="40" color="#D0D5DD"></uni-icons>
          </view>
          <view class="empty-decoration empty-decoration--1"></view>
          <view class="empty-decoration empty-decoration--2"></view>
        </view>
        <text class="empty-title">还没有记账</text>
        <text class="empty-desc">点击下方按钮，开始记录每一笔支出</text>
      </view>
    </view>

    <!-- FAB 新增按钮 -->
    <view class="fab" @click="goAdd">
      <view class="fab__inner">
        <uni-icons type="plusempty" size="24" color="#fff"></uni-icons>
      </view>
    </view>
  </view>
</template>

<script>
import { listJotdown, delJotdown, jotdownStats } from '@/api/main/jotdown'
import themeMixin from '@/mixins/theme'

const EXPENSE_TYPE_MAP = {
  '1': { label: '餐饮', color: '#FF8FA0', bg: '#FFE8ED', icon: 'fire' },
  '2': { label: '交通', color: '#7EC8E3', bg: '#E8F6FC', icon: 'navigate' },
  '3': { label: '购物', color: '#FFB088', bg: '#FFF0E8', icon: 'shop' },
  '4': { label: '娱乐', color: '#7ED7A8', bg: '#E8F8F0', icon: 'flag' },
  '5': { label: '医疗', color: '#B592D8', bg: '#F3EDFA', icon: 'heart-filled' },
  '6': { label: '住房', color: '#B0A0F0', bg: '#F0EDFD', icon: 'home' },
  '9': { label: '其他', color: '#C4BFC6', bg: '#F5F3F5', icon: 'more-filled' }
}

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export default {
  mixins: [themeMixin],
  data() {
    return {
      query: {
        pageNum: 1,
        pageSize: 20,
        description: ''
      },
      list: [],
      loading: false,
      loadingMore: false,
      hasMore: true,
      searchFocused: false,
      /** 当前查看的月份，格式 YYYY-MM */
      currentMonth: '',
      stats: {
        totalAmount: '0.00',
        totalCount: 0,
        typeDistribution: []
      }
    }
  },
  computed: {
    totalAmount() {
      return this.stats.totalAmount
    },
    summaryMonth() {
      if (!this.currentMonth) return ''
      const [y, m] = this.currentMonth.split('-')
      return `${y}年${parseInt(m)}月`
    },
    /** 当前月份是否为注册月份（最早的可导航边界） */
    isFirstMonth() {
      // 允许退回到 2024年1月，足够覆盖所有数据
      return this.currentMonth <= '2024-01'
    },
    /** 当前月份是否为未来月份（不可导航到未来） */
    isLastMonth() {
      if (!this.currentMonth) return true
      const now = new Date()
      const ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      return this.currentMonth >= ym
    },
    typeDistribution() {
      return this.stats.typeDistribution
    },
    groupedList() {
      const groups = {}
      this.list.forEach(item => {
        const dateStr = this.formatDate(item.date)
        if (!groups[dateStr]) {
          groups[dateStr] = { date: dateStr, dateLabel: this.formatDateLabel(item.date), items: [], dayTotal: 0 }
        }
        groups[dateStr].items.push(item)
        groups[dateStr].dayTotal += parseFloat(item.amount) || 0
      })
      return Object.values(groups).map(g => ({
        ...g,
        dayTotal: g.dayTotal.toFixed(2)
      }))
    },
    // 主题
    themeClass() {
      return 'theme-' + (this.$store.state.theme.theme || 'default')
    },
    loadMoreStatus() {
      if (this.loadingMore) return 'loading'
      if (!this.hasMore) return 'noMore'
      return 'more'
    }
  },
  onShow() {
    // 进页面时先定位到最近有数据的月份，避免默认落在无数据的当前月而显示空白
    this.initMonth()
  },
  onPullDownRefresh() {
    this.query.pageNum = 1
    this.list = []
    this.hasMore = true
    this.getList().then(() => {
      uni.stopPullDownRefresh()
    })
  },
  onReachBottom() {
    this.loadMore()
  },
  methods: {
    /** 初始化月份：定位到最近一条数据所属月份，无数据则落到当前月 */
    initMonth() {
      // resolveLatestMonth 内部已兜底，永不 reject
      this.resolveLatestMonth().then(() => {
        this.query.pageNum = 1
        this.list = []
        this.hasMore = true
        this.getList()
        this.fetchStats()
      })
    },
    /** 查最新一条随手记（列表按 date desc 排序），推导月份 YYYY-MM */
    resolveLatestMonth() {
      const fallback = () => {
        const now = new Date()
        this.currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      }
      return listJotdown({ pageNum: 1, pageSize: 1 }).then(res => {
        const rows = res.rows || []
        const latest = rows.length && rows[0].date ? String(rows[0].date) : ''
        if (latest.length >= 7) {
          this.currentMonth = latest.slice(0, 7)
        } else {
          fallback()
        }
      }).catch(fallback)
    },
    getList() {
      this.loading = true
      const monthRange = this.monthDateRange(this.currentMonth)
      const query = {
        ...this.query,
        params: {
          ...this.query.params,
          beginTime: monthRange.beginTime,
          endTime: monthRange.endTime
        }
      }
      return listJotdown(query).then(res => {
        console.log('[jotdown] getList res:', JSON.stringify(res))
        const rows = res.rows || []
        if (this.query.pageNum === 1) {
          this.list = rows
        } else {
          this.list = this.list.concat(rows)
        }
        this.hasMore = rows.length >= this.query.pageSize
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    // 获取统计（不受分页影响），add by CYQ 2026年7月2日
    fetchStats() {
      const monthRange = this.monthDateRange(this.currentMonth)
      const params = {
        ...(this.query.description ? { description: this.query.description } : {}),
        params: {
          beginTime: monthRange.beginTime,
          endTime: monthRange.endTime
        }
      }
      jotdownStats(params).then(res => {
        const d = res.data || {}
        this.stats.totalAmount = d.totalAmount !== undefined ? Number(d.totalAmount).toFixed(2) : '0.00'
        this.stats.totalCount = d.totalCount || 0
        const rawList = d.typeDistribution || []
        this.stats.typeDistribution = rawList.map(item => {
          const typeKey = item.expenseType || '9'
          return {
            type: typeKey,
            total: Number(item.total).toFixed(2),
            ...EXPENSE_TYPE_MAP[typeKey] || EXPENSE_TYPE_MAP['9']
          }
        })
        this.stats.typeDistribution.sort((a, b) => parseFloat(b.total) - parseFloat(a.total))
        const grandTotal = this.stats.typeDistribution.reduce((s, i) => s + parseFloat(i.total), 0)
        this.stats.typeDistribution.forEach(item => {
          item.percent = grandTotal > 0 ? Math.round((parseFloat(item.total) / grandTotal) * 100) : 0
        })
      }).catch(() => {
        // 统计接口失败不影响列表展示
      })
    },
    handleQuery() {
      this.query.pageNum = 1
      this.list = []
      this.hasMore = true
      this.getList()
      this.fetchStats()
    },
    resetQuery() {
      this.query.description = ''
      this.handleQuery()
    },
    /** 切换到上一个月 */
    prevMonth() {
      if (this.isFirstMonth) return
      const [y, m] = this.currentMonth.split('-').map(Number)
      const d = new Date(y, m - 2, 1) // 减一个月
      this.currentMonth = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
      this.refreshMonthData()
    },
    /** 切换到下一个月 */
    nextMonth() {
      if (this.isLastMonth) return
      const [y, m] = this.currentMonth.split('-').map(Number)
      const d = new Date(y, m, 1) // 加一个月
      this.currentMonth = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
      this.refreshMonthData()
    },
    /** 根据 YYYY-MM 计算当月起止日期 */
    monthDateRange(ym) {
      if (!ym) {
        const now = new Date()
        ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      }
      const [y, m] = ym.split('-').map(Number)
      const beginTime = `${y}-${String(m).padStart(2, '0')}-01`
      const lastDay = new Date(y, m, 0).getDate() // 当月最后一天
      const endTime = `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
      return { beginTime, endTime }
    },
    /** 月份切换后刷新列表和统计 */
    refreshMonthData() {
      this.query.pageNum = 1
      this.list = []
      this.hasMore = true
      this.getList()
      this.fetchStats()
    },
    loadMore() {
      if (this.loadingMore || !this.hasMore) return
      this.loadingMore = true
      this.query.pageNum++
      this.getList().then(() => {
        this.loadingMore = false
      })
    },
    goAdd() {
      uni.navigateTo({ url: '/pages/jotdown/add' })
    },
    goEdit(item) {
      uni.navigateTo({ url: '/pages/jotdown/add?id=' + item.id })
    },
    doDelete(item) {
      uni.showModal({
        title: '确认删除',
        content: `确定删除「${item.description}」吗？`,
        confirmColor: '#F56C6C',
        success: (res) => {
          if (res.confirm) {
            delJotdown(item.id).then(() => {
              uni.showToast({ title: '已删除', icon: 'success' })
              this.query.pageNum = 1
              this.list = []
              this.hasMore = true
              this.getList()
              this.fetchStats()
            })
          }
        }
      })
    },
    getTypeLabel(type) {
      const t = EXPENSE_TYPE_MAP[type]
      return t ? t.label : '其他'
    },
    getTypeColor(type) {
      const t = EXPENSE_TYPE_MAP[type]
      return t ? t.color : '#B0B0B0'
    },
    getTypeBg(type) {
      const t = EXPENSE_TYPE_MAP[type]
      return t ? t.bg : '#F5F5F5'
    },
    getTypeIcon(type) {
      const t = EXPENSE_TYPE_MAP[type]
      return t ? t.icon : 'more-filled'
    },
    formatAmount(amount) {
      return (parseFloat(amount) || 0).toFixed(2)
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return String(dateStr).substring(0, 10)
    },
    formatDateLabel(dateStr) {
      if (!dateStr) return ''
      const str = String(dateStr)
      const date = new Date(str)
      const today = new Date()
      const yesterday = new Date(today)
      yesterday.setDate(yesterday.getDate() - 1)

      const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate())
      const todayOnly = new Date(today.getFullYear(), today.getMonth(), today.getDate())
      const yesterdayOnly = new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate())

      if (dateOnly.getTime() === todayOnly.getTime()) {
        return '今天'
      }
      if (dateOnly.getTime() === yesterdayOnly.getTime()) {
        return '昨天'
      }

      const month = date.getMonth() + 1
      const day = date.getDate()
      const weekday = WEEKDAYS[date.getDay()]
      return `${month}月${day}日 ${weekday}`
    }
  }
}
</script>

<style scoped>
.jotdown-page {
  min-height: 100vh;
  background: var(--bg-gradient, linear-gradient(180deg, var(--bg-start) 0%, var(--bg-end) 100%));
  padding-bottom: 120rpx;
}

/* ===== 搜索栏 ===== */
.search-header {
  padding: 20rpx 28rpx 16rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 14rpx;
  background: var(--search-bg);
  backdrop-filter: blur(12rpx);
  border-radius: 60rpx;
  padding: 16rpx 24rpx;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2rpx solid var(--search-border);
  box-shadow: var(--search-shadow),
              inset -2rpx -2rpx 8rpx rgba(255, 255, 255, 0.04),
              inset 2rpx 2rpx 8rpx rgba(0, 0, 0, 0.15);
}

.search-box--active {
  background: var(--card-bg);
  border-color: var(--color-primary);
  box-shadow: 0 6rpx 24rpx var(--color-primary-dim),
              inset -2rpx -2rpx 8rpx rgba(255, 255, 255, 0.06),
              inset 2rpx 2rpx 8rpx rgba(0, 0, 0, 0.15);
  transform: scale(1.02);
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: var(--color-text);
  background: transparent;
  border: none;
  line-height: 1.4;
}

.search-input::placeholder {
  color: var(--color-text-secondary);
}

.search-clear {
  padding: 8rpx;
  display: flex;
  align-items: center;
  background: var(--skeleton-2);
  border-radius: 50%;
  transition: transform 0.2s ease;
}

.search-clear:active {
  transform: scale(0.85);
}

/* ===== 月度汇总 ===== */
.summary-card {
  margin: 16rpx 28rpx 12rpx;
  background: var(--summary-card-bg);
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: var(--card-shadow);
  position: relative;
  overflow: hidden;
  border: 1rpx solid var(--card-border);
}

.summary-card::before {
  content: '';
  position: absolute;
  top: -50rpx;
  right: -30rpx;
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  background: radial-gradient(circle, var(--skeleton-2) 0%, transparent 70%);
  pointer-events: none;
}

.summary-card::after {
  content: '';
  position: absolute;
  bottom: -40rpx;
  left: -40rpx;
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: radial-gradient(circle, var(--skeleton-1) 0%, transparent 70%);
  pointer-events: none;
}

.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24rpx;
  position: relative;
  z-index: 1;
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.nav-arrow {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--card-bg);
  border: 1rpx solid var(--card-border);
  transition: all 0.2s ease;
}

.nav-arrow:active {
  transform: scale(0.88);
  background: var(--skeleton-2);
}

.nav-arrow--disabled {
  opacity: 0.4;
  pointer-events: none;
}

.summary-month {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.month-text {
  font-size: 26rpx;
  color: var(--summary-text-color);
  font-weight: 700;
}

.month-count {
  font-size: 20rpx;
  color: var(--summary-text-muted);
}

.summary-total {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
}

.total-label {
  font-size: 20rpx;
  color: var(--summary-text-muted);
  letter-spacing: 2rpx;
}

.total-amount {
  font-size: 44rpx;
  font-weight: 900;
  color: var(--total-amount-color);
  letter-spacing: -1rpx;
}

.type-bar {
  display: flex;
  height: 12rpx;
  border-radius: 6rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
  gap: 4rpx;
  position: relative;
  z-index: 1;
}

.type-bar-segment {
  border-radius: 6rpx;
  min-width: 8rpx;
  transition: width 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.type-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  position: relative;
  z-index: 1;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.legend-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 8rpx currentColor;
}

.legend-text {
  font-size: 22rpx;
  color: var(--summary-text-muted);
  font-weight: 500;
}

.legend-amount {
  font-size: 22rpx;
  color: var(--summary-text-color);
  font-weight: 700;
}

/* ===== 骨架屏 ===== */
.skeleton-list {
  padding: 20rpx 28rpx;
}

.skeleton-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: var(--card-bg);
  backdrop-filter: blur(10rpx);
  border-radius: 24rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 16rpx;
  box-shadow: var(--card-shadow);
}

.skeleton-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  background: linear-gradient(90deg, var(--skeleton-1) 25%, var(--skeleton-2) 50%, var(--skeleton-1) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.skeleton-line {
  border-radius: 10rpx;
  background: linear-gradient(90deg, var(--skeleton-1) 25%, var(--skeleton-2) 50%, var(--skeleton-1) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
  height: 28rpx;
}

.skeleton-line--title {
  width: 60%;
}

.skeleton-line--sub {
  width: 35%;
  height: 22rpx;
}

.skeleton-amount {
  width: 100rpx;
  height: 32rpx;
  border-radius: 10rpx;
  background: linear-gradient(90deg, var(--skeleton-1) 25%, var(--skeleton-2) 50%, var(--skeleton-1) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ===== 日期分组 ===== */
.list-area {
  padding: 0 28rpx;
}

.date-group {
  margin-bottom: 8rpx;
}

.date-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 24rpx 8rpx 12rpx;
}

.date-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: var(--color-date-dot);
  flex-shrink: 0;
  box-shadow: 0 0 10rpx var(--color-primary-dim);
}

.date-text {
  font-size: 26rpx;
  color: var(--color-text);
  font-weight: 700;
}

.date-amount {
  font-size: 24rpx;
  color: var(--color-accent);
  margin-left: auto;
  font-weight: 700;
}

/* ===== 卡片 ===== */
.group-cards {
  background: var(--card-bg);
  backdrop-filter: blur(10rpx);
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: var(--card-shadow), var(--card-shadow-inset);
}

.jot-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx;
  position: relative;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.jot-card:active {
  background: var(--skeleton-2);
  transform: scale(0.98);
}

.jot-card + .jot-card {
  border-top: 1rpx solid var(--card-border);
}

.jot-card__icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s ease;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
}

.jot-card:active .jot-card__icon {
  transform: scale(0.9) rotate(-5deg);
}

.jot-card__body {
  flex: 1;
  min-width: 0;
}

.jot-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.jot-card__desc {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--color-text);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 16rpx;
}

.jot-card__amount {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--color-amount);
  flex-shrink: 0;
  letter-spacing: -0.5rpx;
}

.jot-card__bottom {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.jot-card__type {
  font-size: 24rpx;
  font-weight: 600;
}

.jot-card__remark {
  font-size: 24rpx;
  color: var(--color-text-secondary);
  max-width: 240rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 加载更多 ===== */
.load-more-wrap {
  padding: 24rpx 0 40rpx;
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 40rpx 80rpx;
  animation: floatIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes floatIn {
  0% { opacity: 0; transform: translateY(30rpx); }
  100% { opacity: 1; transform: translateY(0); }
}

.empty-illustration {
  position: relative;
  margin-bottom: 40rpx;
}

.empty-circle {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: var(--empty-circle-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--empty-shadow);
  animation: bounce 3s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10rpx); }
}

.empty-decoration {
  position: absolute;
  border-radius: 50%;
  animation: twinkle 2s ease-in-out infinite;
}

@keyframes twinkle {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.15); }
}

.empty-decoration--1 {
  width: 28rpx;
  height: 28rpx;
  background: var(--color-primary);
  top: -12rpx;
  right: -24rpx;
  animation-delay: 0.3s;
  box-shadow: 0 0 12rpx var(--color-primary-dim);
}

.empty-decoration--2 {
  width: 20rpx;
  height: 20rpx;
  background: var(--deco-color);
  bottom: 12rpx;
  left: -20rpx;
  animation-delay: 0.6s;
  box-shadow: 0 0 12rpx var(--deco-color);
}

.empty-title {
  font-size: 34rpx;
  color: var(--color-text);
  font-weight: 800;
  margin-bottom: 12rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: var(--color-text-secondary);
  text-align: center;
}

/* ===== FAB ===== */
.fab {
  position: fixed;
  right: 36rpx;
  bottom: 80rpx;
  z-index: 100;
  animation: fabAppear 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes fabAppear {
  0% { opacity: 0; transform: scale(0) translateY(40rpx); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

.fab__inner {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--fab-bg-start) 0%, var(--fab-bg-end) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--fab-shadow),
              0 4rpx 12rpx rgba(0, 0, 0, 0.2),
              inset -3rpx -3rpx 8rpx rgba(255, 255, 255, 0.1),
              inset 3rpx 3rpx 8rpx rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2rpx solid rgba(255, 255, 255, 0.08);
}

.fab__inner:active {
  transform: scale(0.88) rotate(-15deg);
  box-shadow: 0 4rpx 16rpx var(--color-primary-dim),
              inset -2rpx -2rpx 6rpx rgba(255, 255, 255, 0.08),
              inset 2rpx 2rpx 6rpx rgba(0, 0, 0, 0.15);
}

/* ===== 库洛米小装饰 ===== */
.date-header::after {
  content: '♡';
  position: absolute;
  right: 0;
  font-size: 16rpx;
  color: var(--color-primary-dim);
}
</style>
