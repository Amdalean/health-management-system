<template>
  <div>
    <div ref="container" id="container"></div>
    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, defineProps } from 'vue'
import { Chart } from '@antv/g2'

const props = defineProps({
  data: {
    type: Array,
    required: true
  }
})

const error = ref(null)
const chart = ref(null)

const initChart = (data) => {
  try {
    // 销毁旧实例
    if (chart.value) {
      chart.value.destroy();
      chart.value = null;
    }
    
    chart.value = new Chart({
      container: 'container',
      autoFit: true,
      height: 400,
    });

    chart.value
      .line()
      .data(data)
      .encode('x', 'date')
      .encode('y', 'expense')
      .scale('y', {
        nice: true,
      })
      .axis('x', {
        title: '日期',
      })
      .axis('y', {
        title: '支出',
      })
      .style({
        stroke: '#1890ff',
        lineWidth: 2,
      });

    // 添加数据点
    chart.value
      .point()
      .data(data)
      .encode('x', 'date')
      .encode('y', 'expense')
      .style({
        fill: '#1890ff',
        stroke: '#fff',
        lineWidth: 2,
      });

    chart.value.render();
  } catch (e) {
    error.value = '图表初始化失败: ' + e.message;
    console.error('图表初始化错误:', e);
  }
}

// 生命周期管理
onMounted(() => {
  initChart(props.data);
});

onUnmounted(() => {
  if (chart.value) {
    chart.value.destroy();
    chart.value = null;
  }
});

// 修改后的监听逻辑
watch(
  () => props.data,
  (newData) => {
    if (newData && newData.length > 0) {
      initChart(newData);
    }
  },
  { deep: true }
);
</script>

<style scoped>
/* 样式部分保持不变 */
</style>