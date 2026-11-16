<template>
  <div class="app-container">
    <el-row :gutter="12" class="mb8">
      <el-col :span="4" v-for="card in statCards" :key="card.key">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :class="{ money: card.money }">
            {{ card.money ? '¥' + money(stats[card.key]) : (stats[card.key] || 0) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="平台" prop="platform">
        <el-select v-model="queryParams.platform" placeholder="全部平台" clearable style="width: 130px">
          <el-option label="淘宝" value="taobao" />
          <el-option label="拼多多" value="pdd" />
        </el-select>
      </el-form-item>
      <el-form-item label="下单时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 250px"
        />
      </el-form-item>
      <el-form-item label="关键字" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="订单号 / 商品ID / 标题"
          clearable
          style="width: 210px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Refresh"
          :loading="syncLoading"
          @click="handleSync"
          v-hasPermi="['main:tbk:order:sync']"
        >同步最近 7 天</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="refreshAll"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList">
      <el-table-column label="平台" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.platform === 'pdd' ? 'danger' : 'warning'" size="small">
            {{ platformName(scope.row.platform) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="订单号" prop="orderId" width="190" show-overflow-tooltip />
      <el-table-column label="商品" prop="itemTitle" min-width="230" show-overflow-tooltip />
      <el-table-column label="推广位" prop="pid" width="140" show-overflow-tooltip />
      <el-table-column label="用户" width="140" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.userName">{{ scope.row.userName }}（{{ scope.row.userId }}）</span>
          <span v-else-if="scope.row.userId">ID {{ scope.row.userId }}</span>
          <span v-else-if="scope.row.externalUser" class="anon-user">{{ scope.row.externalUser }}</span>
          <span v-else class="anon-user">未归因</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="statusDesc" width="100" align="center" />
      <el-table-column label="实付" prop="payAmount" width="100" align="right">
        <template #default="scope">¥{{ money(scope.row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="预估佣金" prop="estimatedCommission" width="110" align="right">
        <template #default="scope">¥{{ money(scope.row.estimatedCommission) }}</template>
      </el-table-column>
      <el-table-column label="结算佣金" prop="settledCommission" width="110" align="right">
        <template #default="scope">
          {{ scope.row.settledCommission == null ? '—' : '¥' + money(scope.row.settledCommission) }}
        </template>
      </el-table-column>
      <el-table-column label="应返现" prop="cashbackAmount" width="100" align="right">
        <template #default="scope">¥{{ money(scope.row.cashbackAmount) }}</template>
      </el-table-column>
      <el-table-column label="归因" width="90" align="center">
        <template #default="scope">
          <el-tag :type="matchTypeTag(scope.row.matchType)" size="small">
            {{ matchTypeLabel(scope.row.matchType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="返现状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="cashbackStatusTag(scope.row.cashbackStatus)" size="small">
            {{ cashbackStatusLabel(scope.row.cashbackStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款时间" prop="payTime" width="165" align="center">
        <template #default="scope">{{ parseTime(scope.row.payTime) || '—' }}</template>
      </el-table-column>
      <el-table-column label="结算时间" prop="settleTime" width="165" align="center">
        <template #default="scope">{{ parseTime(scope.row.settleTime) || '—' }}</template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="TbkOrder">
import { listTbkOrder, getTbkOrderStats, syncTbkOrder } from '@/api/tbk/order'

const { proxy } = getCurrentInstance()

const orderList = ref([])
const loading = ref(false)
const syncLoading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const stats = ref({})
const dateRange = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    platform: null,
    keyword: null,
    beginTime: null,
    endTime: null
  }
})

const { queryParams } = toRefs(data)

const statCards = [
  { key: 'todayCount', label: '今日订单' },
  { key: 'todayCommission', label: '今日预估佣金', money: true },
  { key: 'monthCount', label: '本月订单' },
  { key: 'monthCommission', label: '本月预估佣金', money: true },
  { key: 'monthCashback', label: '本月应返现', money: true }
]

function matchTypeLabel(value) {
  const map = { deterministic: '精确', approx: '近似', ambiguous: '多用户', anon: '匿名', none: '未归因' }
  return map[value] || '未归因'
}

function matchTypeTag(value) {
  if (value === 'deterministic') return 'success'
  if (value === 'approx') return 'primary'
  if (value === 'ambiguous') return 'warning'
  return 'info'
}

function cashbackStatusLabel(value) {
  const map = {
    estimated: '预估',
    settled: '已结算',
    invalid: '已退款',
    ambiguous: '待确认',
    unattributed: '未归因',
    unpaid: '未支付'
  }
  return map[value] || '—'
}

function cashbackStatusTag(value) {
  if (value === 'settled') return 'success'
  if (value === 'estimated') return 'primary'
  if (value === 'invalid') return 'danger'
  if (value === 'ambiguous') return 'warning'
  return 'info'
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function platformName(platform) {
  if (platform === 'pdd') return '拼多多'
  if (platform === 'taobao') return '淘宝'
  return platform || '—'
}

function buildQuery() {
  const query = { ...queryParams.value }
  if (dateRange.value && dateRange.value.length === 2) {
    query.beginTime = dateRange.value[0]
    query.endTime = dateRange.value[1]
  } else {
    query.beginTime = null
    query.endTime = null
  }
  return query
}

function getList() {
  loading.value = true
  listTbkOrder(buildQuery())
    .then(res => {
      orderList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function getStats() {
  getTbkOrderStats(buildQuery())
    .then(res => {
      stats.value = res.data || {}
    })
    .catch(() => {})
}

function refreshAll() {
  getList()
  getStats()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  refreshAll()
}

function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSync() {
  proxy.$modal
    .confirm('同步最近 7 天的订单？')
    .then(() => {
      syncLoading.value = true
      return syncTbkOrder({ days: 7 })
    })
    .then(res => {
      const data = res.data || {}
      proxy.$modal.msgSuccess(`同步完成：淘宝 ${data.taobao?.saved || 0} 单，拼多多 ${data.pdd?.saved || 0} 单`)
      refreshAll()
    })
    .catch(() => {})
    .finally(() => {
      syncLoading.value = false
    })
}

refreshAll()
</script>

<style scoped>
.stat-card {
  border-radius: 10px;
}

.stat-label {
  font-size: 13px;
  color: #8a9099;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2329;
  line-height: 1.2;
}

.stat-value.money {
  color: #ff5000;
}

.anon-user {
  color: #a8aeb8;
}
</style>
