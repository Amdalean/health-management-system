<template>
  <div class="app-container">
    <!-- 搜索与操作栏 -->
    <el-card shadow="never" class="mb16">
      <el-row :gutter="10" align="middle">
        <el-col :span="6">
          <el-select
            v-model="selectedSymbol"
            filterable
            remote
            reserve-keyword
            placeholder="输入代码或名称搜索已下载标的（支持非持仓）"
            :remote-method="searchStock"
            :loading="searchLoading"
            style="width:100%"
            @change="onSymbolChange"
          >
            <el-option
              v-for="item in searchResults"
              :key="item.symbol"
              :label="`${item.symbol} ${item.name}`"
              :value="item.symbol"
            />
          </el-select>
        </el-col>
        <el-col :span="3">
          <el-select v-model="selectedYear" style="width:100%" @change="loadKline">
            <el-option v-for="year in yearOptions" :key="year" :label="year" :value="String(year)" />
          </el-select>
        </el-col>
        <el-col :span="1.5">
          <el-button type="primary" :icon="RefreshRight" @click="loadKline" :loading="loading">刷新</el-button>
        </el-col>
        <el-col :span="12" v-if="stockInfo">
          <span class="stock-title">{{ stockInfo.name }}（{{ stockInfo.symbol }}）</span>
          <span class="stock-price">¥{{ currentPrice }}</span>
          <span class="stock-change" :class="priceChange >= 0 ? 'up' : 'down'">
            {{ priceChange >= 0 ? '+' : '' }}{{ priceChangePct?.toFixed(2) }}%
          </span>
          <span class="stock-period">— {{ selectedYear }}年 · {{ barsCount }}交易日</span>
        </el-col>
      </el-row>
    </el-card>

    <!-- 快速跳转：持仓列表 -->
    <el-card shadow="never" class="mb16" v-if="holdings.length > 0">
      <template #header>
        <span style="font-size:13px;color:#909399">持仓快捷跳转</span>
      </template>
      <el-tag
        v-for="h in holdings"
        :key="h.symbol"
        :type="h.symbol === selectedSymbol ? 'primary' : 'info'"
        style="margin: 0 8px 8px 0; cursor: pointer;"
        @click="jumpToSymbol(h.symbol)"
      >
        {{ h.name }} ({{ h.symbol }})
      </el-tag>
    </el-card>

    <!-- K 线图 -->
    <el-card shadow="never" class="mb16">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span v-if="selectedSymbol">{{ stockInfo?.name || selectedSymbol }} K线</span>
          <span v-else>请选择标的</span>
          <div v-if="signalSummary">
            <el-tag type="danger" size="small">金叉 {{ signalSummary.gcCount }} 次</el-tag>
            <el-tag type="success" size="small" style="margin-left:6px">死叉 {{ signalSummary.dcCount }} 次</el-tag>
          </div>
        </div>
      </template>
      <div v-if="!klineData || !klineData.bars || klineData.bars.length === 0" style="height:520px;display:flex;align-items:center;justify-content:center;color:#909399">
        <span>暂无K线数据</span>
      </div>
      <div v-else ref="klineChartRef" style="height:520px;width:100%"></div>
    </el-card>

    <!-- 图例说明 -->
    <el-card shadow="never" class="mb16">
      <el-row :gutter="20">
        <el-col :span="4"><el-tag type="danger" size="small">▲ 金叉</el-tag><span style="font-size:12px;color:#909399;margin-left:4px">买入信号</span></el-col>
        <el-col :span="4"><el-tag type="success" size="small">▼ 死叉</el-tag><span style="font-size:12px;color:#909399;margin-left:4px">卖出信号</span></el-col>
        <el-col :span="4"><span style="display:inline-block;width:16px;height:3px;background:#409EFF;vertical-align:middle;margin-right:4px"></span><span style="font-size:12px;color:#909399">MA10</span></el-col>
        <el-col :span="4"><span style="display:inline-block;width:16px;height:3px;background:#E6A23C;vertical-align:middle;margin-right:4px"></span><span style="font-size:12px;color:#909399">MA30</span></el-col>
        <el-col :span="4"><span style="display:inline-block;width:16px;height:2px;background:#909399;vertical-align:middle;margin-right:4px"></span><span style="font-size:12px;color:#909399">布林带</span></el-col>
        <el-col :span="4"><span style="display:inline-block;width:16px;height:2px;background:#C0C4CC;vertical-align:middle;margin-right:4px;border-top:1px dashed;border-bottom:1px dashed;height:4px"></span><span style="font-size:12px;color:#909399">布林上/下轨</span></el-col>
      </el-row>
    </el-card>

    <!-- 信号列表 -->
    <el-card shadow="never" v-if="signals.length > 0">
      <template #header><span>信号时间线</span></template>
      <el-table :data="sortedSignals" stripe size="small" :default-sort="{ prop: 'date', order: 'descending' }">
        <el-table-column prop="date" label="日期" width="120" sortable />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.type === 'golden_cross'" type="danger" size="small">金叉买入</el-tag>
            <el-tag v-else-if="row.type === 'death_cross'" type="success" size="small">死叉卖出</el-tag>
            <el-tag v-else size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="100" align="right">
          <template #default="{ row }">¥{{ row.price?.toFixed(3) }}</template>
        </el-table-column>
        <el-table-column prop="label" label="说明" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="StockKline">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RefreshRight } from '@element-plus/icons-vue'
import { getHoldings, getKlineData, searchSymbols } from '@/api/main/stock'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const searchLoading = ref(false)
const selectedSymbol = ref('')
const selectedYear = ref(String(new Date().getFullYear()))
const holdings = ref([])
const searchResults = ref([])

// 年份选项：从2024到当前年份
const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let y = currentYear; y >= 2024; y--) {
    years.push(y)
  }
  return years
})

const klineData = ref(null)
const klineChartRef = ref(null)
let klineChart = null

// 计算属性
const stockInfo = computed(() => {
  if (!klineData.value) return null
  return { symbol: klineData.value.symbol, name: klineData.value.name }
})

const barsCount = computed(() => klineData.value?.bars?.length || 0)
const currentPrice = computed(() => {
  const bars = klineData.value?.bars
  return bars?.length > 0 ? bars[bars.length - 1].close.toFixed(2) : '—'
})
const priceChange = computed(() => {
  const bars = klineData.value?.bars
  if (!bars || bars.length < 2) return 0
  const first = bars[0].close
  const last = bars[bars.length - 1].close
  return +(last - first).toFixed(2)
})
const priceChangePct = computed(() => {
  const bars = klineData.value?.bars
  if (!bars || bars.length < 2) return 0
  const first = bars[0].close
  return +((bars[bars.length - 1].close - first) / first * 100).toFixed(2)
})

const signals = computed(() => klineData.value?.signals || [])
const sortedSignals = computed(() => [...signals.value].sort((a, b) => (a.date > b.date ? -1 : 1)))
const signalSummary = computed(() => {
  const gc = signals.value.filter(s => s.type === 'golden_cross').length
  const dc = signals.value.filter(s => s.type === 'death_cross').length
  return gc + dc > 0 ? { gcCount: gc, dcCount: dc } : null
})

// 布林带计算（前端，从收盘价）
function computeBollingerBands(closes, period = 20, multiplier = 2) {
  const bands = []
  for (let i = 0; i < closes.length; i++) {
    if (i < period - 1) {
      bands.push(null)
      continue
    }
    const slice = closes.slice(i - period + 1, i + 1)
    const sum = slice.reduce((a, b) => a + b, 0)
    const mean = sum / period
    const squaredDiffs = slice.map(v => (v - mean) ** 2)
    const std = Math.sqrt(squaredDiffs.reduce((a, b) => a + b, 0) / period)
    bands.push({
      middle: +mean.toFixed(3),
      upper: +(mean + multiplier * std).toFixed(3),
      lower: +(mean - multiplier * std).toFixed(3)
    })
  }
  return bands
}

// 搜索股票（已下载标的，支持非持仓，防下拉滥用）add by CYQ 2026-08-31
function searchStock(query) {
  if (!query || query.trim().length < 1) {
    // 空输入仅展示持仓（避免一次下拉 699 全部已下载）
    searchResults.value = holdings.value.slice(0, 20)
    return
  }
  searchLoading.value = true
  const q = query.trim()
  searchSymbols(q, 20).then(res => {
    let list = res.data || res.rows || res || []
    if (!Array.isArray(list)) list = []
    if (list.length > 0) {
      searchResults.value = list.slice(0, 20)
    } else {
      // 后端无结果时回退本地持仓匹配
      const uq = q.toUpperCase()
      const matches = holdings.value.filter(
        h => h.symbol.includes(uq) || (h.name && h.name.toUpperCase().includes(uq))
      )
      searchResults.value = matches.length > 0 ? matches.slice(0, 20) : []
    }
  }).catch(() => {
    const uq = q.toUpperCase()
    const matches = holdings.value.filter(
      h => h.symbol.includes(uq) || (h.name && h.name.toUpperCase().includes(uq))
    )
    searchResults.value = matches.slice(0, 20)
  }).finally(() => {
    searchLoading.value = false
  })
}

function onSymbolChange(symbol) {
  if (symbol) {
    // URL 更新但不刷新（方便分享/收藏）
    router.replace({ query: { ...route.query, symbol } })
    loadKline()
  }
}

function jumpToSymbol(symbol) {
  selectedSymbol.value = symbol
  router.replace({ query: { ...route.query, symbol } })
  loadKline()
}

async function loadKline() {
  if (!selectedSymbol.value) return
  loading.value = true
  try {
    const res = await getKlineData(selectedSymbol.value, selectedYear.value)
    klineData.value = res.data || null
    
    // 无数据时清除图表
    if (!klineData.value || !klineData.value.bars || klineData.value.bars.length === 0) {
      if (klineChart) {
        klineChart.dispose()
        klineChart = null
      }
      return
    }
    
    nextTick(() => {
      renderKlineChart()
    })
  } catch (e) {
    console.error('获取K线数据失败', e)
    klineData.value = null
    if (klineChart) {
      klineChart.dispose()
      klineChart = null
    }
  } finally {
    loading.value = false
  }
}

function renderKlineChart() {
  if (!klineChartRef.value || !klineData.value?.bars || klineData.value.bars.length < 10) return
  if (klineChart) klineChart.dispose()
  klineChart = echarts.init(klineChartRef.value)

  const bars = klineData.value.bars
  const dates = bars.map(b => b.date)
  const kData = bars.map(b => [b.open, b.close, b.low, b.high])
  const ma10Values = bars.map(b => b.ma10)
  const ma30Values = bars.map(b => b.ma30)
  const volumeData = bars.map(b => b.volume || 0)

  // 布林带
  const closes = bars.map(b => b.close)
  const bb = computeBollingerBands(closes)
  const bbUpper = bb.map(b => b?.upper ?? null)
  const bbMiddle = bb.map(b => b?.middle ?? null)
  const bbLower = bb.map(b => b?.lower ?? null)

  // 金叉/死叉标注
  const gcMarkData = []
  const dcMarkData = []
  const signalList = klineData.value.signals || []
  for (const sig of signalList) {
    const idx = dates.indexOf(sig.date)
    if (idx >= 0) {
      const base = { coord: [idx, sig.price], value: sig.price.toFixed(2) }
      if (sig.type === 'golden_cross') {
        gcMarkData.push({
          ...base,
          symbol: 'pin',
          symbolSize: 28,
          itemStyle: { color: '#f56c6c' },
          label: { formatter: '金叉', color: '#fff', fontSize: 9, backgroundColor: '#f56c6c',
            padding: [2, 6], borderRadius: 3, position: 'top', distance: 8 }
        })
      } else if (sig.type === 'death_cross') {
        dcMarkData.push({
          ...base,
          symbol: 'pin',
          symbolSize: 28,
          itemStyle: { color: '#67c23a' },
          label: { formatter: '死叉', color: '#fff', fontSize: 9, backgroundColor: '#67c23a',
            padding: [2, 6], borderRadius: 3, position: 'bottom', distance: 8 }
        })
      }
    }
  }

  klineChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      confine: true,
      formatter: params => {
        const idx = params[0]?.dataIndex ?? 0
        const b = bars[idx]
        if (!b) return ''
        return `<b>${b.date}</b><br/>
          O: ${b.open.toFixed(2)} H: ${b.high.toFixed(2)}<br/>
          L: ${b.low.toFixed(2)} C: ${b.close.toFixed(2)}<br/>
          MA10: ${b.ma10?.toFixed(2) ?? '—'} MA30: ${b.ma30?.toFixed(2) ?? '—'}<br/>
          布林中: ${bb[idx]?.middle?.toFixed(2) ?? '—'} 上: ${bb[idx]?.upper?.toFixed(2) ?? '—'} 下: ${bb[idx]?.lower?.toFixed(2) ?? '—'}<br/>
          成交量: ${(b.volume || 0).toLocaleString()}`
      }
    },
    legend: {
      data: ['K线', 'MA10', 'MA30', '布林上轨', '布林中轨', '布林下轨'],
      top: 0,
      selected: {
        'K线': true, 'MA10': true, 'MA30': true,
        '布林上轨': true, '布林中轨': true, '布林下轨': true
      }
    },
    grid: [
      { left: '3%', right: '4%', top: '12%', height: '58%' },
      { left: '3%', right: '4%', top: '78%', height: '14%' }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        axisLabel: { rotate: 45, fontSize: 9, interval: Math.max(1, Math.floor(dates.length / 30)) },
        splitLine: { show: false },
        gridIndex: 0
      },
      {
        type: 'category',
        data: dates,
        gridIndex: 1,
        axisLabel: { show: false },
        axisLine: { show: false },
        splitLine: { show: false }
      }
    ],
    yAxis: [
      {
        scale: true,
        gridIndex: 0,
        splitArea: { show: true, areaStyle: { color: ['rgba(250,250,250,0.3)', 'rgba(200,200,200,0.1)'] } }
      },
      {
        scale: true,
        gridIndex: 1,
        splitNumber: 2,
        axisLabel: { show: false },
        splitLine: { show: false }
      }
    ],
    dataZoom: [
      { type: 'inside', xAxisIndex: [0, 1], start: Math.max(0, 100 - 120 * 100 / dates.length), end: 100 },
      { type: 'slider', xAxisIndex: [0, 1], start: Math.max(0, 100 - 120 * 100 / dates.length), end: 100, height: 16, bottom: 0 }
    ],
    series: [
      // K线
      {
        name: 'K线',
        type: 'candlestick',
        data: kData,
        xAxisIndex: 0,
        yAxisIndex: 0,
        itemStyle: {
          color: '#ef5350',
          color0: '#26a69a',
          borderColor: '#ef5350',
          borderColor0: '#26a69a'
        },
        markPoint: {
          data: [...gcMarkData, ...dcMarkData],
          symbol: 'pin',
          symbolSize: 28,
          label: { show: true, fontSize: 9 }
        }
      },
      // MA10
      {
        name: 'MA10',
        type: 'line',
        data: ma10Values,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1.5, color: '#409EFF' },
        xAxisIndex: 0,
        yAxisIndex: 0
      },
      // MA30
      {
        name: 'MA30',
        type: 'line',
        data: ma30Values,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1.5, color: '#E6A23C' },
        xAxisIndex: 0,
        yAxisIndex: 0
      },
      // 布林上轨
      {
        name: '布林上轨',
        type: 'line',
        data: bbUpper,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1, color: '#C0C4CC', type: 'dashed' },
        xAxisIndex: 0,
        yAxisIndex: 0
      },
      // 布林中轨
      {
        name: '布林中轨',
        type: 'line',
        data: bbMiddle,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1, color: '#909399' },
        xAxisIndex: 0,
        yAxisIndex: 0
      },
      // 布林下轨
      {
        name: '布林下轨',
        type: 'line',
        data: bbLower,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1, color: '#C0C4CC', type: 'dashed' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(144,147,153,0.06)' },
            { offset: 1, color: 'rgba(144,147,153,0.01)' }
          ])
        },
        xAxisIndex: 0,
        yAxisIndex: 0
      },
      // 成交量
      {
        name: '成交量',
        type: 'bar',
        data: volumeData,
        xAxisIndex: 1,
        yAxisIndex: 1,
        itemStyle: {
          color: params => {
            const idx = params.dataIndex
            const b = bars[idx]
            return b && b.close >= b.open ? '#ef5350' : '#26a69a'
          }
        }
      }
    ]
  })
}

function handleResize() {
  if (klineChart) klineChart.resize()
}

// 从路由参数初始化
async function initFromRoute() {
  // 先载入持仓列表（用于搜索）
  try {
    const res = await getHoldings()
    holdings.value = res.data || []
    searchResults.value = holdings.value
  } catch (e) {
    console.error('获取持仓失败', e)
  }

  // 从路由参数读取 symbol
  const symbol = route.query.symbol
  if (symbol) {
    selectedSymbol.value = symbol
  } else if (holdings.value.length > 0) {
    selectedSymbol.value = holdings.value[0].symbol
  }

  if (route.query.year) {
    selectedYear.value = route.query.year
  }

  if (selectedSymbol.value) {
    loadKline()
  }
}

onMounted(() => {
  initFromRoute()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (klineChart) { klineChart.dispose(); klineChart = null }
})
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.stock-title { font-size: 18px; font-weight: 600; color: #303133; margin-right: 12px; }
.stock-price { font-size: 20px; font-weight: 700; color: #303133; margin-right: 8px; }
.stock-change { font-size: 14px; font-weight: 500; }
.stock-change.up { color: #f56c6c; }
.stock-change.down { color: #26a69a; }
.stock-period { font-size: 12px; color: #909399; margin-left: 8px; }
</style>
