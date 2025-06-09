<template>
  <div class="chart-container">
    <div ref="chartRef" class="chart"></div>
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

const chartRef = ref(null)
let chart = null

const initChart = (data) => {
  if (chart) {
    chart.dispose()
  }
  
  chart = echarts.init(chartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '5%',
      right: '5%',
      bottom: '20%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.date),
      name: '日期',
      nameLocation: 'middle',
      nameGap: 50,
      axisLabel: {
        show: true,
        interval: 0,
        rotate: 45,
        margin: 20,
        fontSize: 12,
        color: '#666'
      },
      axisTick: {
        alignWithLabel: true
      }
    },
    yAxis: {
      type: 'value',
      name: '支出',
      nameLocation: 'middle',
      nameGap: 50,
      axisLabel: {
        fontSize: 12,
        color: '#666'
      }
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
}

const handleResize = () => {
  chart?.resize()
}

onMounted(() => {
  initChart(props.data)
  window.addEventListener('resize', handleResize)
  setTimeout(handleResize, 100)
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
    if (newData?.length) {
      initChart(newData)
    }
  },
  { deep: true }
)
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
  padding: 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.chart {
  flex: 1;
  width: 100%;
  min-height: 500px;
  min-width: 800px;
}
</style>