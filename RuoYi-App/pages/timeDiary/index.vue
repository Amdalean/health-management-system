<template>
  <view class="diary-page" :class="themeClass">
    <!-- 顶部搜索栏 -->
    <view class="search-header">
      <view class="search-box" :class="{ 'search-box--active': searchFocused }">
        <uni-icons type="search" size="18" color="#B0B8C4"></uni-icons>
        <input
          class="search-input"
          v-model="query.title"
          placeholder="搜索事项..."
          placeholder-style="color:#C0C8D4"
          confirm-type="search"
          @focus="searchFocused = true"
          @blur="searchFocused = false"
          @confirm="handleQuery"
        />
        <view v-if="query.title" class="search-clear" @click="resetQuery">
          <uni-icons type="clear" size="16" color="#C0C4CC"></uni-icons>
        </view>
      </view>
      <!-- 日期筛选 -->
      <view class="date-filter">
        <picker mode="date" :value="selectedDate" @change="onDateChange">
          <view class="date-btn" :class="{ 'date-btn--active': selectedDate }">
            <uni-icons type="calendar" size="14" :color="selectedDate ? '#409EFF' : '#909399'"></uni-icons>
            <text class="date-btn-text" :class="{ 'date-btn-text--active': selectedDate }">{{ dateButtonText }}</text>
            <view v-if="selectedDate" class="date-btn-clear" @click.stop="clearDate">
              <uni-icons type="clear" size="14" color="#C0C4CC"></uni-icons>
            </view>
          </view>
        </picker>
      </view>
    </view>

    <!-- 月度汇总卡片 -->
    <view class="summary-card" v-if="list.length > 0 || stats.totalMinutes > 0">
      <view class="summary-top">
        <view class="month-nav">
          <view class="nav-arrow" :class="{ 'nav-arrow--disabled': isFirstMonth }" @click="prevMonth">
            <uni-icons type="arrowleft" size="20" :color="isFirstMonth ? '#C0C4CC' : '#606266'"></uni-icons>
          </view>
          <view class="summary-month">
            <text class="month-text">{{ summaryMonth }}</text>
            <text class="month-count">{{ totalCount }} 条记录</text>
          </view>
          <view class="nav-arrow" :class="{ 'nav-arrow--disabled': isLastMonth }" @click="nextMonth">
            <uni-icons type="arrowright" size="20" :color="isLastMonth ? '#C0C4CC' : '#606266'"></uni-icons>
          </view>
        </view>
        <view class="summary-total">
          <text class="total-label">累计时长</text>
          <text class="total-amount">{{ totalDurationText }}</text>
        </view>
      </view>
      <!-- 分类分布条 -->
      <view class="type-bar" v-if="categoryDistribution.length > 0">
        <view
          v-for="t in categoryDistribution"
          :key="t.category"
          class="type-bar-segment"
          :style="{ width: t.percent + '%', backgroundColor: t.color }"
        ></view>
      </view>
      <view class="type-legend" v-if="categoryDistribution.length > 0">
        <view
          v-for="t in categoryDistribution"
          :key="t.category"
          class="legend-item"
        >
          <view class="legend-dot" :style="{ backgroundColor: t.color }"></view>
          <text class="legend-text">{{ t.label }}</text>
          <text class="legend-amount">{{ formatDuration(t.totalMinutes) }}</text>
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
            <text class="date-amount">{{ formatDuration(group.dayTotal) }}</text>
          </view>
          <!-- 卡片列表 -->
          <view class="group-cards">
            <view
              v-for="item in group.items"
              :key="item.diaryId"
              class="diary-card"
              @click="goEdit(item)"
              @longpress="doDelete(item)"
            >
              <view class="diary-card__time">
                <text class="diary-card__range">{{ item.startTime }} - {{ item.endTime }}</text>
                <text class="diary-card__duration">{{ formatDuration(item.durationMin) }}</text>
              </view>
              <view class="diary-card__body">
                <view class="diary-card__top">
                  <view class="diary-card__tag" :style="{ backgroundColor: getCategoryBg(item.category) }">
                    <text class="diary-card__emoji">{{ getCategoryEmoji(item.category) }}</text>
                    <text class="diary-card__tag-text" :style="{ color: getCategoryColor(item.category) }">{{ item.category }}</text>
                  </view>
                  <text class="diary-card__title">{{ item.title }}</text>
                </view>
                <view class="diary-card__bottom" v-if="item.description">
                  <text class="diary-card__remark">{{ item.description }}</text>
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
            <uni-icons type="calendar" size="40" color="#D0D5DD"></uni-icons>
          </view>
          <view class="empty-decoration empty-decoration--1"></view>
          <view class="empty-decoration empty-decoration--2"></view>
        </view>
        <text class="empty-title">还没有时间记录</text>
        <text class="empty-desc">点击下方按钮，记录你的每一刻时间</text>
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
import { listTimeDiary, delTimeDiary, getTimeDiaryStats } from '@/api/main/timeDiary'
import themeMixin from '@/mixins/theme'

// 分类配置（与 PC 端 timeDiary/index.vue 保持一致）
const CATEGORY_MAP = {
  '工作': { label: '工作', color: '#5B9BD5', bg: '#EAF1FB', emoji: '💼' },
  '学习': { label: '学习', color: '#70AD47', bg: '#EDF6E7', emoji: '📚' },
  '生活': { label: '生活', color: '#ED7D31', bg: '#FDF0E6', emoji: '🏠' },
  '其他': { label: '其他', color: '#A5A5A5', bg: '#F2F2F2', emoji: '🎮' }
}

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

export default {
  mixins: [themeMixin],
  data() {
    return {
      query: {
        pageNum: 1,
        pageSize: 20,
        title: ''
      },
      /** 单日筛选（YYYY-MM-DD），为空表示按月份浏览 */
      selectedDate: '',
      list: [],
      loading: false,
      loadingMore: false,
      hasMore: true,
      searchFocused: false,
      /** 当月记录总数（来自列表接口 total） */
      totalCount: 0,
      /** 当前查看的月份，格式 YYYY-MM */
      currentMonth: '',
      stats: {
        totalMinutes: 0,
        categoryDistribution: []
      }
    }
  },
  computed: {
    totalDurationText() {
      return this.formatDuration(this.stats.totalMinutes)
    },
    summaryMonth() {
      if (!this.currentMonth) return ''
      const [y, m] = this.currentMonth.split('-')
      return `${y}年${parseInt(m)}月`
    },
    /** 当前月份是否为最早的可导航边界 */
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
    categoryDistribution() {
      return this.stats.categoryDistribution
    },
    dateButtonText() {
      if (!this.selectedDate) return '全部'
      const [y, m, d] = this.selectedDate.split('-')
      return `${parseInt(m)}月${parseInt(d)}日`
    },
    groupedList() {
      const groups = {}
      this.list.forEach(item => {
        const dateStr = this.formatDate(item.diaryDate)
        if (!groups[dateStr]) {
          groups[dateStr] = { date: dateStr, dateLabel: this.formatDateLabel(item.diaryDate), items: [], dayTotal: 0 }
        }
        groups[dateStr].items.push(item)
        groups[dateStr].dayTotal += parseInt(item.durationMin) || 0
      })
      return Object.values(groups)
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
    // 初始化当前月份
    const now = new Date()
    this.currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    this.query.pageNum = 1
    this.list = []
    this.hasMore = true
    this.getList()
    this.fetchStats()
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
    getList() {
      this.loading = true
      const query = {
        ...this.query,
        orderByColumn: 'start_time',
        isAsc: 'asc'
      }
      if (this.selectedDate) {
        query.diaryDate = this.selectedDate
      } else {
        const monthRange = this.monthDateRange(this.currentMonth)
        query.params = {
          ...this.query.params,
          beginTime: monthRange.beginTime,
          endTime: monthRange.endTime
        }
      }
      return listTimeDiary(query).then(res => {
        const rows = res.rows || []
        if (this.query.pageNum === 1) {
          this.list = rows
          this.totalCount = res.total || 0
        } else {
          this.list = this.list.concat(rows)
        }
        this.hasMore = rows.length >= this.query.pageSize
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    // 获取统计（不受分页影响），单日筛选时统计当天
    fetchStats() {
      const range = this.selectedDate
        ? { beginTime: this.selectedDate, endTime: this.selectedDate }
        : this.monthDateRange(this.currentMonth)
      const params = {
        params: {
          beginTime: range.beginTime,
          endTime: range.endTime
        }
      }
      getTimeDiaryStats(params).then(res => {
        const d = res.data || {}
        const rawList = d.categoryDistribution || []
        const totalMinutes = rawList.reduce((s, i) => s + (parseInt(i.totalMinutes) || 0), 0)
        this.stats.totalMinutes = totalMinutes
        const order = ['工作', '学习', '生活', '其他']
        this.stats.categoryDistribution = rawList.map(item => {
          const meta = CATEGORY_MAP[item.category] || CATEGORY_MAP['其他']
          return {
            category: item.category,
            // 分类名以字典值为准（字典新增分类也能正确显示）
            label: item.category,
            color: meta.color,
            totalMinutes: parseInt(item.totalMinutes) || 0,
            percent: totalMinutes > 0 ? Math.round(((parseInt(item.totalMinutes) || 0) / totalMinutes) * 100) : 0
          }
        }).sort((a, b) => order.indexOf(a.category) - order.indexOf(b.category))
      }).catch(() => {
        // 统计接口失败不影响列表展示
      })
    },
    handleQuery() {
      this.query.pageNum = 1
      this.list = []
      this.hasMore = true
      this.getList()
    },
    resetQuery() {
      this.query.title = ''
      this.handleQuery()
    },
    /** 选择具体日期查看当天记录 */
    onDateChange(e) {
      this.selectedDate = e.detail.value
      this.handleQuery()
      this.fetchStats()
    },
    /** 清除日期筛选，回到月度浏览 */
    clearDate() {
      this.selectedDate = ''
      this.handleQuery()
      this.fetchStats()
    },
    /** 切换到上一个月 */
    prevMonth() {
      if (this.isFirstMonth) return
      this.selectedDate = ''
      const [y, m] = this.currentMonth.split('-').map(Number)
      const d = new Date(y, m - 2, 1) // 减一个月
      this.currentMonth = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
      this.refreshMonthData()
    },
    /** 切换到下一个月 */
    nextMonth() {
      if (this.isLastMonth) return
      this.selectedDate = ''
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
      uni.navigateTo({ url: '/pages/timeDiary/add' })
    },
    goEdit(item) {
      uni.navigateTo({ url: '/pages/timeDiary/add?id=' + item.diaryId })
    },
    doDelete(item) {
      uni.showModal({
        title: '确认删除',
        content: `确定删除「${item.title}」吗？`,
        confirmColor: '#F56C6C',
        success: (res) => {
          if (res.confirm) {
            delTimeDiary(item.diaryId).then(() => {
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
    getCategoryColor(category) {
      const meta = CATEGORY_MAP[category]
      return meta ? meta.color : '#A5A5A5'
    },
    getCategoryBg(category) {
      const meta = CATEGORY_MAP[category]
      return meta ? meta.bg : '#F2F2F2'
    },
    getCategoryEmoji(category) {
      const meta = CATEGORY_MAP[category]
      return meta ? meta.emoji : '🎮'
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return String(dateStr).substring(0, 10)
    },
    formatDateLabel(dateStr) {
      if (!dateStr) return ''
      const str = String(dateStr).substring(0, 10)
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
    },
    /** 分钟 → "x h" / "x h y m" / "y m" */
    formatDuration(minutes) {
      const m = parseInt(minutes) || 0
      if (m <= 0) return '0m'
      const h = Math.floor(m / 60)
      const rest = m % 60
      if (h > 0 && rest > 0) return `${h}h ${rest}m`
      if (h > 0) return `${h}h`
      return `${rest}m`
    }
  }
}
</script>

<style scoped>
.diary-page {
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
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.search-box {
  flex: 1;
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

/* ===== 日期筛选 ===== */
.date-filter {
  flex-shrink: 0;
}

.date-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  background: var(--search-bg);
  border: 2rpx solid var(--search-border);
  border-radius: 60rpx;
  padding: 16rpx 20rpx;
  transition: all 0.2s ease;
}

.date-btn--active {
  border-color: var(--color-primary);
  background: var(--card-bg);
}

.date-btn-text {
  font-size: 24rpx;
  color: var(--color-text-secondary);
  font-weight: 600;
}

.date-btn-text--active {
  color: var(--color-primary);
}

.date-btn-clear {
  display: flex;
  align-items: center;
  padding: 4rpx;
  margin-left: 2rpx;
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

.diary-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx;
  position: relative;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.diary-card:active {
  background: var(--skeleton-2);
  transform: scale(0.98);
}

.diary-card + .diary-card {
  border-top: 1rpx solid var(--card-border);
}

.diary-card__time {
  width: 140rpx;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6rpx;
}

.diary-card__range {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: -0.5rpx;
}

.diary-card__duration {
  font-size: 22rpx;
  font-weight: 600;
  color: var(--color-accent);
  background: var(--skeleton-2);
  border-radius: 8rpx;
  padding: 2rpx 10rpx;
}

.diary-card__body {
  flex: 1;
  min-width: 0;
}

.diary-card__top {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 6rpx;
}

.diary-card__tag {
  display: flex;
  align-items: center;
  gap: 4rpx;
  border-radius: 12rpx;
  padding: 4rpx 10rpx;
  flex-shrink: 0;
}

.diary-card__emoji {
  font-size: 20rpx;
}

.diary-card__tag-text {
  font-size: 22rpx;
  font-weight: 700;
}

.diary-card__title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--color-text);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.diary-card__bottom {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.diary-card__remark {
  font-size: 24rpx;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
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
