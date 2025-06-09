<template>
  <div>
    <div ref="chartRef" style="width: 100%; height: 400px;"></div>
    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    required: true
  }
})

const error = ref(null)
const chartRef = ref(null)
let chart = null

const initChart = (data) => {
  try {
    if (chart) {
      chart.dispose()
    }
    
    chart = echarts.init(chartRef.value)
    
    const option = {
      tooltip: {
        trigger: 'axis'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        top: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: data.map(item => item.date),
        name: '日期',
        nameLocation: 'middle',
        nameGap: 35,
        axisLabel: {
          interval: 0,
          rotate: 30
        }
      },
      yAxis: {
        type: 'value',
        name: '支出',
        nameLocation: 'middle',
        nameGap: 35
      },
      series: [
        {
          data: data.map(item => item.expense),
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: {
            color: '#1890ff'
          },
          lineStyle: {
            width: 2
          }
        }
      ],
      animation: false
    }
    
    chart.setOption(option)
  } catch (e) {
    error.value = '图表初始化失败: ' + e.message
    console.error('图表初始化错误:', e)
  }
}

// 监听窗口大小变化
const handleResize = () => {
  chart && chart.resize()
}

onMounted(() => {
  initChart(props.data)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chart) {
    chart.dispose()
    chart = null
  }
  window.removeEventListener('resize', handleResize)
})

watch(
  () => props.data,
  (newData) => {
    if (newData && newData.length > 0) {
      initChart(newData)
    }
  },
  { deep: true }
)
</script>

<style scoped>
.error-message {
  color: red;
  margin-top: 10px;
}
</style>