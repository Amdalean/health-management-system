<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户名称" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="部门名称" prop="deptName">
        <el-input
          v-model="queryParams.deptName"
          placeholder="请输入部门名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="打卡时间" prop="createTime">
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
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mobile:clockin:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mobile:clockin:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="DataAnalysis"
          @click="goStatistics"
          v-hasPermi="['mobile:clockin:statistics']"
        >打卡统计</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="clockinList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="用户" align="center" prop="userName" width="120" />
      <el-table-column label="昵称" align="center" prop="nickName" width="100" />
      <el-table-column label="部门" align="center" prop="deptName" width="120" />
      <el-table-column label="地址" align="center" prop="address" min-width="250" :show-overflow-tooltip="true" />
      <el-table-column label="经度" align="center" prop="longitude" width="120" />
      <el-table-column label="纬度" align="center" prop="latitude" width="120" />
      <el-table-column label="类型" align="center" prop="clockinType" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.clockinType === '内勤' ? 'success' : 'warning'">
            {{ scope.row.clockinType || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="打卡时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mobile:clockin:remove']">删除</el-button>
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

    <!-- 详情对话框 -->
    <el-dialog title="打卡记录详情" v-model="detailOpen" width="500px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.userName }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{ detail.longitude }}</el-descriptions-item>
        <el-descriptions-item label="纬度">{{ detail.latitude }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="detail.clockinType === '内勤' ? 'success' : 'warning'">
            {{ detail.clockinType || '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="打卡时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="ClockinRecord">
import { useRouter } from 'vue-router';
import { listClockinRecord, getClockinRecord, delClockinRecord } from "@/api/mobile/clockin";

const { proxy } = getCurrentInstance();
const router = useRouter();

const clockinList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const multiple = ref(true);
const total = ref(0);
const detailOpen = ref(false);
const detail = ref({});

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userName: undefined,
    deptName: undefined
  }
});

const { queryParams } = toRefs(data);
const dateRange = ref([]);

function getList() {
  loading.value = true;
  listClockinRecord(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    clockinList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  dateRange.value = [];
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  multiple.value = !selection.length;
}

function handleDetail(row) {
  getClockinRecord(row.id).then(response => {
    detail.value = response.data;
    detailOpen.value = true;
  });
}

function handleDelete(row) {
  const recordIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除打卡记录编号为"' + recordIds + '"的数据项？').then(function () {
    return delClockinRecord(recordIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mobile/clockin/admin/export', {
    ...queryParams.value
  }, `打卡记录_${proxy.parseTime(new Date(), '{y}-{m}-{d}_{h}-{i}-{s}')}.xlsx`)
}

/** 跳转打卡统计页面 */
function goStatistics() {
  router.push('/mobile/clockin-statistics');
}

getList();
</script>
