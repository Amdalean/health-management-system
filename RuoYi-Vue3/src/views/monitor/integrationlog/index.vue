<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
         <el-form-item label="集成节点" prop="title">
            <el-input v-model="queryParams.title" placeholder="请输入集成节点" clearable style="width: 200px" @keyup.enter="handleQuery" />
         </el-form-item>
         <el-form-item label="业务类型" prop="bizType">
            <el-input v-model="queryParams.bizType" placeholder="如 EMAIL" clearable style="width: 200px" @keyup.enter="handleQuery" />
         </el-form-item>
         <el-form-item label="发送方" prop="sender">
            <el-input v-model="queryParams.sender" placeholder="请输入发送方" clearable style="width: 200px" @keyup.enter="handleQuery" />
         </el-form-item>
         <el-form-item label="接收方" prop="receiver">
            <el-input v-model="queryParams.receiver" placeholder="请输入接收方" clearable style="width: 200px" @keyup.enter="handleQuery" />
         </el-form-item>
         <el-form-item label="服务器标识" prop="serverId">
            <el-input v-model="queryParams.serverId" placeholder="请输入服务器标识" clearable style="width: 200px" @keyup.enter="handleQuery" />
         </el-form-item>
         <el-form-item label="单据标识" prop="billId">
            <el-input v-model="queryParams.billId" placeholder="请输入单据标识" clearable style="width: 200px" @keyup.enter="handleQuery" />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 200px">
               <el-option v-for="dict in sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
         </el-form-item>
         <el-form-item label="开始时间" style="width: 308px">
            <el-date-picker
               v-model="dateRange"
               value-format="YYYY-MM-DD HH:mm:ss"
               type="daterange"
               range-separator="-"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
               :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            ></el-date-picker>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['monitor:integrationlog:remove']">删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" @click="handleClean" v-hasPermi="['monitor:integrationlog:remove']">清空</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['monitor:integrationlog:export']">导出</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="日志编号" align="center" prop="logId" width="90" />
         <el-table-column label="集成节点" align="center" prop="title" :show-overflow-tooltip="true" />
         <el-table-column label="业务类型" align="center" prop="bizType" width="100" />
         <el-table-column label="发送方" align="center" prop="sender" :show-overflow-tooltip="true" width="120" />
         <el-table-column label="接收方" align="center" prop="receiver" :show-overflow-tooltip="true" />
         <el-table-column label="服务器标识" align="center" prop="serverId" :show-overflow-tooltip="true" width="130" />
         <el-table-column label="单据标识" align="center" prop="billId" :show-overflow-tooltip="true" />
         <el-table-column label="状态" align="center" prop="status" width="90">
            <template #default="scope">
               <dict-tag :options="sys_common_status" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="开始时间" align="center" prop="startTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.startTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="耗时" align="center" prop="costTime" width="100">
            <template #default="scope">
               <span>{{ scope.row.costTime }}毫秒</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="90" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['monitor:integrationlog:query']">详细</el-button>
            </template>
         </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

      <!-- 集成日志详细 -->
      <el-dialog title="集成日志详细" v-model="open" width="900px" append-to-body>
         <el-form :model="form" label-width="100px">
            <el-row>
               <el-col :span="12"><el-form-item label="集成节点：">{{ form.title }}</el-form-item></el-col>
               <el-col :span="12"><el-form-item label="业务类型：">{{ form.bizType }}</el-form-item></el-col>
               <el-col :span="12"><el-form-item label="发送方：">{{ form.sender }}</el-form-item></el-col>
               <el-col :span="12"><el-form-item label="接收方：">{{ form.receiver }}</el-form-item></el-col>
               <el-col :span="12"><el-form-item label="服务器标识：">{{ form.serverId }}</el-form-item></el-col>
               <el-col :span="12"><el-form-item label="单据标识：">{{ form.billId }}</el-form-item></el-col>
               <el-col :span="24"><el-form-item label="执行方法：">{{ form.method }}</el-form-item></el-col>
               <el-col :span="8">
                  <el-form-item label="状态：">
                     <el-tag :type="form.status === 0 ? 'success' : 'danger'">{{ form.status === 0 ? '成功' : '失败' }}</el-tag>
                  </el-form-item>
               </el-col>
               <el-col :span="8"><el-form-item label="耗时：">{{ form.costTime }}毫秒</el-form-item></el-col>
               <el-col :span="8"><el-form-item label="操作人：">{{ form.operName }}</el-form-item></el-col>
               <el-col :span="12"><el-form-item label="开始时间：">{{ parseTime(form.startTime) }}</el-form-item></el-col>
               <el-col :span="12"><el-form-item label="结束时间：">{{ parseTime(form.endTime) }}</el-form-item></el-col>
               <el-col :span="24">
                  <el-form-item label="发送报文：">
                     <el-input type="textarea" :rows="6" :model-value="form.sendMessage" readonly />
                  </el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="响应报文：">
                     <el-input type="textarea" :rows="6" :model-value="form.responseMessage" readonly />
                  </el-form-item>
               </el-col>
               <el-col :span="24" v-if="form.status === 1">
                  <el-form-item label="异常信息：">
                     <el-input type="textarea" :rows="4" :model-value="form.errorMsg" readonly />
                  </el-form-item>
               </el-col>
            </el-row>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button @click="open = false">关 闭</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="IntegrationLog">
import { listIntegrationLog, getIntegrationLog, delIntegrationLog, cleanIntegrationLog } from "@/api/monitor/integrationlog";

const { proxy } = getCurrentInstance();
const { sys_common_status } = proxy.useDict("sys_common_status");

const logList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const multiple = ref(true);
const total = ref(0);
const dateRange = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
    bizType: undefined,
    sender: undefined,
    receiver: undefined,
    serverId: undefined,
    billId: undefined,
    status: undefined
  }
});

const { queryParams, form } = toRefs(data);

/** 查询集成日志列表 */
function getList() {
  loading.value = true;
  listIntegrationLog(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    logList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  queryParams.value.pageNum = 1;
  getList();
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.logId);
  multiple.value = !selection.length;
}

/** 详细按钮操作 */
function handleView(row) {
  getIntegrationLog(row.logId).then(response => {
    form.value = response.data;
    open.value = true;
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const logIds = row.logId || ids.value;
  proxy.$modal.confirm('是否确认删除日志编号为"' + logIds + '"的数据项?').then(function () {
    return delIntegrationLog(logIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 清空按钮操作 */
function handleClean() {
  proxy.$modal.confirm("是否确认清空所有集成日志数据项?").then(function () {
    return cleanIntegrationLog();
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("清空成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("monitor/integrationlog/export", {
    ...queryParams.value
  }, `integration_log_${new Date().getTime()}.xlsx`);
}

getList();
</script>
