<template>
  <div class="app-container factor-page">
    <!-- 状态条 -->
    <el-card shadow="never" class="mb12">
      <div class="status-bar">
        <div class="status-left">
          <el-tag :type="statusTag" effect="dark" size="large">{{ statusText }}</el-tag>
          <span class="sep">|</span>
          <span>数据截止：<b>{{ run.dataMaxDate || '-' }}</b></span>
          <span class="sep">|</span>
          <span>行情最新：<b>{{ status.latestQuoteDate || '-' }}</b></span>
          <span class="sep">|</span>
          <span>上次评估：{{ formatTime(run.startTime) }}
            <span v-if="run.endTime">（{{ duration }}）</span>
          </span>
          <span class="sep">|</span>
          <span>代码版本：<code>{{ run.gitHead || '-' }}</code></span>
        </div>
        <div class="status-right">
          <el-button :icon="RefreshRight" :loading="loading" @click="loadAll">刷新</el-button>
          <el-button type="primary" plain :icon="VideoPlay" @click="goRun">去执行</el-button>
        </div>
      </div>
      <el-progress v-if="isRunning" :percentage="Number(run.progress || 0)" :stroke-width="14" class="mt8" />
      <el-alert v-if="stale" type="warning" :closable="false" show-icon class="mt8"
                :title="`因子结果已过期：评估数据截止 ${run.dataMaxDate}，行情已到 ${status.latestQuoteDate}`"
                description="请管理员到「系统监控 → 定时任务」执行「A股因子评估」一次；过期结果不参与排序。" />
      <el-alert v-else-if="run.errorMsg" type="error" :closable="false" show-icon class="mt8"
                :title="'上次执行失败：' + run.errorMsg" />
    </el-card>

    <!-- 空态 -->
    <el-empty v-if="!status.latestRunId && !loading" description="尚未生成因子报告">
      <template #description>
        <p>尚无因子运行记录。请管理员到「系统监控 → 定时任务」执行「A股因子评估」一次。</p>
      </template>
      <el-button type="primary" @click="goRun">去定时任务</el-button>
    </el-empty>

    <el-tabs v-else v-model="activeTab" type="border-card">
      <!-- 因子看板 -->
      <el-tab-pane label="因子看板" name="board">
        <div class="toolbar">
          <el-radio-group v-model="verdictFilter" size="small">
            <el-radio-button label="ALL">全部 {{ evals.length }}</el-radio-button>
            <el-radio-button label="PASS">PASS {{ counts.PASS || 0 }}</el-radio-button>
            <el-radio-button label="FAIL">FAIL {{ counts.FAIL || 0 }}</el-radio-button>
            <el-radio-button label="INSUFFICIENT">INSUFFICIENT {{ counts.INSUFFICIENT || 0 }}</el-radio-button>
          </el-radio-group>
          <el-input v-model="keyword" placeholder="搜索因子 / 族" clearable size="small" class="search" />
          <span class="hint">点击行查看原因与分段 IC</span>
        </div>

        <el-table :data="filteredEvals" v-loading="loading" stripe size="small"
                  :row-class-name="rowClass" @row-click="openDetail">
          <el-table-column prop="factor_name" label="因子" width="120" fixed>
            <template #default="{ row }"><code>{{ row.factor_name }}</code></template>
          </el-table-column>
          <el-table-column prop="universe" label="池" width="100" show-overflow-tooltip />
          <el-table-column label="方向" width="70" align="center">
            <template #default="{ row }">{{ direction(row.factor_name) }}</template>
          </el-table-column>
          <el-table-column label="覆盖率" width="90" align="right">
            <template #default="{ row }">{{ pct(row.coverage) }}</template>
          </el-table-column>
          <el-table-column prop="n_days" label="有效天数" width="90" align="right" />
          <el-table-column label="IC均值" width="90" align="right">
            <template #default="{ row }">{{ num(row.ic_mean, 4) }}</template>
          </el-table-column>
          <el-table-column label="ICIR" width="80" align="right">
            <template #default="{ row }">{{ num(row.icir, 2) }}</template>
          </el-table-column>
          <el-table-column label="胜率" width="80" align="right">
            <template #default="{ row }">{{ pct(row.ic_win_rate) }}</template>
          </el-table-column>
          <el-table-column label="t" width="70" align="right">
            <template #default="{ row }">{{ num(row.ic_t_stat, 2) }}</template>
          </el-table-column>
          <el-table-column label="FDR p" width="80" align="right">
            <template #default="{ row }">{{ num(row.fdr_p, 4) }}</template>
          </el-table-column>
          <el-table-column label="多空年化" width="100" align="right">
            <template #default="{ row }">
              <span :class="signClass(row.ls_annual)">{{ pct(row.ls_annual) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单调性" width="85" align="right">
            <template #default="{ row }">{{ num(row.monotonicity, 2) }}</template>
          </el-table-column>
          <el-table-column label="换手" width="80" align="right">
            <template #default="{ row }">{{ pct(row.turnover) }}</template>
          </el-table-column>
          <el-table-column label="结论" width="110" align="center" fixed="right">
            <template #default="{ row }">
              <el-tag :type="verdictTag(row.verdict)" size="small">{{ row.verdict }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 运行记录 -->
      <el-tab-pane label="运行记录" name="runs">
        <el-table :data="runs" v-loading="loading" stripe size="small">
          <el-table-column prop="run_id" label="run_id" width="190" />
          <el-table-column prop="trigger_type" label="触发" width="80" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="runTag(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="stage" label="阶段" width="110" />
          <el-table-column prop="progress" label="进度" width="80" align="right" />
          <el-table-column prop="data_max_date" label="数据截止" width="110" />
          <el-table-column label="开始" width="170">
            <template #default="{ row }">{{ formatTime(row.start_time) }}</template>
          </el-table-column>
          <el-table-column label="结束" width="170">
            <template #default="{ row }">{{ formatTime(row.end_time) }}</template>
          </el-table-column>
          <el-table-column prop="git_head" label="版本" width="90" />
          <el-table-column prop="error_msg" label="错误" min-width="160" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>

      <!-- 合成与回测 -->
      <el-tab-pane label="合成与回测" name="composite">
        <el-descriptions :column="3" border size="small" class="mb12">
          <el-descriptions-item label="区间">{{ summary.start }} ~ {{ summary.end }}</el-descriptions-item>
          <el-descriptions-item label="持有期">{{ summary.horizon }} 日（skip {{ summary.skip }}）</el-descriptions-item>
          <el-descriptions-item label="池">{{ summary.universe || '-' }}</el-descriptions-item>
          <el-descriptions-item label="数据截止">{{ summary.data_max_date || '-' }}</el-descriptions-item>
          <el-descriptions-item label="配置指纹">{{ summary.config_hash || '-' }}</el-descriptions-item>
          <el-descriptions-item label="代码版本">{{ summary.git_head || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-alert type="info" :closable="false" show-icon
                  title="合成因子与月频回测尚未生成（P1 待开发）"
                  description="当前可查看单因子检验结论；合成权重、净值曲线与三档成本回测将在 factor_engine 落地后展示。" />
      </el-tab-pane>

      <!-- 口径说明 -->
      <el-tab-pane label="口径说明" name="method">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="PASS / FAIL / INSUFFICIENT">
            有效天数 &lt; 250、覆盖率 &lt; 60% → INSUFFICIENT（弃权，不参与合成）；
            IC 均值 ≤ 0、ICIR &lt; 0.3、FDR p &gt; 0.10、单调性 &lt; 0.6、多空年化 ≤ 0 → FAIL；否则 PASS。
          </el-descriptions-item>
          <el-descriptions-item label="多重检验">16 个因子同时检验，以 BH-FDR 校正后的 p 值为准，单看原始 p 视为不合格。</el-descriptions-item>
          <el-descriptions-item label="复权">优先 raw × adj_factor；adj_factor 不完整时用 pct_chg 链式合成（比率类因子正确）。</el-descriptions-item>
          <el-descriptions-item label="样本外">报告未显式切分训练/验证/样本外时，不能把区间内 IC 当样本外结论。</el-descriptions-item>
          <el-descriptions-item label="持仓纪律">
            因子分只做排序微调（≤±20%），不覆盖 Graham 闸门；闸门 FAIL 仍然拦截买入。
          </el-descriptions-item>
          <el-descriptions-item label="触发方式">
            系统监控 → 定时任务 → 「A股因子评估」→ 执行一次；每天 02:00 自动执行。
            页面不提供同步触发，避免长任务占用 Tomcat 线程与数据库连接。
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
    </el-tabs>

    <!-- 因子详情 -->
    <el-drawer v-model="drawerVisible" :title="detail.factor_name || '因子详情'" size="520px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="结论">
          <el-tag :type="verdictTag(detail.verdict)">{{ detail.verdict }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="IC 均值 / ICIR">{{ num(detail.ic_mean, 4) }} / {{ num(detail.icir, 2) }}</el-descriptions-item>
        <el-descriptions-item label="覆盖率 / 有效天数">{{ pct(detail.coverage) }} / {{ detail.n_days }}</el-descriptions-item>
        <el-descriptions-item label="多空年化 / 单调性">{{ pct(detail.ls_annual) }} / {{ num(detail.monotonicity, 2) }}</el-descriptions-item>
        <el-descriptions-item label="FDR p / t 值">{{ num(detail.fdr_p, 4) }} / {{ num(detail.ic_t_stat, 2) }}</el-descriptions-item>
        <el-descriptions-item label="原因">
          <div v-if="detailReasons.length">
            <div v-for="(r, i) in detailReasons" :key="i">· {{ r }}</div>
          </div>
          <span v-else>—（PASS 无否决项）</span>
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="splitIc.length" class="mt12">
        <h4>分段 IC</h4>
        <el-table :data="splitIc" size="small" border>
          <el-table-column prop="segment" label="区间" width="100" />
          <el-table-column prop="n" label="天数" width="80" align="right" />
          <el-table-column prop="ic" label="IC均值" align="right" />
        </el-table>
      </div>
      <el-alert v-else class="mt12" type="info" :closable="false" show-icon
                title="报告未按训练/验证/样本外切分，不能当作样本外结论。" />
    </el-drawer>
  </div>
</template>

<script setup name="StockFactor">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshRight, VideoPlay } from '@element-plus/icons-vue'
import { getFactorStatus, getFactorEvals, getFactorReport, getFactorRuns } from '@/api/main/stock'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('board')
const verdictFilter = ref('ALL')
const keyword = ref('')
const status = ref({})
const evals = ref([])
const runs = ref([])
const summary = ref({})
const drawerVisible = ref(false)
const detail = reactive({})

const run = computed(() => status.value.run || {})
const counts = computed(() => status.value.verdictCounts || {})
const isRunning = computed(() => !!status.value.running)
const stale = computed(() => {
  const d = run.value.data_max_date || run.value.dataMaxDate
  const q = status.value.latestQuoteDate
  return !!(d && q && String(q) > String(d))
})
const statusText = computed(() => {
  if (!status.value.latestRunId) return '未生成'
  if (isRunning.value) return '运行中'
  const s = run.value.status
  if (s === 'success') return '成功'
  if (s === 'failed') return '失败'
  return s || '未知'
})
const statusTag = computed(() => {
  if (!status.value.latestRunId) return 'info'
  if (isRunning.value) return 'warning'
  if (run.value.status === 'success') return 'success'
  if (run.value.status === 'failed') return 'danger'
  return 'info'
})
const duration = computed(() => {
  const t0 = run.value.start_time || run.value.startTime
  const t1 = run.value.end_time || run.value.endTime
  if (!t0 || !t1) return ''
  const ms = new Date(t1).getTime() - new Date(t0).getTime()
  if (ms <= 0) return ''
  return ms > 60000 ? `${Math.round(ms / 60000)} 分钟` : `${Math.round(ms / 1000)} 秒`
})
const filteredEvals = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return evals.value.filter(e => {
    if (verdictFilter.value !== 'ALL' && e.verdict !== verdictFilter.value) return false
    if (!k) return true
    return String(e.factor_name || '').toLowerCase().includes(k)
  })
})
const detailReasons = computed(() => {
  try {
    const r = typeof detail.reasons_json === 'string' ? JSON.parse(detail.reasons_json) : detail.reasons_json
    return Array.isArray(r) ? r : []
  } catch (e) { return [] }
})
const splitIc = computed(() => {
  const si = summary.value?.factors?.[detail.factor_name]?.split_ic
  if (!si) return []
  return Object.entries(si).map(([k, v]) => ({
    segment: k, n: v?.n_days ?? 0, ic: num(v?.ic_mean, 4)
  }))
})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const s = await getFactorStatus()
    status.value = s.data || {}
    if (status.value.latestRunId) {
      const [e, r, rep] = await Promise.all([
        getFactorEvals(status.value.latestRunId),
        getFactorRuns(20),
        getFactorReport(status.value.latestRunId, 'summary')
      ])
      evals.value = e.data || []
      runs.value = r.data || []
      summary.value = rep.data || {}
    } else {
      evals.value = []
      runs.value = []
      summary.value = {}
    }
  } catch (e) {
    ElMessage.error('加载因子数据失败：' + (e?.message || e))
  } finally {
    loading.value = false
  }
}

function goRun() {
  ElMessage.info('请到「系统监控 → 定时任务」找到「A股因子评估」，点「执行一次」')
  router.push('/monitor/job')
}

function openDetail(row) {
  Object.assign(detail, row)
  drawerVisible.value = true
}

function verdictTag(v) {
  return v === 'PASS' ? 'success' : v === 'FAIL' ? 'danger' : 'info'
}
function runTag(v) {
  return v === 'success' ? 'success' : v === 'failed' ? 'danger'
    : v === 'running' ? 'warning' : 'info'
}
function rowClass({ row }) {
  return row.verdict === 'INSUFFICIENT' ? 'row-muted' : ''
}
function direction(name) {
  const neg = ['vol_20', 'vol_60', 'turn_20', 'corr_pv_20', 'maxret_20']
  return neg.includes(name) ? '-1' : '+1'
}
function num(v, nd = 2) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (!Number.isFinite(n)) return '-'
  return n.toFixed(nd)
}
function pct(v) {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (!Number.isFinite(n)) return '-'
  return (n * 100).toFixed(1) + '%'
}
function signClass(v) {
  const n = Number(v)
  return n > 0 ? 'up' : n < 0 ? 'down' : ''
}
function formatTime(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.factor-page { font-size: 13px; }
.mb12 { margin-bottom: 12px; }
.mt8 { margin-top: 8px; }
.mt12 { margin-top: 12px; }
.status-bar { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.status-left { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.status-right { display: flex; gap: 8px; }
.sep { color: #dcdfe6; margin: 0 4px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.search { width: 220px; }
.hint { color: #909399; font-size: 12px; }
.up { color: #67c23a; }
.down { color: #f56c6c; }
:deep(.row-muted) { color: #909399; }
:deep(.row-muted code) { color: #909399; }
</style>
