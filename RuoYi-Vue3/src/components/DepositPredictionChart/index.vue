<template>
  <div class="prediction-container">
    <!-- 概览卡片 -->
    <div class="overview-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="prediction-card">
            <div class="card-content">
              <div class="card-title">预测年底存款</div>
              <div class="card-value">¥{{ formatMoney(predictionData.prediction?.yearEndDeposit) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="prediction-card">
            <div class="card-content">
              <div class="card-title">目标存款</div>
              <div class="card-value">¥{{ formatMoney(predictionData.prediction?.targetYearEndDeposit) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="prediction-card">
            <div class="card-content">
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
              <div class="card-title">差距</div>
              <div class="card-value" :class="getGapClass()">
                ¥{{ formatMoney(predictionData.prediction?.targetGap) }}
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 仪表盘 -->
    <div class="gauge-section">
      <el-card class="gauge-card">
        <template #header>
          <div class="card-header">
            <span>目标达成度</span>
          </div>
        </template>
        <div class="gauge-container">
          <div ref="gaugeRef" class="gauge-chart"></div>
          <div class="gauge-info">
            <div class="info-item">
              <span class="info-label">当前达成：</span>
              <span class="info-value" :class="getAchievementClass()">
                {{ predictionData.prediction?.achievementRate || 0 }}%
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">目标：</span>
              <span class="info-value">100%</span>
            </div>
            <div class="info-item">
              <span class="info-label">状态：</span>
              <span class="info-value" :class="getStatusClass()">
                {{ getStatusText() }}
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 趋势分析 -->
    <div class="trend-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>趋势分析</span>
              </div>
            </template>
            <div class="trend-info">
              <p><strong>平均月增长：</strong>¥{{ formatMoney(predictionData.trend?.avgMonthlyGrowth) }}</p>
              <p><strong>当前存款：</strong>¥{{ formatMoney(predictionData.trend?.currentDeposit) }}</p>
              <p><strong>剩余月数：</strong>{{ predictionData.trend?.remainingMonths || 0 }}个月</p>
              <p><strong>数据点数：</strong>{{ predictionData.trend?.dataPoints || 0 }}个</p>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>改进建议</span>
              </div>
            </template>
            <div class="recommendations">
              <ul>
                <li v-for="(recommendation, index) in predictionData.recommendations" :key="index">
                  {{ recommendation }}
                </li>
              </ul>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
})

const predictionData = ref({})
const gaugeRef = ref(null)
let gaugeChart = null

// 格式化金额
const formatMoney = (amount) => {
  if (!amount) return '0.00'
  return (Number(amount) / 10000).toFixed(2)
}

// 获取达成率样式
const getAchievementClass = () => {
  const rate = predictionData.value.prediction?.achievementRate || 0
  if (rate >= 100) return 'success-text'
  if (rate >= 80) return 'warning-text'
  return 'danger-text'
}

// 获取差距样式
const getGapClass = () => {
  const gap = predictionData.value.prediction?.targetGap || 0
  if (gap >= 0) return 'success-text'
  return 'danger-text'
}

// 获取状态样式
const getStatusClass = () => {
  const rate = predictionData.value.prediction?.achievementRate || 0
  if (rate >= 100) return 'success-text'
  if (rate >= 80) return 'warning-text'
  return 'danger-text'
}

// 获取状态文本
const getStatusText = () => {
  const rate = predictionData.value.prediction?.achievementRate || 0
  if (rate >= 100) return '已完成'
  if (rate >= 80) return '进行中'
  return '未完成'
}

// 初始化仪表盘
const initGaugeChart = () => {
  if (gaugeChart) {
    gaugeChart.dispose()
  }
  
  gaugeChart = echarts.init(gaugeRef.value)
  
  const achievementRate = predictionData.value.prediction?.achievementRate || 0
  
  const option = {
    series: [{
      type: 'gauge',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 120,
      splitNumber: 12,
      itemStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [{
            offset: 0, color: '#67c23a'
          }, {
            offset: 0.5, color: '#e6a23c'
          }, {
            offset: 1, color: '#f56c6c'
          }]
        },
        shadowColor: 'rgba(0,138,255,0.45)',
        shadowBlur: 10,
        shadowOffsetX: 2,
        shadowOffsetY: 2
      },
      progress: {
        show: true,
        roundCap: true,
        width: 20,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: '#67c23a'
            }, {
              offset: 0.5, color: '#e6a23c'
            }, {
              offset: 1, color: '#f56c6c'
            }]
          }
        }
      },
      pointer: {
        icon: 'path://M2090.36389,615.30999 L2090.36389,615.30999 C2091.48372,615.30999 2092.40383,616.194028 2092.44859,617.312956 L2096.90698,728.755929 C2097.05155,732.369577 2094.2393,735.416212 2090.62566,735.56078 C2090.53845,735.564269 2090.45117,735.566014 2090.36389,735.566014 L2090.36389,735.566014 C2086.74736,735.566014 2083.81557,732.63423 2083.81557,729.017692 C2083.81557,728.930412 2083.81732,728.84314 2083.82081,728.755929 L2088.2792,617.312956 C2088.32396,616.194028 2089.24407,615.30999 2090.36389,615.30999 Z',
        length: '60%',
        width: 8,
        offsetCenter: [0, '5%']
      },
      axisLine: {
        roundCap: true,
        lineStyle: {
          width: 20,
          color: [
            [0.3, '#f56c6c'],
            [0.7, '#e6a23c'],
            [1, '#67c23a']
          ]
        }
      },
      axisTick: {
        splitNumber: 2,
        lineStyle: {
          width: 2,
          color: '#999'
        }
      },
      splitLine: {
        length: 15,
        lineStyle: {
          width: 3,
          color: '#999'
        }
      },
      axisLabel: {
        distance: 35,
        color: '#999',
        fontSize: 12
      },
      title: {
        offsetCenter: [0, '20%'],
        fontSize: 16,
        color: '#666'
      },
      detail: {
        fontSize: 0,
        offsetCenter: [0, '70%'],
        valueAnimation: true,
        formatter: function (value) {
          return ''
        },
        color: 'auto'
      },
      data: [{
        value: Math.min(achievementRate, 120),
        name: '目标达成度'
      }]
    }]
  }
  
  gaugeChart.setOption(option)
}

// 处理窗口大小变化
const handleResize = () => {
  if (gaugeChart) {
    gaugeChart.resize()
  }
}

// 生命周期钩子
onMounted(() => {
  predictionData.value = props.data
  if (predictionData.value.prediction) {
    initGaugeChart()
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (gaugeChart) {
    gaugeChart.dispose()
    gaugeChart = null
  }
  window.removeEventListener('resize', handleResize)
})

// 监听数据变化
watch(
  () => props.data,
  (newData) => {
    predictionData.value = newData
    if (newData?.prediction) {
      setTimeout(() => {
        initGaugeChart()
      }, 100)
    }
  },
  { deep: true }
)
</script>

<style scoped>
.prediction-container {
  padding: 20px;
}

.overview-cards {
  margin-bottom: 30px;
}

.prediction-card {
  text-align: center;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  overflow: hidden;
}

.prediction-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.prediction-card :deep(.el-card__body) {
  padding: 25px 15px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.card-content {
  padding: 0;
}

.card-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 15px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.card-value {
  font-size: 28px;
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

.gauge-section {
  margin-bottom: 20px;
}

.gauge-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.gauge-card :deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: bold;
}

.gauge-card :deep(.el-card__body) {
  background: rgba(255, 255, 255, 0.95);
  padding: 30px;
}

.gauge-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gauge-chart {
  height: 300px;
  width: 60%;
  position: relative;
  z-index: 1;
}

.gauge-info {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  background: rgba(255, 255, 255, 0.9);
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 2;
  min-width: 150px;
}

.info-item {
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.info-value {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-left: 10px;
}

.trend-section {
  margin-bottom: 20px;
}

.trend-section :deep(.el-card) {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.trend-section :deep(.el-card:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.card-header {
  font-weight: bold;
  color: #333;
  font-size: 16px;
  border-bottom: 2px solid #e6e6e6;
  padding-bottom: 10px;
}

.trend-info p {
  margin: 15px 0;
  font-size: 14px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.trend-info p strong {
  color: #333;
  font-weight: 600;
}

.recommendations ul {
  margin: 0;
  padding-left: 20px;
}

.recommendations li {
  margin: 12px 0;
  font-size: 14px;
  color: #666;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #67c23a;
  transition: all 0.3s ease;
}

.recommendations li:hover {
  background: #e8f5e8;
  transform: translateX(5px);
}

@media screen and (max-width: 768px) {
  .prediction-container {
    padding: 10px;
  }
  
  .card-value {
    font-size: 18px;
  }
  
  .gauge-chart {
    height: 200px;
  }
}
</style> 