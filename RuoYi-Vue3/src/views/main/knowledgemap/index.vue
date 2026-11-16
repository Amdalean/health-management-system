<template>
  <div class="app-container">
    <!-- 工具栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-row :gutter="12" align="middle">
        <el-col :span="7">
          <el-input
            v-model="keyword"
            placeholder="请输入节点名称搜索"
            clearable
            prefix-icon="Search"
            @input="handleSearch"
            @clear="handleSearch"
          />
        </el-col>
        <el-col :span="13">
          <el-tag class="stat-tag">节点总数 {{ stats.total }}</el-tag>
          <el-tag class="stat-tag" type="primary">目录 {{ stats.folder }}</el-tag>
          <el-tag class="stat-tag" type="success">文件 {{ stats.file }}</el-tag>
          <el-tag v-if="keyword && stats.matched > -1" class="stat-tag" type="warning">命中 {{ stats.matched }}</el-tag>
        </el-col>
        <el-col :span="4" style="text-align: right">
          <el-button type="primary" plain icon="Refresh" @click="getMapData">刷新</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 知识图谱 -->
    <el-card shadow="never" v-loading="loading">
      <div v-show="stats.total > 0" ref="chartRef" class="map-chart"></div>
      <el-empty v-if="!loading && stats.total === 0" description="暂无资料库数据，请先在资料库管理中上传资料" />
    </el-card>

    <!-- 节点详情卡片 -->
    <el-dialog v-model="detailVisible" :title="activeNode ? activeNode.fileName : ''" width="520px" append-to-body>
      <el-descriptions v-if="activeNode" :column="1" border>
        <el-descriptions-item label="节点类型">
          <el-tag :type="isFolder(activeNode) ? 'primary' : 'success'" size="small">
            {{ isFolder(activeNode) ? '目录' : '文件' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属分类">
          <dict-tag v-if="activeNode.categoryId" :options="category" :value="activeNode.categoryId" />
          <span v-else>未分类</span>
        </el-descriptions-item>
        <el-descriptions-item label="上传时间">
          {{ parseTime(activeNode.uploadTime, '{y}-{m}-{d}') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="直接子节点">
          {{ childCountMap[activeNode.id] || 0 }} 个
        </el-descriptions-item>
        <el-descriptions-item label="文件描述">
          {{ activeNode.description || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="activeNode && activeNode.filePath" type="primary" icon="View" @click="previewVisible = true">预览文件</el-button>
          <el-button @click="detailVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 文件预览 -->
    <el-dialog title="文件预览" v-model="previewVisible" width="500px" append-to-body>
      <FilePreview :model-value="activeNode ? activeNode.filePath : ''" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="previewVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="KnowledgeMap">
/**
 * 知识地图 - 资料库数据关系图谱
 * add by CYQ 2026-08-13 说明: 基于资料库(hms_document)层级数据渲染关系图谱，点击节点弹出详情卡片
 */
import * as echarts from 'echarts'
import { listDocument } from '@/api/main/document'
import FilePreview from '@/components/FilePreview/index.vue'

const { proxy } = getCurrentInstance()
const { category } = proxy.useDict('category')

const chartRef = ref(null)
const loading = ref(true)
const keyword = ref('')
const detailVisible = ref(false)
const previewVisible = ref(false)
const activeNode = ref(null)
const rawList = ref([])
const childCountMap = ref({})

let chart = null
let graphLinks = []

const stats = computed(() => {
  const total = rawList.value.length
  const folder = rawList.value.filter(d => (childCountMap.value[d.id] || 0) > 0).length
  const matched = keyword.value.trim()
    ? rawList.value.filter(d => (d.fileName || '').toLowerCase().includes(keyword.value.trim().toLowerCase())).length
    : -1
  return { total, folder, file: total - folder, matched }
})

/** 判断节点是否为目录（存在子节点） */
function isFolder(doc) {
  return (childCountMap.value[doc.id] || 0) > 0
}

/** 查询资料库数据并构建图谱 */
function getMapData() {
  loading.value = true
  listDocument().then(response => {
    rawList.value = response.data || []
    buildGraph()
    nextTick(() => {
      renderChart(true)
      loading.value = false
    })
  }).catch(() => {
    loading.value = false
  })
}

/** 根据 pid 层级关系构建图谱节点与连线 */
function buildGraph() {
  const ids = new Set(rawList.value.map(d => d.id))
  const childCount = {}
  const links = []
  rawList.value.forEach(d => {
    const pid = d.pid
    if (pid && pid !== 0 && ids.has(pid)) {
      links.push({ source: String(pid), target: String(d.id) })
      childCount[pid] = (childCount[pid] || 0) + 1
    }
  })
  childCountMap.value = childCount
  graphLinks = links
}

/** 构建 ECharts 配置（搜索关键字高亮命中节点、弱化其余节点） */
function buildOption() {
  const kw = keyword.value.trim().toLowerCase()
  const nodes = rawList.value.map(d => {
    const folder = (childCountMap.value[d.id] || 0) > 0
    const matched = !kw || (d.fileName || '').toLowerCase().includes(kw)
    return {
      id: String(d.id),
      name: d.fileName || ('节点' + d.id),
      category: folder ? 0 : 1,
      symbolSize: folder ? Math.min(34 + (childCountMap.value[d.id] || 0) * 6, 60) : 22,
      raw: d,
      itemStyle: { opacity: matched ? 1 : 0.12 },
      label: {
        show: matched,
        fontWeight: kw && matched ? 'bold' : 'normal',
        color: kw && matched ? '#e6a23c' : '#303133'
      }
    }
  })
  const links = graphLinks.map(l => ({
    ...l,
    lineStyle: { opacity: kw ? 0.08 : 0.7 }
  }))
  return {
    color: ['#409EFF', '#67C23A'],
    tooltip: {
      formatter: params => {
        if (params.dataType !== 'node') return ''
        const d = params.data.raw || {}
        const type = (childCountMap.value[d.id] || 0) > 0 ? '目录' : '文件'
        let html = `${params.data.name}<br/>类型：${type}`
        if (d.uploadTime) html += `<br/>上传时间：${proxy.parseTime(d.uploadTime, '{y}-{m}-{d}')}`
        return html
      }
    },
    legend: {
      data: ['目录', '文件'],
      top: 8,
      left: 16
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: links,
      categories: [{ name: '目录' }, { name: '文件' }],
      roam: true,
      draggable: true,
      label: { show: true, position: 'right', fontSize: 13 },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: 7,
      lineStyle: { color: '#b0b6bf', width: 1.5, curveness: 0.1 },
      emphasis: {
        focus: 'adjacency',
        label: { fontWeight: 'bold' },
        lineStyle: { width: 3 }
      },
      force: {
        repulsion: 300,
        edgeLength: [90, 180],
        gravity: 0.08
      }
    }]
  }
}

/** 渲染图表，notMerge=true 用于数据刷新，false 用于搜索高亮（保留节点位置） */
function renderChart(notMerge = false) {
  if (!chartRef.value || rawList.value.length === 0) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
    chart.on('click', params => {
      if (params.dataType === 'node' && params.data.raw) {
        openDetail(params.data.raw)
      }
    })
  }
  chart.setOption(buildOption(), notMerge)
}

/** 搜索高亮 */
function handleSearch() {
  renderChart(false)
}

/** 打开节点详情卡片 */
function openDetail(doc) {
  activeNode.value = doc
  detailVisible.value = true
}

/** 窗口尺寸变化时重绘 */
function handleResize() {
  if (chart) chart.resize()
}

onMounted(() => {
  getMapData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.toolbar-card {
  margin-bottom: 12px;
}
.stat-tag {
  margin-right: 8px;
}
.map-chart {
  height: calc(100vh - 260px);
  min-height: 420px;
}
</style>
