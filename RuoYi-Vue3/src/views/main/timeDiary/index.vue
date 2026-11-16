<template>
  <div class="time-diary-container">
    <!-- 时间轴视图 -->
    <div class="timeline-wrapper" :class="{ 'collapsed': timelineCollapsed }">
      <div class="timeline-card">
        <div class="card-header" @click="timelineCollapsed = !timelineCollapsed">
          <div class="header-title">
            <span class="header-icon">🕐</span>
            <span class="header-text">今日时间轴</span>
            <el-icon class="collapse-icon" :class="{ 'rotated': timelineCollapsed }"><ArrowDown /></el-icon>
          </div>
        </div>
        <transition name="slide-fade">
          <div v-show="!timelineCollapsed" class="card-body">
            <div class="timeline-toolbar">
              <el-date-picker
                v-model="timelineDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                class="date-picker"
                size="small"
                @change="loadTimeline"
              />
              <el-button type="primary" size="small" @click="handleAdd" v-hasPermi="['main:timeDiary:add']" class="timeline-add-btn">
                <el-icon><Plus /></el-icon>
                新增
              </el-button>
            </div>
            <div v-if="timelineList.length === 0" class="empty-timeline">
              <div class="empty-content">
                <img :src="emptyImage" class="empty-image" alt="空状态" />
                <p class="empty-text">这天还没有记录</p>
                <p class="empty-hint">快去记录你的时间吧～身体好 财运好 运气好 越来越好 🎉</p>
              </div>
            </div>
            <div v-else class="timeline-container">
              <div v-for="(item, index) in timelineList" :key="item.diaryId" class="timeline-item" :style="{ animationDelay: index * 0.1 + 's' }">
                <div class="timeline-time">
                  <span class="time-range">{{ item.startTime }} - {{ item.endTime }}</span>
                  <span class="duration-badge">{{ item.durationMin }}分钟</span>
                </div>
                <div class="timeline-line">
                  <div class="timeline-dot" :style="{ backgroundColor: getCategoryColor(item.category) }" />
                  <div v-if="index < timelineList.length - 1" class="timeline-connector" />
                </div>
                <div class="timeline-content">
                  <div class="timeline-title">
                    <el-tag :class="['category-tag', 'category-' + item.category]" size="small">
                      {{ getCategoryEmoji(item.category) }} {{ item.category }}
                    </el-tag>
                    <span class="title-text">{{ item.title }}</span>
                  </div>
                  <div v-if="item.description" class="timeline-desc">{{ item.description }}</div>
                </div>
                <div class="timeline-actions">
                  <el-button link type="primary" size="small" @click="handleUpdate(item)" v-hasPermi="['main:timeDiary:edit']">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleDelete(item)" v-hasPermi="['main:timeDiary:remove']">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- 列表区域 -->
    <div class="list-wrapper" :class="{ 'collapsed': listCollapsed }">
      <div class="list-card">
        <div class="card-header" @click="listCollapsed = !listCollapsed">
          <div class="header-title">
            <span class="header-icon">📋</span>
            <span class="header-text">历史记录</span>
            <el-icon class="collapse-icon" :class="{ 'rotated': listCollapsed }"><ArrowDown /></el-icon>
          </div>
        </div>
        <transition name="slide-fade">
          <div v-show="!listCollapsed" class="card-body">
            <el-form :model="queryParams" ref="queryRef" :inline="true" class="search-form" label-width="70px">
              <el-form-item label="日期" prop="diaryDate">
                <el-date-picker
                  v-model="queryParams.diaryDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择日期"
                  clearable
                  class="search-input"
                />
              </el-form-item>
              <el-form-item label="分类" prop="category">
                <el-select v-model="queryParams.category" placeholder="选择分类" clearable class="search-input">
                  <el-option
                      v-for="dict in time_diary_category"
                      :key="dict.value"
                      :label="dict.label"
                      :value="dict.value"
                  ></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="事项" prop="title">
                <el-input v-model="queryParams.title" placeholder="搜索事项..." clearable class="search-input" @keyup.enter="handleQuery" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleQuery" class="search-btn">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
                <el-button @click="resetQuery" class="reset-btn">
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>
              </el-form-item>
            </el-form>

            <div class="toolbar">
              <el-button type="primary" @click="handleAdd" v-hasPermi="['main:timeDiary:add']" class="action-btn">
                <el-icon><Plus /></el-icon>
                新增
              </el-button>
              <el-button type="danger" :disabled="multiple" @click="handleDelete" v-hasPermi="['main:timeDiary:remove']" class="action-btn">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
              <el-button type="success" @click="handleExport" v-hasPermi="['main:timeDiary:export']" class="action-btn">
                <el-icon><Download /></el-icon>
                导出
              </el-button>
            </div>

            <el-table v-loading="loading" :data="timeDiaryList" @selection-change="handleSelectionChange" class="data-table" stripe>
              <el-table-column type="selection" width="55" align="center" />
              <el-table-column label="日期" align="center" prop="diaryDate" width="120">
                <template #default="scope">
                  <span class="date-cell">{{ parseTime(scope.row.diaryDate, '{y}-{m}-{d}') }}</span>
                </template>
              </el-table-column>
              <el-table-column label="时间段" align="center" width="140">
                <template #default="scope">
                  <span class="time-cell">{{ scope.row.startTime }} - {{ scope.row.endTime }}</span>
                </template>
              </el-table-column>
              <el-table-column label="分类" align="center" prop="category" width="110">
                <template #default="scope">
                  <el-tag :class="['category-tag', 'category-' + scope.row.category]" size="small">
                    {{ getCategoryEmoji(scope.row.category) }} {{ scope.row.category }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="事项" align="center" prop="title" show-overflow-tooltip>
                <template #default="scope">
                  <span class="title-cell">{{ scope.row.title }}</span>
                </template>
              </el-table-column>
              <el-table-column label="时长" align="center" prop="durationMin" width="100">
                <template #default="scope">
                  <span class="duration-cell">{{ scope.row.durationMin }}分钟</span>
                </template>
              </el-table-column>
              <el-table-column label="描述" align="center" prop="description" show-overflow-tooltip />
              <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
                <template #default="scope">
                  <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['main:timeDiary:edit']">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['main:timeDiary:remove']">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <pagination
              v-show="total > 0"
              :total="total"
              v-model:page="queryParams.pageNum"
              v-model:limit="queryParams.pageSize"
              @pagination="getList"
              class="pagination-wrapper"
            />
          </div>
        </transition>
      </div>
    </div>

    <!-- 统计图表区域（移到最后） -->
    <div class="stats-row">
      <div class="stats-card pie-card" :class="{ 'collapsed': pieCollapsed }">
        <div class="card-header" @click="handlePieHeaderClick">
          <div class="header-title">
            <span class="header-icon"></span>
            <span class="header-text">时间都去哪儿了</span>
            <el-icon class="collapse-icon" :class="{ 'rotated': pieCollapsed }"><ArrowDown /></el-icon>
          </div>
          <el-date-picker
            v-model="statsDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="date-range-picker"
            size="small"
            @click.stop
            @mousedown.stop
            @change="loadStats"
          />
        </div>
        <transition name="slide-fade">
          <div v-show="!pieCollapsed" class="card-body chart-body">
            <div ref="pieChartRef" class="chart-container" />
          </div>
        </transition>
      </div>

      <div class="stats-card bar-card" :class="{ 'collapsed': barCollapsed }">
        <div class="card-header" @click="barCollapsed = !barCollapsed">
          <div class="header-title">
            <span class="header-icon">📊</span>
            <span class="header-text">每日时长趋势</span>
            <el-icon class="collapse-icon" :class="{ 'rotated': barCollapsed }"><ArrowDown /></el-icon>
          </div>
        </div>
        <transition name="slide-fade">
          <div v-show="!barCollapsed" class="card-body chart-body">
            <div ref="barChartRef" class="chart-container" />
          </div>
        </transition>
      </div>
    </div>

    <!-- 添加或修改对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body class="fancy-dialog">
      <img :src="emptyImage" class="dialog-banner" alt="身体好 财运好 运气好 越来越好" />
      <el-form ref="timeDiaryRef" :model="form" :rules="rules" label-width="80px" class="dialog-form">
        <el-form-item label="📅 日期" prop="diaryDate">
          <el-date-picker
            v-model="form.diaryDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="⏰ 时间段" prop="startTime">
          <div class="dialog-time-range">
            <el-time-picker
              v-model="form.startTime"
              value-format="HH:mm"
              placeholder="开始时间"
              @change="autoCalcDuration"
            />
            <span class="time-arrow">→</span>
            <el-time-picker
              v-model="form.endTime"
              value-format="HH:mm"
              placeholder="结束时间"
              @change="autoCalcDuration"
            />
          </div>
        </el-form-item>
        <el-form-item label="🏷️ 分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
            <el-option
                v-for="dict in time_diary_category"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="✨ 事项" prop="title">
          <el-input v-model="form.title" placeholder="请输入事项标题" />
        </el-form-item>
        <el-form-item label="📝 描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入详细描述" />
        </el-form-item>
        <el-form-item label="️ 时长" prop="durationMin">
          <el-input-number v-model="form.durationMin" :min="0" :step="5" placeholder="分钟" style="width: 100%" />
          <span class="form-hint">选择时间段后自动计算</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel" class="cancel-btn">取消</el-button>
          <el-button type="warning" @click="submitForm" class="confirm-btn">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="TimeDiary">
import { listTimeDiary, getTimeDiary, delTimeDiary, addTimeDiary, updateTimeDiary, getTimeDiaryStats, getTimeDiaryTimeline } from "@/api/main/timeDiary";
import * as echarts from 'echarts';
import { onMounted, onBeforeUnmount, nextTick } from 'vue';
import { ArrowDown, Edit, Delete, Search, Refresh, Plus, Download } from '@element-plus/icons-vue';
import emptyImage from '@/assets/images/time-diary-empty.jpg';

const { proxy } = getCurrentInstance();
const { time_diary_category } = proxy.useDict('time_diary_category');

// 分类颜色映射（降低饱和度）
const categoryColors = {
  '工作': '#5B9BD5',
  '学习': '#70AD47',
  '生活': '#ED7D31',
  '其他': '#A5A5A5'
};

const categoryEmojis = {
  '工作': '',
  '学习': '📚',
  '生活': '🏠',
  '其他': '🎮'
};

function getCategoryColor(category) {
  return categoryColors[category] || '#A5A5A5';
}

function getCategoryEmoji(category) {
  return categoryEmojis[category] || '📌';
}

// 折叠状态
const pieCollapsed = ref(false);
const barCollapsed = ref(false);
const timelineCollapsed = ref(false);
const listCollapsed = ref(false);

// 统计图表
const statsDateRange = ref([]);
const pieChartRef = ref(null);
const barChartRef = ref(null);
let pieChart = null;
let barChart = null;

// 时间轴
const timelineDate = ref(new Date().toISOString().split('T')[0]);
const timelineList = ref([]);

// 列表
const timeDiaryList = ref([]);
const open = ref(false);
const loading = ref(true);
const ids = ref([]);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    diaryDate: null,
    category: null,
    title: null,
  },
  rules: {
    diaryDate: [{ required: true, message: "请选择日期", trigger: "change" }],
    startTime: [
      { required: true, message: "请选择开始时间", trigger: "change" },
      {
        validator: (rule, value, callback) => {
          if (!form.value.endTime) {
            callback(new Error("请选择结束时间"));
          } else {
            callback();
          }
        },
        trigger: "change"
      }
    ],
    category: [{ required: true, message: "请选择分类", trigger: "change" }],
    title: [{ required: true, message: "请输入事项标题", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  listTimeDiary(queryParams.value).then(response => {
    timeDiaryList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function loadStats() {
  const params = {};
  if (statsDateRange.value && statsDateRange.value.length === 2) {
    params['params[beginTime]'] = statsDateRange.value[0];
    params['params[endTime]'] = statsDateRange.value[1];
  }
  getTimeDiaryStats(params).then(response => {
    renderPieChart(response.data.categoryDistribution || []);
    renderBarChart(response.data.dailyTrend || [], response.data.dailyCategoryTrend || []);
  });
}

function loadTimeline() {
  if (!timelineDate.value) return;
  getTimeDiaryTimeline({ diaryDate: timelineDate.value }).then(response => {
    timelineList.value = response.data || [];
  });
}

// 处理饼图卡片头部点击，避免日期选择器触发折叠
function handlePieHeaderClick(event) {
  // 如果点击的是日期选择器或其子元素，不折叠
  if (event.target.closest('.el-date-editor')) {
    return;
  }
  pieCollapsed.value = !pieCollapsed.value;
}

function renderPieChart(data) {
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value);
  }
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}分钟 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { fontSize: 12 }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 3
      },
      label: {
        show: true,
        formatter: '{b}\n{d}%',
        fontSize: 13
      },
      emphasis: {
        label: { show: true, fontSize: 15, fontWeight: 'bold' },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      data: data.map(item => ({
        name: item.category,
        value: item.totalMinutes,
        itemStyle: { color: getCategoryColor(item.category) }
      }))
    }]
  };
  pieChart.setOption(option, true);
}

function renderBarChart(dailyData, dailyCategoryData) {
  if (!barChart) {
    barChart = echarts.init(barChartRef.value);
  }

  // 空状态处理
  if (!dailyData || dailyData.length === 0) {
    barChart.clear();
    barChart.setOption({
      title: {
        text: '暂无数据',
        subtext: '快去记录你的时间吧～',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 16 },
        subtextStyle: { color: '#ccc', fontSize: 12 }
      }
    });
    return;
  }

  // 获取所有日期（保留所有日期，包括没有数据的）
  const allDates = dailyData.map(item => item.diaryDate);

  // 获取所有分类
  const categories = [...new Set(dailyCategoryData.map(item => item.category))];

  // 格式化时长显示
  function formatDuration(mins) {
    if (mins >= 60) {
      const hours = Math.floor(mins / 60);
      const minutes = mins % 60;
      return minutes > 0 ? `${hours}h${minutes}m` : `${hours}h`;
    }
    return `${mins}m`;
  }

  // 为每个分类创建数据系列
  const series = categories.map(category => {
    const data = allDates.map(date => {
      const record = dailyCategoryData.find(item => item.diaryDate === date && item.category === category);
      return record ? record.totalMinutes : 0;
    });
    return {
      name: category,
      type: 'bar',
      stack: 'total',
      data: data,
      itemStyle: {
        color: getCategoryColor(category)
      },
      emphasis: {
        focus: 'series'
      }
    };
  });

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: function(params) {
        let result = params[0].name + '<br/>';
        let total = 0;
        params.forEach(param => {
          if (param.value > 0) {
            result += `${param.marker} ${param.seriesName}: ${formatDuration(param.value)}<br/>`;
            total += param.value;
          }
        });
        result += `<strong>总计：${formatDuration(total)}</strong>`;
        return result;
      }
    },
    legend: {
      data: categories,
      top: 10,
      textStyle: { fontSize: 12 }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true, top: 50 },
    xAxis: {
      type: 'category',
      data: allDates,
      axisLabel: { rotate: 30, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '时长',
      axisLabel: { 
        formatter: function(value) {
          return formatDuration(value);
        }
      }
    },
    series: series
  };
  barChart.setOption(option, true);
}

// 自动计算时长
function autoCalcDuration() {
  // 使用 nextTick 确保 form.value 已更新
  nextTick(() => {
    if (form.value.startTime && form.value.endTime) {
      const [sh, sm] = form.value.startTime.split(':').map(Number);
      const [eh, em] = form.value.endTime.split(':').map(Number);
      let mins = (eh * 60 + em) - (sh * 60 + sm);
      if (mins < 0) mins += 24 * 60;
      form.value.durationMin = mins;
    }
  });
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    diaryId: null,
    diaryDate: null,
    startTime: null,
    endTime: null,
    category: '工作',
    title: null,
    description: null,
    durationMin: 0
  };
  proxy.resetForm("timeDiaryRef");
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.diaryId);
  multiple.value = !selection.length;
}

function handleAdd() {
  reset();
  const today = new Date().toISOString().split('T')[0];
  form.value.diaryDate = today;
  // 查询今天所有记录（倒序），取第一条的结束时间作为开始时间（不跨天）
  listTimeDiary({
    pageNum: 1,
    pageSize: 100,
    diaryDate: today,
    orderByColumn: 'start_time',
    isAsc: 'desc'
  }).then(response => {
    if (response.rows && response.rows.length > 0) {
      // 倒序排列，第一条是最新的
      form.value.startTime = response.rows[0].endTime;
    }
  });
  open.value = true;
  title.value = "✨ 添加时间管理局";
}

function handleUpdate(row) {
  reset();
  const _id = row.diaryId || ids.value[0];
  getTimeDiary(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "✏️ 修改时间管理局";
  });
}

function submitForm() {
  proxy.$refs["timeDiaryRef"].validate(valid => {
    if (valid) {
      if (form.value.startTime && form.value.endTime && !form.value.durationMin) {
        const [sh, sm] = form.value.startTime.split(':').map(Number);
        const [eh, em] = form.value.endTime.split(':').map(Number);
        let mins = (eh * 60 + em) - (sh * 60 + sm);
        if (mins < 0) mins += 24 * 60;
        form.value.durationMin = mins;
      }
      if (form.value.diaryId != null) {
        updateTimeDiary(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功 ✨");
          open.value = false;
          getList();
          loadStats();
          loadTimeline();
        });
      } else {
        addTimeDiary(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功 🎉");
          open.value = false;
          getList();
          loadStats();
          loadTimeline();
        });
      }
    }
  });
}

function handleDelete(row) {
  const _ids = row.diaryId || ids.value;
  proxy.$modal.confirm('确认要删除这条时间记录吗？').then(() => {
    return delTimeDiary(_ids);
  }).then(() => {
    getList();
    loadStats();
    loadTimeline();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

function handleExport() {
  proxy.download('main/timeDiary/export', {
    ...queryParams.value
  }, `timeDiary_${new Date().getTime()}.xlsx`);
}

function handleResize() {
  pieChart && pieChart.resize();
  barChart && barChart.resize();
}

onMounted(() => {
  getList();
  loadStats();
  loadTimeline();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  pieChart && pieChart.dispose();
  barChart && barChart.dispose();
});
</script>

<style scoped>
/* 容器样式 - 柔和背景 */
.time-diary-container {
  padding: 20px;
  background: #F5F7FA;
  min-height: 100vh;
}

/* 卡片通用样式 */
.stats-card,
.timeline-wrapper,
.list-wrapper {
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.stats-card,
.timeline-card,
.list-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* 统计卡片 - 柔和配色 */
.pie-card {
  border-top: 3px solid #5B9BD5;
}

.pie-card .card-header {
  background: linear-gradient(90deg, #5B9BD5 0%, #7FB3E0 100%);
  color: white;
  padding: 14px 20px;
}

.bar-card {
  border-top: 3px solid #70AD47;
}

.bar-card .card-header {
  background: linear-gradient(90deg, #70AD47 0%, #8FC473 100%);
  color: white;
  padding: 14px 20px;
}

/* 时间轴卡片 - 柔和紫色 */
.timeline-card {
  border-top: 3px solid #9B8EC4;
}

.timeline-card .card-header {
  background: linear-gradient(90deg, #9B8EC4 0%, #B5A8D4 100%);
  color: white;
  padding: 14px 20px;
}

/* 列表卡片 - 柔和红色 */
.list-card {
  border-top: 3px solid #E07A7A;
}

.list-card .card-header {
  background: linear-gradient(90deg, #E07A7A 0%, #E89A9A 100%);
  color: white;
  padding: 14px 20px;
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
  transition: all 0.3s ease;
}

.card-header:hover {
  filter: brightness(1.05);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
}

.header-icon {
  font-size: 18px;
}

.header-text {
  letter-spacing: 0.5px;
}

.collapse-icon {
  transition: transform 0.3s ease;
  font-size: 16px;
}

.collapse-icon.rotated {
  transform: rotate(-90deg);
}

.header-tag {
  background: rgba(255, 255, 255, 0.25) !important;
  border-color: rgba(255, 255, 255, 0.4) !important;
  color: white !important;
}

/* 卡片内容 */
.card-body {
  padding: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.form-input {
  width: 160px;
}

.title-input {
  width: 220px;
}

.category-select {
  width: 140px;
}

.time-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-input {
  width: 120px;
}

.time-separator {
  color: #F0AD4E;
  font-weight: bold;
  font-size: 16px;
}

.submit-btn {
  background: linear-gradient(135deg, #F0AD4E 0%, #E89A3A 100%);
  border: none;
  border-radius: 20px;
  padding: 10px 28px;
  font-weight: 600;
  box-shadow: 0 3px 10px rgba(240, 173, 78, 0.3);
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(240, 173, 78, 0.4);
}

/* 统计区域布局 */
.stats-row {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 20px;
}

.chart-body {
  padding: 16px;
}

.chart-container {
  height: 300px;
}

@media (max-width: 768px) {
  .chart-container {
    height: 250px;
  }
}

.date-range-picker {
  width: 220px;
}

.date-picker {
  width: 150px;
}

.timeline-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #F0F0F0;
}

.timeline-add-btn {
  border-radius: 20px;
  background: linear-gradient(135deg, #FF9A9E 0%, #FECFEF 100%);
  border: none;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(255, 154, 158, 0.3);
  transition: all 0.3s ease;
}

.timeline-add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 154, 158, 0.4);
}

/* 时间轴样式 */
.timeline-container {
  padding: 10px 0;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  padding: 14px 0;
  border-bottom: 1px dashed #E8E8E8;
  position: relative;
  animation: slideIn 0.4s ease forwards;
  opacity: 0;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-15px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.timeline-item:last-child {
  border-bottom: none;
}

.timeline-time {
  width: 140px;
  flex-shrink: 0;
  text-align: right;
  padding-right: 20px;
}

.time-range {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.duration-badge {
  display: inline-block;
  margin-top: 4px;
  padding: 2px 8px;
  background: #E3F2FD;
  border-radius: 10px;
  font-size: 11px;
  color: #5B9BD5;
  font-weight: 500;
}

.timeline-line {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 24px;
  flex-shrink: 0;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  box-shadow: 0 0 0 3px white, 0 2px 6px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 2;
  transition: transform 0.2s ease;
}

.timeline-item:hover .timeline-dot {
  transform: scale(1.2);
}

.timeline-connector {
  width: 2px;
  flex: 1;
  background: #E8E8E8;
  margin-top: 4px;
}

.timeline-content {
  flex: 1;
  padding: 0 20px;
  min-width: 0;
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.category-tag {
  border-radius: 12px;
  font-weight: 500;
}

.category-工作 {
  background: #5B9BD5;
  color: white;
  border: none;
}

.category-学习 {
  background: #70AD47;
  color: white;
  border: none;
}

.category-生活 {
  background: #ED7D31;
  color: white;
  border: none;
}

.category-其他 {
  background: #A5A5A5;
  color: white;
  border: none;
}

.title-text {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.timeline-desc {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.timeline-item:hover .timeline-actions {
  opacity: 1;
}

/* 空状态 */
.empty-timeline {
  padding: 40px 0;
  text-align: center;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-image {
  max-width: 400px;
  width: 100%;
  height: auto;
  border-radius: 12px;
  opacity: 0.9;
}

.empty-text {
  font-size: 15px;
  color: #666;
  margin: 0;
}

.empty-hint {
  font-size: 13px;
  color: #999;
  margin: 0;
}

/* 搜索表单 */
.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
  padding: 16px;
  background: #F8F9FA;
  border-radius: 8px;
  margin-bottom: 16px;
}

.search-input {
  width: 160px;
}

.search-btn,
.reset-btn {
  border-radius: 18px;
  padding: 9px 22px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding: 0 4px;
}

.action-btn {
  border-radius: 18px;
  padding: 9px 22px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.12);
}

/* 数据表格 */
.data-table {
  border-radius: 8px;
  overflow: hidden;
}

.data-table :deep(.el-table__header) {
  background: #F8F9FA;
}

.data-table :deep(.el-table__header th) {
  color: #606266;
  font-weight: 600;
  padding: 12px 0;
}

.data-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.data-table :deep(.el-table__row:hover) {
  background: #F8F9FA !important;
}

.date-cell,
.time-cell {
  font-weight: 500;
  color: #303133;
}

.title-cell {
  font-weight: 500;
}

.duration-cell {
  color: #ED7D31;
  font-weight: 600;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 对话框样式 */
.fancy-dialog :deep(.el-dialog__header) {
  background: linear-gradient(90deg, #F0AD4E 0%, #E89A3A 100%);
  color: white;
  padding: 18px 20px;
  border-radius: 12px 12px 0 0;
}

.fancy-dialog :deep(.el-dialog__title) {
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.fancy-dialog :deep(.el-dialog__headerbtn) {
  top: 18px;
}

.fancy-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: white;
}

.dialog-banner {
  display: block;
  width: 100%;
  height: auto;
  border-radius: 8px;
  margin-bottom: 4px;
}

.dialog-form {
  padding: 20px;
}

.dialog-time-range {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.dialog-time-range :deep(.el-time-picker) {
  flex: 1;
}

.time-arrow {
  color: #FF9A9E;
  font-weight: bold;
  font-size: 16px;
  flex-shrink: 0;
}

.form-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #EBEEF5;
}

.cancel-btn,
.confirm-btn {
  border-radius: 18px;
  padding: 9px 28px;
  font-weight: 500;
}

.confirm-btn {
  background: linear-gradient(135deg, #F0AD4E 0%, #E89A3A 100%);
  border: none;
  box-shadow: 0 3px 10px rgba(240, 173, 78, 0.3);
}

.confirm-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(240, 173, 78, 0.4);
}

/* 折叠动画 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 夜间模式适配 - 使用 html.dark 选择器 */
html.dark .time-diary-container {
  background: #1a1a2e !important;
}

html.dark .stats-card,
html.dark .timeline-card,
html.dark .list-card {
  background: #16213e !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3) !important;
}


html.dark .pie-card .card-header {
  background: linear-gradient(90deg, #4A7BA7 0%, #6B9BC7 100%) !important;
}

html.dark .bar-card .card-header {
  background: linear-gradient(90deg, #5A8A37 0%, #7AAA57 100%) !important;
}

html.dark .timeline-card .card-header {
  background: linear-gradient(90deg, #7B6EA4 0%, #9588B4 100%) !important;
}

html.dark .list-card .card-header {
  background: linear-gradient(90deg, #C06A6A 0%, #D08A8A 100%) !important;
}

html.dark .card-body {
  background: #16213e !important;
  color: #e0e0e0 !important;
}


html.dark .search-form {
  background: #1a1a2e !important;
}

html.dark .search-input :deep(.el-input__wrapper) {
  background: #16213e !important;
}

html.dark .search-input :deep(.el-input__inner) {
  color: #e0e0e0 !important;
}

html.dark .data-table :deep(.el-table) {
  background: #16213e !important;
  color: #e0e0e0 !important;
}

html.dark .data-table :deep(.el-table__header) {
  background: #1a1a2e !important;
}

html.dark .data-table :deep(.el-table__header th) {
  background: #1a1a2e !important;
  color: #b0b0b0 !important;
  border-bottom-color: #2a2a4a !important;
}

html.dark .data-table :deep(.el-table__row) {
  background: #16213e !important;
  color: #e0e0e0 !important;
}

html.dark .data-table :deep(.el-table__row:hover) {
  background: #1a1a2e !important;
}

html.dark .data-table :deep(.el-table__row td) {
  border-bottom-color: #2a2a4a !important;
}

html.dark .date-cell,
html.dark .time-cell,
html.dark .title-cell {
  color: #e0e0e0 !important;
}

html.dark .timeline-desc {
  color: #b0b0b0 !important;
}

html.dark .empty-text {
  color: #b0b0b0 !important;
}

html.dark .empty-hint {
  color: #808080 !important;
}

html.dark .form-hint {
  color: #808080 !important;
}

html.dark .duration-cell {
  color: #F0AD4E !important;
}

html.dark .category-tag {
  opacity: 0.85 !important;
}

html.dark .dialog-form {
  color: #e0e0e0 !important;
}

html.dark .dialog-form :deep(.el-form-item__label) {
  color: #e0e0e0 !important;
}

html.dark .dialog-form :deep(.el-input__wrapper) {
  background: #1a1a2e !important;
}

html.dark .dialog-form :deep(.el-input__inner) {
  color: #e0e0e0 !important;
}

html.dark .fancy-dialog :deep(.el-dialog) {
  background: #16213e !important;
}

html.dark .fancy-dialog :deep(.el-dialog__body) {
  background: #16213e !important;
  color: #e0e0e0 !important;
}

html.dark .fancy-dialog :deep(.el-dialog__footer) {
  background: #16213e !important;
  border-top-color: #2a2a4a !important;
}

html.dark .header-text {
  color: white !important;
}

html.dark .time-range {
  color: #e0e0e0 !important;
}

html.dark .duration-badge {
  background: #2a3a5e !important;
  color: #8BB8E8 !important;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .time-diary-container {
    padding: 12px;
  }

  .form-input,
  .title-input,
  .category-select,
  .time-input {
    width: 100%;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .toolbar {
    flex-wrap: wrap;
  }
}
</style>

<!-- 夜间模式样式（非 scoped） -->
<style>
html.dark .time-diary-container {
  background: #1a1a2e !important;
}

html.dark .stats-card,
html.dark .timeline-card,
html.dark .list-card {
  background: #16213e !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3) !important;
}


html.dark .pie-card .card-header {
  background: linear-gradient(90deg, #4A7BA7 0%, #6B9BC7 100%) !important;
}

html.dark .bar-card .card-header {
  background: linear-gradient(90deg, #5A8A37 0%, #7AAA57 100%) !important;
}

html.dark .timeline-card .card-header {
  background: linear-gradient(90deg, #7B6EA4 0%, #9588B4 100%) !important;
}

html.dark .list-card .card-header {
  background: linear-gradient(90deg, #C06A6A 0%, #D08A8A 100%) !important;
}

html.dark .card-body {
  background: #16213e !important;
  color: #e0e0e0 !important;
}


html.dark .search-form {
  background: #1a1a2e !important;
}

html.dark .search-input .el-input__wrapper {
  background: #16213e !important;
}

html.dark .search-input .el-input__inner {
  color: #e0e0e0 !important;
}

html.dark .data-table .el-table {
  background: #16213e !important;
  color: #e0e0e0 !important;
}

html.dark .data-table .el-table__header {
  background: #1a1a2e !important;
}

html.dark .data-table .el-table__header th {
  background: #1a1a2e !important;
  color: #b0b0b0 !important;
  border-bottom-color: #2a2a4a !important;
}

html.dark .data-table .el-table__row {
  background: #16213e !important;
  color: #e0e0e0 !important;
}

html.dark .data-table .el-table__row:hover {
  background: #1a1a2e !important;
}

html.dark .data-table .el-table__row td {
  border-bottom-color: #2a2a4a !important;
}

html.dark .date-cell,
html.dark .time-cell,
html.dark .title-cell {
  color: #e0e0e0 !important;
}

html.dark .timeline-desc {
  color: #b0b0b0 !important;
}

html.dark .empty-text {
  color: #b0b0b0 !important;
}

html.dark .empty-hint {
  color: #808080 !important;
}

html.dark .form-hint {
  color: #808080 !important;
}

html.dark .duration-cell {
  color: #F0AD4E !important;
}

html.dark .category-tag {
  opacity: 0.85 !important;
}

html.dark .dialog-form {
  color: #e0e0e0 !important;
}

html.dark .dialog-form .el-form-item__label {
  color: #e0e0e0 !important;
}

html.dark .dialog-form .el-input__wrapper {
  background: #1a1a2e !important;
}

html.dark .dialog-form .el-input__inner {
  color: #e0e0e0 !important;
}

html.dark .fancy-dialog .el-dialog {
  background: #16213e !important;
}

html.dark .fancy-dialog .el-dialog__body {
  background: #16213e !important;
  color: #e0e0e0 !important;
}

html.dark .fancy-dialog .el-dialog__footer {
  background: #16213e !important;
  border-top-color: #2a2a4a !important;
}

html.dark .header-text {
  color: white !important;
}

html.dark .time-range {
  color: #e0e0e0 !important;
}

html.dark .title-text {
  color: #e0e0e0 !important;
}

html.dark .timeline-title {
  color: #e0e0e0 !important;
}

html.dark .duration-badge {
  background: #2a3a5e !important;
  color: #8BB8E8 !important;
}

html.dark .category-tag {
  opacity: 0.9 !important;
}

html.dark .category-工作 {
  background: #4A7BA7 !important;
}

html.dark .category-学习 {
  background: #5A8A37 !important;
}

html.dark .category-生活 {
  background: #C06A3A !important;
}

html.dark .category-其他 {
  background: #8A8A8A !important;
}
</style>
