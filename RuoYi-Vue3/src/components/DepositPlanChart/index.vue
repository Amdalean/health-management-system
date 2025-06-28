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
  
  return {
    title: {
      text: '存款计划执行情况',
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
      data: ['实际存款', '计划存款'],
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
    series: [
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
    ],
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

// 生命周期钩子
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
  
  .chart {
    min-height: 300px;
    min-width: 300px;
  }
}
</style> 