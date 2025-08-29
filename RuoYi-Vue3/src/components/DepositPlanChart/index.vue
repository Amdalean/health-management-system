<template>
  <div class="chart-container">
    <!-- 概览卡片 -->
    <div class="overview-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="prediction-card">
            <div class="card-content">
              <div class="card-icon">📊</div>
              <div class="card-title">预测年底存款</div>
              <div class="card-value">¥{{ formatMoney(predictionData.prediction?.yearEndDeposit) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="prediction-card">
            <div class="card-content">
              <div class="card-icon">🎯</div>
              <div class="card-title">目标存款</div>
              <div class="card-value">¥{{ formatMoney(predictionData.prediction?.targetYearEndDeposit) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="prediction-card">
            <div class="card-content">
              <div class="card-icon">📈</div>
              <div class="card-title">达成率</div>
              <div class="card-value" :class="getAchievementClass()">
                {{ predictionData.prediction?.achievementRate || 0 }}%
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="prediction-card">
            <div class="card-content">
              <div class="card-icon">⚖️</div>
              <div class="card-title">差距</div>
              <div class="card-value" :class="getGapClass()">
                ¥{{ formatMoney(predictionData.prediction?.targetGap) }}
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 图表区域 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :span="16">
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <span class="header-title">存款计划执行情况与预测</span>
                </div>
                <div class="header-right">
                  <el-switch
                    v-model="showPrediction"
                    active-text="显示预测"
                    inactive-text="隐藏预测"
                    @change="updateChart"
                    class="prediction-switch"
                  />
                </div>
              </div>
            </template>
            <div ref="chartRef" class="chart"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="gauge-card">
            <template #header>
              <div class="card-header">
                <span class="header-title">🏦 存款类型占比</span>
              </div>
            </template>
            <div class="gauge-content">
              <div class="gauge-chart-container">
                <div ref="pieChartRef" class="gauge-chart"></div>
              </div>
              <div class="gauge-info">
                <div class="info-item">
                  <span class="info-label">活期存款：</span>
                  <span class="info-value">¥{{ formatMoney(depositTypeData.current || 0) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">定期存款：</span>
                  <span class="info-value">¥{{ formatMoney(depositTypeData.fixed || 0) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">理财存款：</span>
                  <span class="info-value">¥{{ formatMoney(depositTypeData.wealth || 0) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">总计：</span>
                  <span class="info-value">¥{{ formatMoney(depositTypeData.total || 0) }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分析区域 -->
      <el-row :gutter="20" class="analysis-row">
        <el-col :span="12">
          <el-card class="analysis-card">
            <template #header>
              <div class="card-header">
                <span class="header-title">📈 趋势分析</span>
              </div>
            </template>
            <div class="trend-info">
              <div class="trend-item">
                <div class="trend-icon">📊</div>
                <div class="trend-content">
                  <div class="trend-label">平均月增长</div>
                  <div class="trend-value">¥{{ formatMoney(predictionData.trend?.avgMonthlyGrowth) }}</div>
                </div>
              </div>
              <div class="trend-item">
                <div class="trend-icon">💰</div>
                <div class="trend-content">
                  <div class="trend-label">当前存款</div>
                  <div class="trend-value">¥{{ formatMoney(predictionData.trend?.currentDeposit) }}</div>
                </div>
              </div>
              <div class="trend-item">
                <div class="trend-icon">⏰</div>
                <div class="trend-content">
                  <div class="trend-label">剩余月数</div>
                  <div class="trend-value">{{ predictionData.trend?.remainingMonths || 0 }}个月</div>
                </div>
              </div>
              <div class="trend-item">
                <div class="trend-icon">📋</div>
                <div class="trend-content">
                  <div class="trend-label">数据点数</div>
                  <div class="trend-value">{{ predictionData.trend?.dataPoints || 0 }}个</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="analysis-card">
            <template #header>
              <div class="card-header">
                <span class="header-title">💡 改进建议</span>
              </div>
            </template>
            <div class="recommendations">
              <div 
                v-for="(recommendation, index) in predictionData.recommendations" 
                :key="index"
                class="recommendation-item"
              >
                <div class="recommendation-icon">💡</div>
                <div class="recommendation-text">{{ recommendation }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    required: true
  },
  predictionData: {
    type: Object,
    default: () => ({})
  },
  // 添加HsmDetail数据属性
  hsmDetailData: {
    type: Array,
    default: () => ([])
  }
})

// 添加计算存款类型数据的方法
const calculateDepositTypeData = () => {
  const typeData = {
    current: 0,
    fixed: 0,
    wealth: 0,
    total: 0
  };

  if (props.hsmDetailData && props.hsmDetailData.length > 0) {
    props.hsmDetailData.forEach(item => {
      // 根据HsmDetail的category分类统计
      switch (item.category) {
        case '活期存款':
          typeData.current += item.amount || 0;
          break;
        case '定期存款':
          typeData.fixed += item.amount || 0;
          break;
        case '理财存款':
          typeData.wealth += item.amount || 0;
          break;
      }
    });
    
    typeData.total = typeData.current + typeData.fixed + typeData.wealth;
  }

  return typeData;
};

// 修改存款类型数据为计算属性
const depositTypeData = computed(() => calculateDepositTypeData());

const chartRef = ref(null)
const gaugeRef = ref(null)
const pieChartRef = ref(null) // 新增饼图引用
let chart = null
let pieChart = null // 新增饼图实例
const showPrediction = ref(true)

// 判断是否为移动端
const isMobile = () => window.innerWidth <= 768

// 格式化金额
const formatMoney = (amount) => {
  if (!amount) return '0.00'
  return (Number(amount) / 10000).toFixed(2)
}

// 获取达成率样式
const getAchievementClass = () => {
  const rate = props.predictionData.prediction?.achievementRate || 0
  if (rate >= 100) return 'success-text'
  if (rate >= 80) return 'warning-text'
  return 'danger-text'
}

// 获取差距样式
const getGapClass = () => {
  const gap = props.predictionData.prediction?.targetGap || 0
  if (gap >= 0) return 'success-text'
  return 'danger-text'
}

// 获取状态样式
const getStatusClass = () => {
  const rate = props.predictionData.prediction?.achievementRate || 0
  if (rate >= 100) return 'success-text'
  if (rate >= 80) return 'warning-text'
  return 'danger-text'
}

// 获取状态文本
const getStatusText = () => {
  const rate = props.predictionData.prediction?.achievementRate || 0
  if (rate >= 100) return '已完成'
  if (rate >= 80) return '进行中'
  return '未完成'
}

// 获取图表配置
const getChartOption = (data, showPredictionData = true) => {
  const mobile = isMobile()
  
  const series = [
    {
      name: '实际存款',
      type: 'line',
      data: data.map(item => item.actualDeposit),
      smooth: true,
      lineStyle: {
        color: '#1890ff',
        width: 3
      },
      itemStyle: {
        color: '#1890ff'
      },
      symbol: 'circle',
      symbolSize: 6
    },
    {
      name: '计划存款',
      type: 'line',
      data: data.map(item => item.plannedDeposit),
      smooth: true,
      lineStyle: {
        color: '#ff4d4f',
        width: 3,
        type: 'dashed'
      },
      itemStyle: {
        color: '#ff4d4f'
      },
      symbol: 'diamond',
      symbolSize: 6
    }
  ]

  // 如果显示预测且有预测数据，添加预测线
  if (showPredictionData && props.predictionData.prediction) {
    const currentMonth = new Date().getMonth() + 1
    const predictedData = []
    
    // 填充已过去月份的实际数据
    for (let i = 0; i < currentMonth; i++) {
      predictedData.push(data[i]?.actualDeposit || 0)
    }
    
    // 填充预测数据
    const currentDeposit = props.predictionData.trend?.currentDeposit || 0
    const avgGrowth = props.predictionData.trend?.avgMonthlyGrowth || 0
    
    for (let i = currentMonth; i < 12; i++) {
      const monthsAhead = i - currentMonth + 1
      const predictedValue = currentDeposit + (avgGrowth * monthsAhead)
      predictedData.push(predictedValue)
    }
    
    series.push({
      name: '预测存款',
      type: 'line',
      data: predictedData,
      smooth: true,
      lineStyle: {
        color: '#52c41a',
        width: 3,
        type: 'dotted'
      },
      itemStyle: {
        color: '#52c41a'
      },
      symbol: 'triangle',
      symbolSize: 6
    })
  }
  
  return {
    title: {
      text: '存款计划执行情况与预测',
      left: 'center',
      textStyle: {
        fontSize: mobile ? 14 : 16,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'axis',
      confine: true,
      formatter: (params) => {
        let result = `${params[0].name}<br/>`
        params.forEach(param => {
          const value = (param.value / 10000).toFixed(2)
          result += `${param.marker}${param.seriesName}: ${value}万元<br/>`
        })
        return result
      }
    },
    legend: {
      data: series.map(s => s.name),
      top: mobile ? 30 : 40,
      left: 'center'
    },
    grid: {
      left: mobile ? '10%' : '5%',
      right: mobile ? '5%' : '5%',
      bottom: mobile ? '10%' : '10%',
      top: mobile ? '20%' : '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.date),
      name: '月份',
      nameLocation: 'middle',
      nameGap: mobile ? 25 : 35,
      axisLabel: {
        show: true,
        rotate: 45,
        margin: mobile ? 8 : 15,
        fontSize: mobile ? 10 : 12,
        color: '#666',
        formatter: (value) => {
          const [year, month] = value.split('-')
          return mobile ? month + '月' : value
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '存款金额（万元）',
      nameLocation: 'middle',
      nameGap: mobile ? 40 : 50,
      axisLabel: {
        fontSize: mobile ? 10 : 12,
        color: '#666',
        formatter: (value) => {
          return (value / 10000).toFixed(1)
        }
      }
    },
    series: series,
    animation: true,
    animationDuration: 1000,
    animationEasing: 'cubicInOut'
  }
}

// 初始化图表
const initChart = (data, showPredictionData = true) => {
  if (chart) {
    chart.dispose()
  }
  
  const mobile = isMobile()
  chart = echarts.init(chartRef.value, null, {
    renderer: 'canvas',
    useDirtyRect: false,
    width: mobile ? 300 : 'auto',
    height: mobile ? 200 : 'auto'
  })
  
  chart.setOption(getChartOption(data, showPredictionData))
}

// 初始化饼图
const initPieChart = () => {
  if (pieChart) {
    pieChart.dispose();
  }
  
  pieChart = echarts.init(pieChartRef.value);
  
  // 使用计算后的存款类型数据
  const currentData = calculateDepositTypeData();
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      textStyle: {
        fontSize: 12
      }
    },
    series: [
      {
        name: '存款类型',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['65%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '14',
            fontWeight: 'bold',
            formatter: '{b}\n{d}%'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: currentData.current, name: '活期存款', itemStyle: { color: '#1890ff' } },
          { value: currentData.fixed, name: '定期存款', itemStyle: { color: '#52c41a' } },
          { value: currentData.wealth, name: '理财存款', itemStyle: { color: '#faad14' } }
        ]
      }
    ]
  };
  
  pieChart.setOption(option);
}

// 更新图表
const updateChart = () => {
  if (props.data?.length) {
    initChart(props.data, showPrediction.value)
  }
}

// 处理图表大小变化
const handleResize = () => {
  if (chart) {
    chart.resize()
  }
  if (pieChart) { // 修改为处理饼图
    pieChart.resize()
  }
}

// 生命周期钩子
onMounted(() => {
  if (props.data?.length) {
    initChart(props.data, showPrediction.value)
  }
  // 初始化饼图而不是仪表盘
  initPieChart()
  window.addEventListener('resize', handleResize)
  setTimeout(handleResize, 100)
})

onUnmounted(() => {
  if (chart) {
    chart.dispose()
    chart = null
  }
  if (pieChart) { // 修改为处理饼图
    pieChart.dispose()
    pieChart = null
  }
  window.removeEventListener('resize', handleResize)
})

// 监听数据变化
watch(
  () => props.data,
  (newData) => {
    if (newData?.length) {
      initChart(newData, showPrediction.value)
    }
  },
  { deep: true }
)

watch(
  () => props.predictionData,
  (newData) => {
    if (newData?.prediction) {
      setTimeout(() => {
        initGaugeChart()
      }, 100)
    }
  },
  { deep: true }
)

watch(
  () => depositTypeData.value, // 监听存款类型数据变化
  () => {
    setTimeout(() => {
      initPieChart()
    }, 100)
  },
  { deep: true }
)

// 修改监听器以监听HsmDetail数据变化
watch(
  () => props.hsmDetailData,
  () => {
    setTimeout(() => {
      initPieChart();
    }, 100);
  },
  { deep: true }
);

</script>

<style scoped>
.chart-container {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 200px);
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.overview-cards {
  margin-bottom: 15px;
}

.main-content {
  margin-bottom: 0;
}

.chart-row {
  margin-bottom: 15px;
}

.analysis-row {
  margin-bottom: 0;
}

.prediction-card {
  text-align: center;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  overflow: hidden;
  border: none;
}

.prediction-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
}

.prediction-card :deep(.el-card__body) {
  padding: 20px 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
}

.card-content {
  padding: 0;
}

.card-icon {
  font-size: 24px;
  margin-bottom: 10px;
  display: block;
}

.card-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.card-value {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.success-text {
  color: #67c23a;
}

.warning-text {
  color: #e6a23c;
}

.danger-text {
  color: #f56c6c;
}

.chart-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: none;
}

.chart-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-bottom: none;
  color: white;
  border-radius: 16px 16px 0 0;
  padding: 16px 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-weight: bold;
  color: white;
  font-size: 16px;
  margin-bottom: 4px;
}

.header-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 400;
}

.header-right {
  display: flex;
  align-items: center;
}

.prediction-switch :deep(.el-switch__label) {
  color: white;
  font-size: 12px;
}

.gauge-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  border: none;
}

.gauge-card :deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: bold;
  border-radius: 16px 16px 0 0;
  padding: 16px 20px;
}

.gauge-card :deep(.el-card__body) {
  background: rgba(255, 255, 255, 0.95);
  padding: 20px;
  height: 300px;
  display: flex;
  flex-direction: column;
}

.gauge-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 15px;
}

.gauge-chart-container {
  height: 160px;
  width: 100%;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gauge-chart {
  height: 160px;
  width: 100%;
  position: relative;
  z-index: 1;
}

.gauge-info {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.info-item {
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  white-space: nowrap;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
  margin-right: 8px;
}

.info-value {
  font-size: 13px;
  font-weight: bold;
  color: #333;
}

.analysis-card {
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  border: none;
}

.analysis-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.analysis-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-bottom: none;
  color: white;
  border-radius: 16px 16px 0 0;
  padding: 16px 20px;
}

.analysis-card :deep(.el-card__body) {
  padding: 20px;
  height: 300px;
  overflow-y: auto;
}

.chart {
  height: 300px;
  width: 100%;
}

.trend-info {
  padding: 15px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  margin: 0;
  height: 100%;
  overflow-y: auto;
}

.trend-item {
  margin: 12px 0;
  display: flex;
  align-items: center;
  padding: 12px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.trend-item:hover {
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.trend-icon {
  font-size: 24px;
  margin-right: 12px;
  min-width: 32px;
  text-align: center;
}

.trend-content {
  flex: 1;
}

.trend-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
  margin-bottom: 4px;
}

.trend-value {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.recommendations {
  margin: 0;
  height: 100%;
  overflow-y: auto;
}

.recommendation-item {
  margin: 10px 0;
  font-size: 14px;
  color: #666;
  padding: 12px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 10px;
  border-left: 4px solid #67c23a;
  transition: all 0.3s ease;
  display: flex;
  align-items: flex-start;
}

.recommendation-item:hover {
  background: linear-gradient(135deg, #e8f5e8 0%, #d4edda 100%);
  transform: translateX(8px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.recommendation-icon {
  font-size: 18px;
  margin-right: 12px;
  margin-top: 2px;
  flex-shrink: 0;
}

.recommendation-text {
  flex: 1;
  line-height: 1.5;
}

@media screen and (max-width: 768px) {
  .chart-container {
    padding: 10px;
  }
  
  .card-value {
    font-size: 16px;
  }
  
  .card-icon {
    font-size: 18px;
  }
  
  .gauge-chart {
    height: 120px;
  }
  
  .gauge-chart-container {
    height: 120px;
  }
  
  .gauge-content {
    gap: 10px;
  }
  
  .gauge-info {
    padding: 12px;
  }
  
  .info-label {
    font-size: 11px;
    margin-right: 6px;
  }
  
  .info-value {
    font-size: 12px;
  }
  
  .chart {
    height: 250px;
  }
  
  .gauge-card :deep(.el-card__body) {
    height: 250px;
    padding: 15px;
  }
  
  .analysis-card :deep(.el-card__body) {
    height: 250px;
    padding: 15px;
  }
  
  .trend-item {
    padding: 8px;
  }
  
  .trend-icon {
    font-size: 16px;
    margin-right: 6px;
  }
  
  .recommendation-item {
    padding: 8px;
  }
  
  .overview-cards {
    margin-bottom: 10px;
  }
  
  .chart-row {
    margin-bottom: 10px;
  }
}
</style>