<template>
  <div>
    <div ref="container" id="container"></div>
    <button @click="handleUpdate">更新数据</button>
    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, defineProps } from 'vue'
import { Chart } from '@antv/g2'

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
})

const error = ref(null)
const chart = ref(null)

const initChart = (data) => {
  try {
    // 销毁旧实例
    debugger
    if (chart.value) chart.value.destroy()
    chart.value = new Chart({
      container: 'container',
    })
    chart.value
        .interval()
        .data(data)
        .encode('x', 'date')
        .encode('y', 'expense')
    chart.value.render()
  } catch (e) {
    error.value = '图表初始化失败: ' + e.message
  }
}

// 生命周期管理保持不变
onMounted(initChart);

// 修改后的监听逻辑
watch(
    () => [props.data],
    () => {
      initChart();
    },
    { deep: true }
);
</script>

<style scoped>
/* 样式部分保持不变 */
</style>