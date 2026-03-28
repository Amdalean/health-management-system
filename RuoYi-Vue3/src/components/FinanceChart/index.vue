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

// 判断是否为移动端
const isMobile = () => window.innerWidth <= 768

// 获取图表配置
const getChartOption = (data) => {
  const mobile = isMobile()
  const isHorizontal = mobile // 移动端横向显示
  
  return {
    tooltip: {
      trigger: 'axis',
      confine: true,
      formatter: (params) => {
        const param = params[0]
        return `${param.name}<br/>${param.seriesName}: ${param.value}`
      }
    },
    grid: {
      left: mobile ? '15%' : '3%',
      right: mobile ? '5%' : '4%',
      bottom: mobile ? '5%' : '15%',
      top: mobile ? '5%' : '10%',
      containLabel: true
    },
    xAxis: {
      type: isHorizontal ? 'value' : 'category',
      data: isHorizontal ? null : data.map(item => item.date),
      name: isHorizontal ? '支出' : '日期',
      nameLocation: 'middle',
      nameGap: mobile ? 20 : 30,
      axisLabel: {
        show: true,
        rotate: isHorizontal ? 0 : 45,
        margin: mobile ? 8 : 15,
        fontSize: mobile ? 10 : 12,
        color: '#666',
        formatter: (value) => {
          if (isHorizontal) {
            return (value / 1000).toFixed(1) + 'k'
          }
          const [year, month] = value.split('-')
          return mobile ? month + '月' : value
        }
      }
    },
    yAxis: {
      type: isHorizontal ? 'category' : 'value',
      data: isHorizontal ? data.map(item => item.date) : null,
      name: isHorizontal ? '日期' : '支出',
      nameLocation: 'middle',
      nameGap: mobile ? 30 : 40,
      axisLabel: {
        fontSize: mobile ? 10 : 12,
        color: '#666',
        formatter: (value) => {
          if (!isHorizontal) {
            return (value / 1000).toFixed(1) + 'k'
          }
          const [year, month] = value.split('-')
          return mobile ? month + '月' : value
        }
      }
    },
    series: [{
      data: data.map(item => ({
        value: item.expense,
        itemStyle: {
          color: '#1890ff'
        }
      })),
      type: 'bar',
      barWidth: mobile ? '60%' : '40%',
      itemStyle: {
        color: '#1890ff',
        borderRadius: [4, 4, 0, 0]
      }
    }],
    animation: true,
    animationDuration: 1000,
    animationEasing: 'cubicInOut'
  }
}

// 初始化图表
const initChart = (data) => {
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
  
  chart.setOption(getChartOption(data))
}

// 处理图表大小变化
const handleResize = () => {
  if (chart) {
    chart.resize()
  }
}

// 处理触摸事件
const handleTouchStart = (e) => {
  if (chart) {
    chart.dispatchAction({
      type: 'showTip',
      seriesIndex: 0,
      dataIndex: Math.floor(e.touches[0].clientX / (chartRef.value.clientWidth / props.data.length))
    })
  }
}

const handleTouchEnd = () => {
  if (chart) {
    chart.dispatchAction({
      type: 'hideTip'
    })
  }
}

// 生命周期钩子
onMounted(() => {
  initChart(props.data)
  window.addEventListener('resize', handleResize)
  chartRef.value.addEventListener('touchstart', handleTouchStart)
  chartRef.value.addEventListener('touchend', handleTouchEnd)
  setTimeout(handleResize, 100)
})

onUnmounted(() => {
  if (chart) {
    chart.dispose()
    chart = null
  }
  window.removeEventListener('resize', handleResize)
  if (chartRef.value) {
    chartRef.value.removeEventListener('touchstart', handleTouchStart)
    chartRef.value.removeEventListener('touchend', handleTouchEnd)
  }
})

// 监听数据变化
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
  touch-action: none;
}

.chart {
  flex: 1;
  width: 100%;
  min-height: 500px;
  min-width: 800px;
}

@media screen and (max-width: 768px) {
  .chart-container {
    padding: 10px;
  }
}
</style>