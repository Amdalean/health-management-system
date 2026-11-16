<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="日期范围" prop="dateRange">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>考勤用户总数</span></template>
          <div class="stat-value">{{ totalAssigned }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="statisticsList" border @row-click="handleRowClick" highlight-current-row>
      <el-table-column label="日期" align="center" prop="clockinDate" width="130" sortable />
      <el-table-column label="星期" align="center" width="90">
        <template #default="scope">
          <span>{{ getWeekday(scope.row.clockinDate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="工作日" align="center" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.isWorkday === true" type="success" size="small">工作日</el-tag>
          <el-tag v-else-if="scope.row.isWorkday === false && scope.row.typeName !== '未知'" type="danger" size="small">休息日</el-tag>
          <span v-else>-</span>
          <div v-if="scope.row.holidayName" style="font-size: 11px; color: #e6a23c; margin-top: 2px;">
            {{ scope.row.holidayName }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="已打卡" align="center" prop="clockinCount" width="90" sortable />
      <el-table-column label="应打卡" align="center" width="90">
        <template #default="scope"><span>{{ totalAssigned }}</span></template>
      </el-table-column>
      <el-table-column label="打卡率" align="center" min-width="180">
        <template #default="scope">
          <el-progress
            :percentage="getRate(scope.row.clockinCount)"
            :stroke-width="16"
            :color="getRateColor(scope.row.clockinCount)"
            :text-inside="true"
          />
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 打卡明细弹窗 -->
    <el-dialog :title="detailTitle" v-model="detailOpen" width="980px" append-to-body destroy-on-close>
      <el-row :gutter="20" class="mb8">
        <el-col :span="8" v-if="detailData.typeName && detailData.typeName !== '未知'">
          <el-tag :type="detailData.isWorkday ? 'success' : 'danger'" size="large" effect="dark">
            {{ detailData.typeName || (detailData.isWorkday ? '工作日' : '休息日') }}
          </el-tag>
          <span v-if="detailData.holidayName" style="margin-left: 8px; color: #e6a23c;">{{ detailData.holidayName }}</span>
        </el-col>
        <el-col :span="8">
          <span>已打卡 <strong>{{ detailData.clockedInUsers ? detailData.clockedInUsers.length : 0 }}</strong> 人</span>
        </el-col>
        <el-col :span="8">
          <span>未打卡 <strong>{{ detailData.notClockedInUsers ? detailData.notClockedInUsers.length : 0 }}</strong> 人</span>
        </el-col>
      </el-row>

      <el-tabs v-model="detailTab">
        <el-tab-pane label="已打卡用户" name="clockedIn">
          <el-table
            :data="detailData.clockedInUsers"
            border max-height="400"
            row-key="userId"
            @expand-change="handleExpandChange"
            v-if="detailData.clockedInUsers && detailData.clockedInUsers.length > 0"
          >
            <el-table-column type="expand">
              <template #default="scope">
                <div v-loading="scope.row._loading" style="padding: 8px 20px;">
                  <el-table :data="scope.row._records" border size="small" v-if="scope.row._records && scope.row._records.length > 0">
                    <el-table-column label="打卡时间" align="center" prop="clockinTime" width="170">
                      <template #default="{ row: r }">
                        <span>{{ parseTime(r.clockinTime) }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="类型" align="center" prop="clockinType" width="80">
                      <template #default="{ row: r }">
                        <el-tag :type="r.clockinType === '内勤' ? 'success' : 'warning'" size="small">{{ r.clockinType || '-' }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="地址" align="center" prop="address" min-width="250" :show-overflow-tooltip="true" />
                    <el-table-column label="经度" align="center" prop="longitude" width="110" />
                    <el-table-column label="纬度" align="center" prop="latitude" width="110" />
                  </el-table>
                  <el-empty v-else description="暂无记录" :image-size="40" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="用户" align="center" prop="userName" width="100" />
            <el-table-column label="昵称" align="center" prop="nickName" width="100" />
            <el-table-column label="部门" align="center" prop="deptName" width="100" />
            <el-table-column label="打卡次数" align="center" prop="clockinCount" width="90">
              <template #default="scope">
                <el-tag size="small">{{ scope.row.clockinCount }} 次</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="首次打卡（上班）" align="center" width="170">
              <template #default="scope">
                <span>{{ parseTime(scope.row.firstClockinTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="末次打卡（下班）" align="center" width="170">
              <template #default="scope">
                <span>{{ scope.row.clockinCount > 1 ? parseTime(scope.row.lastClockinTime) : '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="当天无人打卡" />
        </el-tab-pane>

        <el-tab-pane label="未打卡用户" name="notClockedIn">
          <el-table :data="detailData.notClockedInUsers" border max-height="400" v-if="detailData.notClockedInUsers && detailData.notClockedInUsers.length > 0">
            <el-table-column label="用户" align="center" prop="userName" width="120" />
            <el-table-column label="昵称" align="center" prop="nickName" width="120" />
            <el-table-column label="部门" align="center" prop="deptName" min-width="150" />
          </el-table>
          <el-empty v-else description="全员已打卡 🎉" />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup name="ClockinStatistics">
import { listClockinStatistics, getDailyDetail, getUserRecords } from "@/api/mobile/clockin";

const { proxy } = getCurrentInstance();

const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const totalAssigned = ref(0);
const statisticsList = ref([]);

const detailOpen = ref(false);
const detailTab = ref('clockedIn');
const detailTitle = ref('');
const detailData = ref({});
const detailDate = ref('');

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 31 }
});
const { queryParams } = toRefs(data);
const dateRange = ref([]);

function getList() {
  loading.value = true;
  const params = {};
  if (dateRange.value && dateRange.value.length === 2) {
    params.beginDate = dateRange.value[0];
    params.endDate = dateRange.value[1];
  }
  listClockinStatistics(params).then(response => {
    statisticsList.value = response.data.list || [];
    totalAssigned.value = response.data.totalAssigned || 0;
    total.value = statisticsList.value.length;
    loading.value = false;
  });
}

function handleQuery() { queryParams.value.pageNum = 1; getList(); }
function resetQuery() { dateRange.value = []; handleQuery(); }

function getWeekday(dateStr) {
  if (!dateStr) return '-';
  return ['星期日','星期一','星期二','星期三','星期四','星期五','星期六'][new Date(dateStr).getDay()];
}

function getRate(count) {
  if (!totalAssigned.value || totalAssigned.value === 0) return 0;
  return Math.round((count / totalAssigned.value) * 100);
}

function getRateColor(count) {
  const rate = getRate(count);
  if (rate >= 80) return '#67c23a';
  if (rate >= 60) return '#e6a23c';
  return '#f56c6c';
}

function handleRowClick(row) {
  detailTitle.value = row.clockinDate + ' ' + getWeekday(row.clockinDate) + ' 打卡明细';
  detailDate.value = row.clockinDate;
  detailData.value = {};
  detailOpen.value = true;
  detailTab.value = 'clockedIn';

  getDailyDetail(row.clockinDate).then(response => {
    detailData.value = response.data;
  }).catch(() => {
    proxy.$modal.msgError('加载明细失败');
    detailOpen.value = false;
  });
}

function handleExpandChange(row, expandedRows) {
  if (expandedRows.some(r => r.userId === row.userId)) {
    if (!row._records) {
      row._loading = true;
      getUserRecords(row.userId, detailDate.value).then(response => {
        row._records = response.data || [];
        row._loading = false;
      }).catch(() => {
        row._records = [];
        row._loading = false;
      });
    }
  }
}

getList();
</script>

<style scoped>
.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #409eff;
  text-align: center;
  padding: 10px 0;
}
.mb8 { margin-bottom: 8px; }
</style>
