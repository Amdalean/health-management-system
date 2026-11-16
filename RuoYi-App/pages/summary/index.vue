<template>
  <view class="summary-page" :class="themeClass">
    <!-- 年/月筛选：年默认本年，汇总跟随筛选 add by CYQ 2026-09-01 -->
    <view class="search-header">
      <view class="filter-row">
        <picker :range="yearRange" :value="yearIndex" @change="onYearChange">
          <view class="filter-btn filter-btn--active">
            <uni-icons type="calendar" size="14" color="#0F172A"></uni-icons>
            <text class="filter-text filter-text--active">{{ query.year }}年</text>
          </view>
        </picker>
        <picker :range="monthRange" range-key="label" :value="monthIndex" @change="onMonthSelect">
          <view class="filter-btn" :class="{ 'filter-btn--active': query.month }">
            <text class="filter-text" :class="{ 'filter-text--active': query.month }">{{ monthLabel }}</text>
            <view v-if="query.month" class="filter-clear" @click.stop="clearMonthOnly">
              <uni-icons type="clear" size="14" color="#94A3B8"></uni-icons>
            </view>
          </view>
        </picker>
        <view class="search-box" :class="{ 'search-box--active': searchFocused }">
          <uni-icons type="search" size="15" color="#94A3B8"></uni-icons>
          <input class="search-input" v-model="query.remark" placeholder="搜索备注" placeholder-style="color:#94A3B8" confirm-type="search" @focus="searchFocused=true" @blur="searchFocused=false" @confirm="handleQuery" />
          <view v-if="query.remark" class="search-clear" @click="resetQuery"><uni-icons type="clear" size="14" color="#94A3B8"></uni-icons></view>
        </view>
      </view>
    </view>

    <!-- 汇总卡：深色 Executive（改回改前深色样式，数据已修复为时点值） -->
    <view class="summary-card" v-if="summaryStat.totalCount > 0 || list.length > 0">
      <view class="summary-top">
        <view class="summary-month">
          <text class="month-text">{{ statTitle }}</text>
          <text class="month-count">{{ summaryStat.totalCount }} 笔汇总</text>
        </view>
        <view class="summary-total">
          <text class="total-label">结余</text>
          <text class="total-amount">¥{{ summaryStat.totalBalance }}</text>
        </view>
      </view>
      <view class="stat-row">
        <view class="stat-item">
          <text class="stat-label">收入</text>
          <text class="stat-value stat-value--income">¥{{ summaryStat.totalIncome }}</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-label">支出</text>
          <text class="stat-value stat-value--expense">¥{{ summaryStat.totalExpense }}</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-label">当前存款</text>
          <text class="stat-value">¥{{ summaryStat.currentDeposit }}</text>
        </view>
      </view>
    </view>

    <!-- 骨架屏 -->
    <view v-if="loading && list.length === 0" class="skeleton-list">
      <view v-for="i in 3" :key="i" class="skeleton-card">
        <view class="skeleton-line skeleton-line--title"></view>
        <view class="skeleton-line skeleton-line--sub"></view>
        <view class="skeleton-line skeleton-line--sub" style="width:40%"></view>
      </view>
    </view>

    <!-- 列表 -->
    <view class="list-area" v-if="!loading || list.length > 0">
      <template v-if="groupedList.length > 0">
        <view v-for="group in groupedList" :key="group.key" class="date-group">
          <view class="date-header">
            <view class="date-dot"></view>
            <text class="date-text">{{ group.label }}</text>
            <text class="date-amount">¥{{ group.groupBalance }}</text>
          </view>
          <view class="group-cards">
            <view v-for="item in group.items" :key="item.id" class="summary-card-item" @click="goEdit(item)" @longpress="doDelete(item)">
              <view class="card-top">
                <view class="card-month">
                  <text class="card-year">{{ item.year }}</text>
                  <text class="card-month-num">{{ item.month }}</text>
                </view>
                <view class="card-main">
                  <view class="amount-row">
                    <text class="amount-val amount-val--income">+¥{{ formatMoney(item.income) }}</text>
                    <text class="amount-dot">·</text>
                    <text class="amount-val amount-val--expense">-¥{{ formatMoney(item.expense) }}</text>
                  </view>
                  <view class="deposit-row">
                    <text class="deposit-text">期初 ¥{{ formatMoney(item.startDeposit) }} → 期末 ¥{{ formatMoney(item.endDeposit) }}</text>
                  </view>
                </view>
                <view class="card-balance">
                  <text class="balance-val" :class="{ 'balance-val--neg': parseFloat(item.balance) < 0 }">¥{{ formatMoney(item.balance) }}</text>
                  <text class="balance-label">结余</text>
                </view>
              </view>
              <view v-if="item.remark" class="card-remark"><text class="remark-text">{{ item.remark }}</text></view>
              <view class="card-footer">
                <text class="footer-text">{{ item.createByName || item.createBy }} · {{ formatDate(item.createTime) }}</text>
                <uni-icons type="forward" size="12" color="#CBD5E1"></uni-icons>
              </view>
            </view>
          </view>
        </view>
        <view class="load-more-wrap" v-if="list.length > 0"><uni-load-more :status="loadMoreStatus" :iconSize="16" color="#94A3B8" /></view>
      </template>
      <view v-else-if="!loading" class="empty-state">
        <view class="empty-illustration">
          <view class="empty-circle"><uni-icons type="wallet" size="36" color="#CBD5E1"></uni-icons></view>
        </view>
        <text class="empty-title">还没有财务汇总</text>
        <text class="empty-desc">点击右下角按钮，创建本月汇总</text>
      </view>
    </view>

    <!-- FAB -->
    <view class="fab" @click="goAdd"><view class="fab__inner"><uni-icons type="plusempty" size="22" color="#fff"></uni-icons></view></view>
  </view>
</template>

<script>
import { listSummary, delSummary, getSummaryStat } from '@/api/main/summary'
import themeMixin from '@/mixins/theme'

export default {
  mixins: [themeMixin],
  data() {
    const curYear = new Date().getFullYear()
    const years = []
    for (let y = curYear - 5; y <= curYear + 1; y++) years.push(String(y))
    return {
      query: { pageNum: 1, pageSize: 20, year: curYear, month: null, remark: '' },
      queryMonth: '',
      list: [],
      total: 0,
      stat: null,
      yearRange: years,
      monthRange: [{ label: '全部月份', value: null }, ...Array.from({length:12}, (_,i)=>({ label: `${i+1}月`, value: i+1 }))],
      loading: false,
      loadingMore: false,
      hasMore: true,
      searchFocused: false
    }
  },
  computed: {
    yearIndex() {
      const idx = this.yearRange.indexOf(String(this.query.year))
      return idx >= 0 ? idx : this.yearRange.length - 2
    },
    monthIndex() {
      if (!this.query.month) return 0
      return this.monthRange.findIndex(m => m.value === this.query.month)
    },
    monthLabel() {
      if (!this.query.month) return '全部月份'
      return `${this.query.month}月`
    },
    queryMonthLabel() {
      if (!this.queryMonth) return '全部月份'
      const [y, m] = this.queryMonth.split('-')
      return `${y}年${parseInt(m)}月`
    },
    statTitle() {
      if (this.query.year && this.query.month) return `${this.query.year}年${this.query.month}月`
      if (this.query.year) return `${this.query.year}年`
      return '全部'
    },
    summaryStat() {
      // 优先使用后端聚合（全量准确），失败回退前端分页计算 add by CYQ 2026-09-01
      if (this.stat) {
        return {
          totalCount: String(this.stat.totalCount || 0),
          totalIncome: parseFloat(this.stat.totalIncome || 0).toFixed(2),
          totalExpense: parseFloat(this.stat.totalExpense || 0).toFixed(2),
          totalBalance: parseFloat(this.stat.totalBalance || 0).toFixed(2),
          currentDeposit: parseFloat(this.stat.currentDeposit || 0).toFixed(2)
        }
      }
      let totalIncome = 0, totalExpense = 0, totalBalance = 0
      const src = this.list
      src.forEach(i => {
        totalIncome += parseFloat(i.income) || 0
        totalExpense += parseFloat(i.expense) || 0
        totalBalance += parseFloat(i.balance) || 0
      })
      // 当前存款 = 按年月倒序第一条的期末（时点值，非累计）
      let currentDeposit = 0
      if (src.length > 0) {
        const latest = [...src].sort((a,b)=> (b.year!==a.year? b.year-a.year : b.month-a.month))[0]
        currentDeposit = parseFloat(latest.endDeposit) || 0
      }
      return {
        totalCount: String(this.total || src.length),
        totalIncome: totalIncome.toFixed(2),
        totalExpense: totalExpense.toFixed(2),
        totalBalance: totalBalance.toFixed(2),
        currentDeposit: currentDeposit.toFixed(2)
      }
    },
    groupedList() {
      const groups = {}
      this.list.forEach(item => {
        const key = `${item.year}-${String(item.month).padStart(2,'0')}`
        const label = `${item.year}年${item.month}月`
        if (!groups[key]) groups[key] = { key, label, items: [], groupBalance: 0 }
        groups[key].items.push(item)
        groups[key].groupBalance += parseFloat(item.balance) || 0
      })
      // 倒序：最新年月在前 add by CYQ 2026-08-31
      return Object.values(groups).map(g => ({ ...g, groupBalance: g.groupBalance.toFixed(2) })).sort((a,b)=> b.key.localeCompare(a.key))
    },
    loadMoreStatus() {
      if (this.loadingMore) return 'loading'
      if (!this.hasMore) return 'noMore'
      return 'more'
    }
  },
  onShow() {
    this.query.pageNum = 1
    this.list = []
    this.hasMore = true
    this.getList()
  },
  onPullDownRefresh() {
    this.query.pageNum = 1
    this.list = []
    this.hasMore = true
    this.getList().then(() => uni.stopPullDownRefresh())
  },
  onReachBottom() { this.loadMore() },
  methods: {
    getList() {
      this.loading = true
      const q = { pageNum: this.query.pageNum, pageSize: this.query.pageSize }
      if (this.query.year) q.year = this.query.year
      if (this.query.month) q.month = this.query.month
      if (this.query.remark) q.remark = this.query.remark
      // 后端聚合统计（全量，不受分页影响）add by CYQ 2026-09-01
      const statQuery = {}
      if (this.query.year) statQuery.year = this.query.year
      if (this.query.month) statQuery.month = this.query.month
      if (this.query.remark) statQuery.remark = this.query.remark
      getSummaryStat(statQuery).then(res => { this.stat = res.data || null }).catch(()=>{ this.stat = null })
      return listSummary(q).then(res => {
        let rows = res.rows || []
        this.total = res.total || rows.length
        // 前端兜底倒序：兼容后端 asc 的旧数据，确保最新在前 add by CYQ 2026-08-31
        rows = rows.slice().sort((a,b)=> (b.year!==a.year? b.year-a.year : b.month-a.month))
        if (this.query.pageNum === 1) this.list = rows
        else this.list = this.list.concat(rows).sort((a,b)=> (b.year!==a.year? b.year-a.year : b.month-a.month))
        this.hasMore = rows.length >= this.query.pageSize
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.query.pageNum = 1; this.list=[]; this.hasMore=true; this.getList() },
    resetQuery() { this.query.remark=''; this.handleQuery() },
    onYearChange(e) {
      const y = parseInt(this.yearRange[e.detail.value])
      this.query.year = y
      // 保持月份筛选，汇总跟随年+月 add by CYQ 2026-09-01
      this.queryMonth = this.query.month ? `${y}-${String(this.query.month).padStart(2,'0')}` : ''
      this.handleQuery()
    },
    onMonthSelect(e) {
      const m = this.monthRange[e.detail.value].value
      this.query.month = m
      this.queryMonth = m ? `${this.query.year}-${String(m).padStart(2,'0')}` : ''
      this.handleQuery()
    },
    clearMonthOnly() { this.query.month=null; this.queryMonth=''; this.handleQuery() },
    onMonthChange(e) {
      const val = e.detail.value // YYYY-MM
      this.queryMonth = val
      const [y,m] = val.split('-')
      this.query.year = parseInt(y)
      this.query.month = parseInt(m)
      this.handleQuery()
    },
    clearMonth() { this.queryMonth=''; this.query.year=new Date().getFullYear(); this.query.month=null; this.handleQuery() },
    loadMore() {
      if (this.loadingMore || !this.hasMore) return
      this.loadingMore = true
      this.query.pageNum++
      this.getList().then(() => { this.loadingMore = false })
    },
    goAdd() { uni.navigateTo({ url: '/pages/summary/add' }) },
    goEdit(item) { uni.navigateTo({ url: '/pages/summary/add?id=' + item.id }) },
    doDelete(item) {
      uni.showModal({ title: '确认删除', content: `确定删除 ${item.year}年${item.month}月 汇总吗？`, confirmColor: '#EF4444', success: (res) => {
        if (res.confirm) delSummary(item.id).then(() => { uni.showToast({ title:'已删除', icon:'success'}); this.query.pageNum=1; this.list=[]; this.hasMore=true; this.getList() })
      }})
    },
    formatMoney(v) { const n=parseFloat(v); return isNaN(n)?'0.00':n.toFixed(2) },
    formatDate(d) { if(!d) return ''; return String(d).substring(0,10) }
  }
}
</script>

<style scoped>
.summary-page { min-height: 100vh; background: #F8FAFC; padding-bottom: 120rpx; }
/* 搜索 — Swiss 极简 */
.search-header { padding: 16rpx 20rpx 12rpx; position: sticky; top:0; z-index:10; background: #F8FAFC; }
.filter-row { display:flex; align-items:center; gap:12rpx; }
.filter-btn { display:flex; align-items:center; gap:6rpx; background: #FFFFFF; border:1rpx solid #E2E8F0; border-radius: 999rpx; padding:14rpx 16rpx; }
.filter-btn--active { border-color: #0F172A; }
.filter-text { font-size:24rpx; color: #64748B; font-weight:500; }
.filter-text--active { color: #0F172A; font-weight:600; }
.filter-clear { display:flex; align-items:center; margin-left:4rpx; }
.search-box { flex:1; display:flex; align-items:center; gap:10rpx; background: #FFFFFF; border-radius:999rpx; padding:14rpx 18rpx; border:1rpx solid #E2E8F0; }
.search-box--active { border-color: #0F172A; }
.search-input { flex:1; font-size:26rpx; color: #0F172A; background:transparent; border:none; }
.search-clear { padding:6rpx; display:flex; align-items:center; background: #F1F5F9; border-radius:50%; }

/* 汇总卡 — 深色卡（改前样式，保留当前存款时点值修复） */
.summary-card { margin: 16rpx 20rpx 12rpx; background: #1E293B; border-radius: 24rpx; padding: 28rpx; border:1rpx solid #334155; box-shadow: 0 8rpx 24rpx rgba(15,23,42,0.18); position:relative; overflow:hidden; }
.summary-top { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:18rpx; }
.summary-month { display:flex; flex-direction:column; gap:6rpx; }
.month-text { font-size:26rpx; color: #F8FAFC; font-weight:700; }
.month-count { font-size:20rpx; color: #94A3B8; }
.summary-total { display:flex; flex-direction:column; align-items:flex-end; gap:4rpx; }
.total-label { font-size:20rpx; color: #94A3B8; letter-spacing:2rpx; }
.total-amount { font-size:42rpx; font-weight:800; color: #F8FAFC; letter-spacing:-0.5rpx; }
.stat-row { display:flex; align-items:center; margin-top:12rpx; }
.stat-item { flex:1; display:flex; flex-direction:column; align-items:center; gap:6rpx; }
.stat-label { font-size:20rpx; color: #94A3B8; }
.stat-value { font-size:26rpx; font-weight:800; color: #F8FAFC; }
.stat-value--income { color: #4ADE80; }
.stat-value--expense { color: #F87171; }
.stat-divider { width:1rpx; height:40rpx; background: rgba(255,255,255,0.12); margin:0 10rpx; }

/* 骨架 */
.skeleton-list { padding: 12rpx 20rpx; }
.skeleton-card { background: #FFFFFF; border-radius:20rpx; padding:24rpx; margin-bottom:12rpx; border:1rpx solid #E2E8F0; display:flex; flex-direction:column; gap:12rpx; }
.skeleton-line { border-radius:8rpx; background: linear-gradient(90deg, #F1F5F9 25%, #E2E8F0 50%, #F1F5F9 75%); background-size:200% 100%; animation: shimmer 1.6s ease-in-out infinite; height:24rpx; }
.skeleton-line--title { width:50%; }
.skeleton-line--sub { width:75%; height:18rpx; }
@keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }

/* 列表 — Swiss 分组 */
.list-area { padding: 0 20rpx; }
.date-group { margin-bottom: 16rpx; }
.date-header { display:flex; align-items:center; gap:10rpx; padding: 14rpx 4rpx 10rpx; }
.date-dot { width:8rpx; height:8rpx; border-radius:50%; background: #0F172A; }
.date-text { font-size:24rpx; color: #0F172A; font-weight:700; }
.date-amount { font-size:22rpx; color: #64748B; margin-left:auto; font-weight:600; }
.group-cards { background: #FFFFFF; border-radius:20rpx; overflow:hidden; border:1rpx solid #E2E8F0; box-shadow: 0 1rpx 8rpx rgba(15,23,42,0.04); }
.summary-card-item { padding: 20rpx 20rpx 16rpx; position:relative; }
.summary-card-item:active { background: #F8FAFC; }
.summary-card-item + .summary-card-item { border-top:1rpx solid #F1F5F9; }
.card-top { display:flex; align-items:center; gap:14rpx; }
.card-month { width:72rpx; height:72rpx; border-radius:16rpx; background: #F1F5F9; border:1rpx solid #E2E8F0; display:flex; flex-direction:column; align-items:center; justify-content:center; flex-shrink:0; }
.card-year { font-size:16rpx; color: #94A3B8; font-weight:600; line-height:1; margin-bottom:2rpx; }
.card-month-num { font-size:28rpx; font-weight:800; color: #0F172A; line-height:1; }
.card-main { flex:1; display:flex; flex-direction:column; gap:6rpx; min-width:0; }
.amount-row { display:flex; align-items:center; gap:8rpx; }
.amount-val { font-size:24rpx; font-weight:700; }
.amount-val--income { color: #059669; }
.amount-val--expense { color: #DC2626; }
.amount-dot { font-size:20rpx; color: #CBD5E1; }
.deposit-row { display:flex; align-items:center; }
.deposit-text { font-size:20rpx; color: #64748B; font-weight:500; }
.card-balance { text-align:right; min-width:160rpx; flex-shrink:0; }
.balance-val { display:block; font-size:28rpx; font-weight:800; color: #0F172A; line-height:1.1; }
.balance-val--neg { color: #DC2626; }
.balance-label { font-size:18rpx; color: #94A3B8; font-weight:500; letter-spacing: 1rpx; }
.card-remark { margin-top:12rpx; font-size:22rpx; color: #475569; background: #F8FAFC; border:1rpx solid #F1F5F9; border-radius:12rpx; padding:10rpx 12rpx; line-height:1.5; }
.remark-text { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; display:block; }
.card-footer { margin-top:10rpx; display:flex; justify-content:space-between; align-items:center; }
.footer-text { font-size:20rpx; color: #94A3B8; }

/* 空状态 */
.empty-state { display:flex; flex-direction:column; align-items:center; padding:120rpx 40rpx 80rpx; }
.empty-circle { width:120rpx; height:120rpx; border-radius:50%; background: #FFFFFF; border:1rpx solid #E2E8F0; display:flex; align-items:center; justify-content:center; }
.empty-title { font-size:30rpx; color: #0F172A; font-weight:700; margin:16rpx 0 8rpx; }
.empty-desc { font-size:24rpx; color: #94A3B8; text-align:center; }
.empty-illustration { position:relative; margin-bottom:8rpx; }
.load-more-wrap { padding:20rpx 0 40rpx; }
.fab { position:fixed; right:28rpx; bottom:72rpx; z-index:100; }
.fab__inner { width:96rpx; height:96rpx; border-radius:50%; background: #0F172A; display:flex; align-items:center; justify-content:center; box-shadow: 0 8rpx 24rpx rgba(15,23,42,0.18); }
.fab__inner:active { transform: scale(0.96); }
</style>
