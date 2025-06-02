<template>
  <!-- 保持模板部分不变 -->
  <div>
    <div style="margin-bottom: 20px">
      <a-radio-group v-model:value="chartType">
        <a-radio-button value="line">趋势图</a-radio-button>
        <a-radio-button value="column">柱状图</a-radio-button>
        <a-radio-button value="pie">占比图</a-radio-button>
      </a-radio-group>
    </div>
    <div ref="chartContainer" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue';
import { Chart } from '@antv/g2';

const props = defineProps({
  data: {
    type: Object,
    required: true,
    default: () => ({ rows: [] })
  }
});

const chartContainer = ref(null);
const chartType = ref('pie');
let chartInstance = null;

// 修改后的数据处理方法
const processData = () => {
  return props.data.rows.map(item => ({
    month: `${item.year}-${String(item.month).padStart(2, '0')}`,
    income: item.income,
    expense: item.expense,
    endDeposit: item.endDeposit,
    balance: item.balance
  }));
};

// 修复后的创建图表方法
const createChart = () => {
  if (!chartContainer.value) return;

  // 销毁旧实例
  if (chartInstance) {
    chartInstance.destroy();
  }

  chartInstance = new Chart({
    container: chartContainer.value,
    autoFit: true,
    height: 400,
  });

  updateChart();
};

// 更新后的图表配置方法
const updateChart = () => {
  if (!chartInstance) return;

  const data = processData();
  chartInstance.clear();
  chartInstance.data(data);

  switch (chartType.value) {
    case 'line':
      // 修改后的折线图配置
      chartInstance
          .line()
          .encode('x', 'month')
          .encode('y', 'value')
          .encode('color', 'type')
          .transform({
            type: 'fold',
            fields: ['income', 'expense', 'endDeposit'],
            key: 'type',
            value: 'value',
          });
      break;

    case 'column':
      // 修改后的柱状图配置
      chartInstance
          .interval()
          .encode('x', 'month')
          .encode('y', 'value')
          .encode('color', 'type')
          .transform({
            type: 'fold',
            fields: ['income', 'expense'],
            key: 'type',
            value: 'value',
          })
          .adjust('stack');
      break;

    case 'pie':
      // 饼图配置保持不变
      const totalIncome = data.reduce((sum, d) => sum + d.income, 0);
      const pieData = [
        { name: '总收入', value: totalIncome },
        { name: '总支出', value: data.reduce((sum, d) => sum + d.expense, 0) }
      ];
      chartInstance
          .interval()
          .coordinate({ type: 'theta' })
          .encode('y', 'value')
          .encode('color', 'name')
          .data(pieData);
      break;
  }

  chartInstance.render();
};

// 生命周期管理保持不变
onMounted(createChart);
onBeforeUnmount(() => chartInstance?.destroy());

// 修改后的监听逻辑
watch(
    () => [props.data, chartType.value],
    () => {
      createChart();
    },
    { deep: true }
);
</script>

<style>
.chart-container {
  width: 100%;
  min-height: 400px;
}
</style>