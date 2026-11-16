<template>
  <div class="app-container">
    <el-row :gutter="16" class="mb16">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight:600;font-size:16px">
              📊 回测结果
            </span>
          </template>
          <el-row :gutter="10">
            <el-col :span="1.5">
              <el-button type="primary" :icon="RefreshRight" @click="loadData" :loading="loading">刷新</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button :icon="RefreshRight" @click="refreshStatus">任务状态</el-button>
            </el-col>
            <el-col :span="14">
              <span v-if="runMsg" style="font-size:12px;color:#909399;margin-left:10px">{{ runMsg }}</span>
              <span style="font-size:12px;color:#e6a23c;margin-left:10px">回测在【系统监控-定时任务】找到 回测LIVE 手动执行</span>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===================== 组合级回测：单一共享现金账户 ===================== -->
    <el-row :gutter="16" class="mb16">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight:600;font-size:16px">
              🏦 组合级回测
              <span style="font-size:12px;color:#909399;margin-left:10px">单一现金账户 · 多标的并发持仓 · 逐笔成交 · 与实盘同源决策链</span>
            </span>
            <el-select
              v-if="pfRuns.length > 1"
              v-model="pfRunId" size="small" style="width:260px;float:right"
              @change="switchRun">
              <el-option
                v-for="r in pfRuns" :key="r.runId" :value="r.runId"
                :label="`${String(r.runId)} · ${r.symbolsCount}只 · ${r.totalReturnPct >= 0 ? '+' : ''}${Number(r.totalReturnPct).toFixed(2)}%`" />
            </el-select>
          </template>
          <el-alert
            v-if="!pf.exists" type="info" :closable="false" show-icon
            :title="pf.message || '暂无组合回测结果，请在【系统监控-定时任务】手动执行 回测LIVE'" />
          <div v-else>
            <el-row :gutter="10">
              <el-col :span="4">
                <el-card shadow="hover" class="stat-card">
                  <div class="stat-label">初始资金</div>
                  <div class="stat-value" style="color:#909399">¥{{ money(pf.initialCapital) }}</div>
                </el-card>
              </el-col>
              <el-col :span="4">
                <el-card shadow="hover" class="stat-card">
                  <div class="stat-label">期末总资产</div>
                  <div class="stat-value" :class="pf.totalReturnPct >= 0 ? 'profit' : 'loss'">¥{{ money(pf.finalValue) }}</div>
                </el-card>
              </el-col>
              <el-col :span="4">
                <el-card shadow="hover" class="stat-card">
                  <div class="stat-label">区间收益</div>
                  <div class="stat-value" :class="pf.totalReturnPct >= 0 ? 'profit' : 'loss'">
                    {{ pf.totalReturnPct >= 0 ? '+' : '' }}{{ num(pf.totalReturnPct, 2) }}%
                  </div>
                </el-card>
              </el-col>
              <el-col :span="4">
                <el-card shadow="hover" class="stat-card">
                  <div class="stat-label">年化收益</div>
                  <div class="stat-value" :class="pf.annualReturnPct >= 0 ? 'profit' : 'loss'">
                    {{ pf.annualReturnPct >= 0 ? '+' : '' }}{{ num(pf.annualReturnPct, 2) }}%
                  </div>
                </el-card>
              </el-col>
              <el-col :span="4">
                <el-card shadow="hover" class="stat-card">
                  <div class="stat-label">最大回撤</div>
                  <div class="stat-value loss">{{ num(pf.maxDrawdownPct, 2) }}%</div>
                </el-card>
              </el-col>
              <el-col :span="4">
                <el-card shadow="hover" class="stat-card">
                  <div class="stat-label">夏普 / 卡玛</div>
                  <div class="stat-value" style="font-size:22px;color:#409EFF">
                    {{ num(pf.sharpeRatio, 3) }} / {{ num(pf.calmarRatio, 2) }}
                  </div>
                </el-card>
              </el-col>
            </el-row>
            <div class="pf-meta">
              <span>行情池 {{ pf.symbolsCount }} 只<span v-if="pf.params?.candidate_symbols_count != null"> / PIT 候选覆盖 {{ pf.params.candidate_symbols_count }} 只</span></span>
              <span>交易日 {{ pf.tradingDays }} 天</span>
              <span>成交 {{ pf.tradeCount }} 笔（买 {{ pf.buyCount }} / 卖 {{ pf.sellCount }}）</span>
              <span>胜率 {{ num(pf.winRate, 1) }}%</span>
              <span>已实现 ¥{{ money(pf.realizedPnl) }}</span>
              <span>费用 ¥{{ money(pfTotalFee) }}</span>
              <el-divider direction="vertical" />
              <span>期末现金 ¥{{ money(pf.finalCash) }} + 市值 ¥{{ money(pf.finalMarketValue) }}</span>
              <span>峰值并发 {{ pf.maxConcurrentPositions }} 只</span>
              <span>仓位占用 {{ num(pf.avgCashUtilizationPct, 1) }}%</span>
              <span style="color:#e6a23c">资金争抢拒买 {{ pf.cashRejectContended }} 次</span>
              <el-divider v-if="pf.benchmarkReturnPct !== null && pf.benchmarkReturnPct !== undefined" direction="vertical" />
              <template v-if="pf.benchmarkReturnPct !== null && pf.benchmarkReturnPct !== undefined">
                <span>基准 {{ pf.benchmarkName || pf.benchmarkCode }} {{ num(pf.benchmarkReturnPct, 2) }}%</span>
                <span>超额
                  <b :style="{ color: Number(pf.excessReturnPct) >= 0 ? '#67c23a' : '#f56c6c' }">
                    {{ Number(pf.excessReturnPct) >= 0 ? '+' : '' }}{{ num(pf.excessReturnPct, 2) }}%
                  </b>
                </span>
              </template>
              <el-divider direction="vertical" />
              <el-tag :type="evidenceTagType" size="small" effect="dark">证据 {{ pf.evidenceStatus || '—' }}</el-tag>
            </div>
            <el-alert
              v-if="pf.evidenceStatus && pf.evidenceStatus !== 'PASS'"
              :type="pf.evidenceStatus === 'FAIL' ? 'error' : 'info'"
              :closable="false" show-icon style="margin-top:10px"
              :title="`证据判定 ${pf.evidenceStatus}：${evidenceReasons.length ? evidenceReasons.join('；') : '样本不足或无可判定项'}`" />
            <el-alert
              type="warning" :closable="false" show-icon style="margin-top:10px"
              title="新开仓候选按每个历史交易日前一日成交额动态取 TopN；已有持仓始终纳入卖出与风控。基础行情面板受本地行情库覆盖限制，可能缺退市或未同步标的，结果仍有覆盖偏差。" />
            <div ref="navChartRef" style="height:340px;width:100%;margin-top:12px"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="pf.exists" :gutter="16" class="mb16">
      <el-col :span="24">
        <el-card shadow="never" body-style="padding:0 16px 12px">
          <el-collapse v-model="pfPosOpen" style="border-top:none;border-bottom:none">
            <el-collapse-item name="pos" :title="`持仓全过程（${pfHoldings.length} 只：持有中 ${holdingCount} / 已清仓 ${closedCount}）`">
              <el-row :gutter="10" style="margin-bottom:10px">
                <el-col :span="8">
                  <el-input v-model="pfHoldQuery" size="small" clearable placeholder="搜标的（代码/名称）" />
                </el-col>
                <el-col :span="4">
                  <el-select v-model="pfHoldStatus" size="small" clearable placeholder="状态">
                    <el-option label="持有中" value="holding" />
                    <el-option label="已清仓" value="closed" />
                  </el-select>
                </el-col>
                <el-col :span="12">
                  <span style="font-size:12px;color:#909399;line-height:32px">
                    显示 {{ pfHoldingsFiltered.length }} / {{ pfHoldings.length }} 只
                  </span>
                </el-col>
              </el-row>
              <el-table :data="pfHoldingsFiltered" size="small" stripe max-height="520" empty-text="回测期间无任何持仓">
                <el-table-column label="标的" prop="name" min-width="120" fixed sortable>
                  <template #default="{ row }">
                    <span style="font-weight:500">{{ row.name }}</span>
                    <span style="color:#909399;font-size:11px;margin-left:4px">{{ row.symbol }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" prop="status" width="80" align="center" sortable>
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'holding' ? 'success' : 'info'" size="small">
                      {{ row.status === 'holding' ? '持有中' : '已清仓' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="首次建仓" width="100" prop="firstBuy" sortable>
                  <template #default="{ row }">{{ row.firstBuy || '—' }}</template>
                </el-table-column>
                <el-table-column label="最后交易" width="100" prop="lastTrade" sortable>
                  <template #default="{ row }">{{ row.lastTrade || '—' }}</template>
                </el-table-column>
                <el-table-column label="持有天数" prop="holdDays" width="80" align="right" sortable>
                  <template #default="{ row }">{{ row.holdDays }}</template>
                </el-table-column>
                <el-table-column label="交易次数" prop="tradeCount" width="80" align="right" sortable />
                <el-table-column label="买入股数" prop="buyVolume" width="90" align="right" sortable>
                  <template #default="{ row }">{{ money0(row.buyVolume) }}</template>
                </el-table-column>
                <el-table-column label="卖出股数" prop="sellVolume" width="90" align="right" sortable>
                  <template #default="{ row }">{{ money0(row.sellVolume) }}</template>
                </el-table-column>
                <el-table-column label="期末股数" prop="finalShares" width="90" align="right" sortable>
                  <template #default="{ row }">{{ money0(row.finalShares) }}</template>
                </el-table-column>
                <el-table-column label="期末成本" prop="avgCost" width="90" align="right" sortable>
                  <template #default="{ row }">{{ row.avgCost == null ? '—' : num(row.avgCost, 3) }}</template>
                </el-table-column>
                <el-table-column label="期末市值" prop="marketValue" width="100" align="right" sortable>
                  <template #default="{ row }">{{ row.finalShares > 0 ? '¥' + money(row.marketValue) : '—' }}</template>
                </el-table-column>
                <el-table-column label="已实现盈亏" prop="realizedPnl" width="110" align="right" sortable>
                  <template #default="{ row }">
                    <span :style="{ color: row.realizedPnl >= 0 ? '#67c23a' : '#f56c6c' }">
                      {{ row.realizedPnl >= 0 ? '+' : '' }}{{ money(row.realizedPnl) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="期末盈亏%" prop="profitPct" width="95" align="right" sortable>
                  <template #default="{ row }">
                    <span v-if="row.profitPct == null" style="color:#c0c4cc">—</span>
                    <span v-else :style="{ color: row.profitPct >= 0 ? '#67c23a' : '#f56c6c' }">
                      {{ row.profitPct >= 0 ? '+' : '' }}{{ num(row.profitPct, 2) }}%
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
            <el-collapse-item name="params" title="本次回测参数">
              <div v-for="sec in pfParamSections" :key="sec.title" class="param-sec">
                <div class="param-sec-title">{{ sec.title }}</div>
                <el-row :gutter="8">
                  <el-col v-for="it in sec.items" :key="sec.title + it.label" :span="6" class="param-item">
                    <span class="param-label">{{ it.label }}</span>
                    <span class="param-value">{{ it.value }}</span>
                  </el-col>
                </el-row>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="pf.exists" :gutter="16" class="mb16">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span>组合逐笔成交（{{ pfTradesFiltered.length }} / {{ pfTrades.length }} 笔）</span>
            <span style="font-size:12px;color:#909399;margin-left:10px">现金列为成交后余额，净值列为当日组合总资产</span>
          </template>
          <el-row :gutter="10" style="margin-bottom:10px">
            <el-col :span="8">
              <el-input v-model="pfQuery" size="small" clearable placeholder="搜标的（代码/名称）或理由关键字" />
            </el-col>
            <el-col :span="4">
              <el-select v-model="pfAction" size="small" clearable placeholder="方向">
                <el-option label="买入" value="买入" />
                <el-option label="卖出" value="卖出" />
              </el-select>
            </el-col>
            <el-col :span="4">
              <el-select v-model="pfPnl" size="small" clearable placeholder="盈亏">
                <el-option label="盈利" value="win" />
                <el-option label="亏损" value="loss" />
              </el-select>
            </el-col>
          </el-row>
          <el-table :data="pfTradesFiltered" size="small" stripe max-height="480" v-loading="pfTradeLoading"
                    empty-text="暂无成交">
            <el-table-column label="日期" width="100" sortable prop="tradeDate">
              <template #default="{ row }">{{ String(row.tradeDate || '').substring(0, 10) }}</template>
            </el-table-column>
            <el-table-column label="标的" width="130">
              <template #default="{ row }">
                <span>{{ row.name || row.symbol }}</span>
                <span style="color:#909399;font-size:11px;margin-left:4px">{{ row.symbol }}</span>
              </template>
            </el-table-column>
            <el-table-column label="方向" width="62" align="center">
              <template #default="{ row }">
                <el-tag :type="row.action === '买入' ? 'danger' : 'success'" size="small">{{ row.action }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="价格" width="76" align="right" sortable prop="price">
              <template #default="{ row }">{{ num(row.price, 3) }}</template>
            </el-table-column>
            <el-table-column label="股数" width="70" align="right" sortable prop="volume" />
            <el-table-column label="费用" width="66" align="right" sortable prop="fee">
              <template #default="{ row }">{{ num(row.fee, 2) }}</template>
            </el-table-column>
            <el-table-column label="现金" width="92" align="right" sortable prop="cashAfter">
              <template #default="{ row }">¥{{ money(row.cashAfter) }}</template>
            </el-table-column>
            <el-table-column label="已实现" width="88" align="right" sortable prop="realizedPnl">
              <template #default="{ row }">
                <span v-if="row.realizedPnl === null || row.realizedPnl === undefined" style="color:#c0c4cc">—</span>
                <span v-else :style="{ color: row.realizedPnl >= 0 ? '#67c23a' : '#f56c6c' }">
                  {{ row.realizedPnl >= 0 ? '+' : '' }}{{ money(row.realizedPnl) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="理由" min-width="180">
              <template #default="{ row }"><span style="font-size:12px">{{ row.reason }}</span></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup name="BacktestLive">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { getBacktestStatus,
         getBacktestPortfolio, getBacktestPortfolioRuns, getBacktestPortfolioTrades } from '@/api/main/stock'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const runMsg = ref('')
function loadData(){
  loadPortfolio()
}
function refreshStatus(){
  getBacktestStatus().then(r=>{
    const st = r.data || {}
    if(st.lastStart){
      runMsg.value = '上次执行 ' + String(st.lastStart).substring(0,19)
        + ' → ' + (st.lastOk === true ? '成功' : st.lastOk === false ? '失败' : '未知')
        + (st.message ? '：' + st.message : '')
    } else {
      runMsg.value = st.message || ''
    }
  }).catch(()=>{ runMsg.value = '' })
}

/* ---------------- 组合级回测（共享现金账户） ---------------- */
const pf = ref({ exists: false })
const pfRuns = ref([])
const pfTrades = ref([])
const pfRunId = ref(null)
const pfTradeLoading = ref(false)
const pfPosOpen = ref(['pos'])
const pfQuery = ref('')
const pfAction = ref('')
const pfPnl = ref('')
const pfHoldQuery = ref('')
const pfHoldStatus = ref('')
const pfTradesFiltered = computed(() => {
  const q = (pfQuery.value || '').trim().toLowerCase()
  return (pfTrades.value || []).filter(t => {
    if (pfAction.value && t.action !== pfAction.value) return false
    if (pfPnl.value === 'win' && !(Number(t.realizedPnl) > 0)) return false
    if (pfPnl.value === 'loss' && !(Number(t.realizedPnl) < 0)) return false
    if (q && !String(t.symbol || '').toLowerCase().includes(q)
          && !String(t.name || '').toLowerCase().includes(q)
          && !String(t.reason || '').toLowerCase().includes(q)) return false
    return true
  })
})
const navChartRef = ref(null)
let navChart = null

/* ---------------- 持仓全过程：用逐笔成交聚合出回测期间出现过的全部标的 ---------------- */
const pfHoldings = computed(() => {
  const posMap = {}
  ;(pf.value.positions || []).forEach(p => { posMap[p.symbol] = p })
  const dates = (pf.value.navCurve || []).map(p => String(p.d || '').substring(0, 10))
  const dateIdx = {}
  dates.forEach((d, i) => { if (d) dateIdx[d] = i })
  const lastNavDate = dates.length ? dates[dates.length - 1] : null

  const agg = {}
  const ensure = (symbol, name) => {
    if (!agg[symbol]) {
      agg[symbol] = {
        symbol, name: name || symbol,
        firstBuy: null, lastSell: null, lastTrade: null,
        buyVolume: 0, sellVolume: 0, realizedPnl: 0, tradeCount: 0
      }
    }
    return agg[symbol]
  }

  for (const t of pfTrades.value || []) {
    const a = ensure(t.symbol, t.name)
    const day = String(t.tradeDate || '').substring(0, 10)
    a.tradeCount++
    if (day && (!a.lastTrade || day > a.lastTrade)) a.lastTrade = day
    if (t.action === '买入') {
      if (day && (!a.firstBuy || day < a.firstBuy)) a.firstBuy = day
      a.buyVolume += Number(t.volume || 0)
    } else if (t.action === '卖出') {
      if (day && (!a.lastSell || day > a.lastSell)) a.lastSell = day
      a.sellVolume += Number(t.volume || 0)
      a.realizedPnl += Number(t.realizedPnl || 0)
    }
  }
  Object.values(posMap).forEach(p => ensure(p.symbol, p.name))

  const list = Object.values(agg).map(a => {
    const pos = posMap[a.symbol]
    const finalShares = pos ? Number(pos.shares || 0) : 0
    const startDate = a.firstBuy
    const endDate = a.lastSell || lastNavDate
    let holdDays = '—'
    if (startDate && endDate && dateIdx[startDate] !== undefined && dateIdx[endDate] !== undefined) {
      holdDays = dateIdx[endDate] - dateIdx[startDate] + 1
    }
    return {
      ...a,
      finalShares,
      status: finalShares > 0 ? 'holding' : 'closed',
      avgCost: pos ? pos.avg_cost : null,
      marketValue: pos ? pos.market_value : 0,
      profitPct: pos ? pos.profit_pct : null,
      holdDays
    }
  })
  list.sort((x, y) => {
    const held = (y.finalShares > 0 ? 1 : 0) - (x.finalShares > 0 ? 1 : 0)
    if (held) return held
    return String(y.lastTrade || '').localeCompare(String(x.lastTrade || ''))
  })
  return list
})
const holdingCount = computed(() => pfHoldings.value.filter(h => h.status === 'holding').length)
const closedCount = computed(() => pfHoldings.value.filter(h => h.status === 'closed').length)
const pfHoldingsFiltered = computed(() => {
  const q = (pfHoldQuery.value || '').trim().toLowerCase()
  return pfHoldings.value.filter(h => {
    if (pfHoldStatus.value && h.status !== pfHoldStatus.value) return false
    if (q && !String(h.symbol || '').toLowerCase().includes(q)
          && !String(h.name || '').toLowerCase().includes(q)) return false
    return true
  })
})

function num(v, d = 2){ const n = Number(v); return Number.isFinite(n) ? n.toFixed(d) : '—' }
function money0(v){ const n = Number(v); return Number.isFinite(n) ? n.toLocaleString('zh-CN') : '—' }
function money(v){
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', {minimumFractionDigits:2, maximumFractionDigits:2}) : '—'
}
const pfTotalFee = computed(()=> Number(pf.value.buyFee || 0) + Number(pf.value.sellFee || 0))
/* 证据判定（避损优先：绝对回撤/亏损 + 跑输基准）——由 Python 侧写入，params_json 携带判据明细 */
const evidenceReasons = computed(() => {
  const p = pf.value.params || {}
  return Array.isArray(p.evidence_reasons) ? p.evidence_reasons : []
})
const evidenceTagType = computed(() => {
  const s = pf.value.evidenceStatus
  return s === 'PASS' ? 'success' : s === 'FAIL' ? 'danger' : 'info'
})

/* ---------------- 本次回测参数（params_json）展示 ---------------- */
const PF_PARAM_LABELS = {
  target_position_pct: '单只目标权重 %',
  max_single_position_pct: '单票上限 %',
  max_positions: '最大持仓数',
  max_equity_pct: '权益上限 %',
  min_position_amount: '单只金额下限(元)',
  market_top_n: '逐日成交额候选 TopN',
  reentry_cooldown_days: '清仓后再入冷却(交易日)',
  min_avg_amount: '最小20日均成交额(元)',
  buy_position_pct: '单笔买入比例 %',
  commission_rate: '佣金率',
  min_commission: '最低佣金(元)',
  stamp_tax_rate: '印花税(卖出)',
  slippage_pct: '滑点',
  risk_free_rate: '无风险利率',
  fail_max_drawdown_pct: '回撤 FAIL 线 %',
  fail_return_pct: '亏损 FAIL 线 %',
  fail_excess_return_pct: '跑输基准 FAIL 线 %',
  min_days: '最少样本天数',
  primary: '主基准',
  ep_min: 'E/P 下限',
  pb_max: 'P/B 上限',
  lev_max: '资产负债率上限',
  coverage_min: '利息保障倍数下限',
  credit_hard: '偿债闸门硬约束',
  block_buy: '估值闸门拦截买入',
  enabled: '启用',
  require_profit_for_band_sell: '上轨/网格只在水上卖出',
  min_profit_pct: '卖出最低浮盈 %',
  grid_add_floor_pct: '网格补仓浮亏下限 %',
  sell_at_pct: '硬止损卖出线 %',
  reduce_at_pct: '硬止损减仓线 %',
  atr_period: 'ATR周期',
  atr_mult: 'ATR倍数',
  min_pct: 'ATR最小距离 %',
  max_pct: 'ATR最大距离 %',
  emergency_pct: 'ATR应急最大回撤 %',
}
function fmtParam(v){
  if (v === null || v === undefined) return '—'
  if (typeof v === 'boolean') return v ? '是' : '否'
  if (Array.isArray(v)) return v.length ? v.join(' / ') : '—'
  if (typeof v === 'number') return Number.isInteger(v) ? String(v) : String(Number(v.toFixed(6)))
  return String(v)
}
function _rows(obj, extra){
  const out = []
  Object.keys(obj || {}).forEach(k => out.push({ label: PF_PARAM_LABELS[k] || k, value: fmtParam(obj[k]) }))
  ;(extra || []).forEach(x => out.push(x))
  return out
}
const pfParamSections = computed(() => {
  const p = pf.value.params || {}
  const secs = []
  if (p.capital != null || p.period) {
    const items = [
      { label: '初始资金', value: '¥' + money(p.capital) },
      { label: '区间', value: p.period || '—' },
    ]
    if (p.max_positions_effective != null) {
      items.push({ label: '持仓数上限(反算)', value: p.max_positions_effective + ' 只' })
    }
    secs.push({ title: '资金 / 区间', items })
  }
  if (p.portfolio) secs.push({ title: '组合规则', items: _rows(p.portfolio) })
  if (p.cost_model) secs.push({ title: '交易成本', items: _rows(p.cost_model) })
  if (p.risk) {
    const items = []
    Object.keys(p.risk).forEach(g => {
      const grp = p.risk[g] || {}
      Object.keys(grp).forEach(k => items.push({
        label: (g === 'hard_stop_loss' ? '硬止损·' : g === 'atr_stop_loss' ? 'ATR·' : '') + (PF_PARAM_LABELS[k] || k),
        value: g === 'atr_stop_loss' && ['min_pct', 'max_pct', 'emergency_pct'].includes(k)
          ? fmtParam(Number(grp[k]) * 100) : fmtParam(grp[k]),
      }))
    })
    secs.push({ title: '风控', items })
  }
  if (p.valuation) secs.push({ title: '估值闸门', items: _rows(p.valuation) })
  if (p.sell_discipline) secs.push({ title: '卖出纪律', items: _rows(p.sell_discipline) })
  if (p.evaluation) secs.push({ title: '评价函数', items: _rows(p.evaluation) })
  if (p.benchmark) secs.push({ title: '基准', items: [
    { label: '主基准', value: (p.benchmark_name || p.benchmark.primary || '—') },
    { label: '启用', value: fmtParam(p.benchmark.enabled) },
    { label: '候选指数', value: fmtParam(p.benchmark.indices) },
  ]})
  if (p.pool) secs.push({ title: '标的池', items: [
    { label: '持仓', value: (p.pool.holdings || []).length + ' 只' },
    { label: '市场候选', value: (p.pool.market || []).length + ' 只' },
    { label: '无行情剔除', value: (p.pool.excluded_no_data || []).length + ' 只' },
  ]})
  return secs
})

function loadPortfolio(){
  getBacktestPortfolioRuns().then(r=>{ pfRuns.value = r.data || [] })
  getBacktestPortfolio(pfRunId.value).then(r=>{
    pf.value = r.data || { exists:false }
    pfRunId.value = pf.value.runId || null
    nextTick(()=>renderNavChart())
    loadPortfolioTrades(pfRunId.value)
  })
}
function loadPortfolioTrades(runId){
  pfTradeLoading.value = true
  getBacktestPortfolioTrades(runId)
    .then(r=>{ pfTrades.value = r.data || [] })
    .finally(()=> pfTradeLoading.value = false)
}
/** 切批次时详情与成交一起切，避免卡片显示最新、表格显示旧批次 */
function switchRun(id){
  pfRunId.value = id
  getBacktestPortfolio(id).then(r=>{
    pf.value = r.data || { exists:false }
    nextTick(()=>renderNavChart())
  })
  loadPortfolioTrades(id)
}

function renderNavChart(){
  if(!navChartRef.value) return
  if(navChart) navChart.dispose()
  const curve = pf.value.navCurve || []
  if(!curve.length) return
  navChart = echarts.init(navChartRef.value)
  const maxPos = Math.max(1, ...curve.map(p=>p.pos || 0))
  navChart.setOption({
    title:{ text:`初始 ¥${money(pf.value.initialCapital)} → 期末 ¥${money(pf.value.finalValue)}`,
            left:8, top:2, textStyle:{fontSize:12, color:'#606266'} },
    tooltip:{trigger:'axis', confine:true},
    legend:{data:['组合总资产','现金','持仓市值','持仓数'], top:0, right:10, textStyle:{fontSize:10}},
    grid:{left:'3%', right:'4%', top:'16%', bottom:'10%', containLabel:true},
    xAxis:{type:'category', data:curve.map(p=>p.d),
           axisLabel:{fontSize:10, formatter:v=>String(v).substring(5)}},
    yAxis:[
      {type:'value', name:'金额', scale:true},
      {type:'value', name:'持仓数', minInterval:1, max:Math.ceil(maxPos*1.6), splitLine:{show:false}},
    ],
    series:[
      {name:'组合总资产', type:'line', data:curve.map(p=>p.nav), showSymbol:false, smooth:false,
       lineStyle:{width:2, color:'#409EFF'}, areaStyle:{color:'rgba(64,192,255,0.08)'}},
      {name:'现金', type:'line', data:curve.map(p=>p.cash), showSymbol:false,
       lineStyle:{width:1, color:'#909399'}},
      {name:'持仓市值', type:'line', data:curve.map(p=>p.mv), showSymbol:false,
       lineStyle:{width:1, color:'#67c23a'}},
      {name:'持仓数', type:'bar', yAxisIndex:1, data:curve.map(p=>p.pos), barMaxWidth:8,
       itemStyle:{color:'rgba(230,162,60,0.28)'}},
    ]
  })
}

function handleResize(){ if(navChart) navChart.resize() }
onMounted(()=>{ loadData(); refreshStatus(); window.addEventListener('resize', handleResize)})
onUnmounted(()=>{
  window.removeEventListener('resize', handleResize)
  if(navChart){ navChart.dispose(); navChart=null }
})
</script>

<style scoped>
.stat-card { text-align: center; cursor: default; }
.stat-card .stat-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-card .stat-value { font-size: 28px; font-weight: 700; }
.stat-card .stat-value.profit { color: #67c23a; }
.stat-card .stat-value.loss { color: #f56c6c; }
.pf-meta { margin-top: 12px; font-size: 12px; color: #606266; line-height: 22px; }
.pf-meta > span { margin-right: 16px; white-space: nowrap; }
.param-sec { margin-bottom: 10px; }
.param-sec-title { font-weight: 600; font-size: 13px; color: #303133; margin: 4px 0 6px; }
.param-item { font-size: 12px; line-height: 22px; margin-bottom: 2px; }
.param-label { color: #909399; margin-right: 6px; }
.param-value { color: #303133; }
</style>
