<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="提醒标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入提醒标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="历法类型" prop="calendarType">
        <el-select v-model="queryParams.calendarType" placeholder="请选择历法类型" clearable style="width: 120px">
          <el-option
            v-for="dict in sys_calendar_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="提醒日期" prop="remindDate">
        <el-date-picker
          v-if="queryParams.calendarType === '0'"
          v-model="queryParams.remindDate"
          type="date"
          value-format="MM-DD"
          format="MM-DD"
          placeholder="选择日期"
          clearable
          style="width: 130px"
        />
        <el-select
          v-else-if="queryParams.calendarType === '1'"
          v-model="queryParams.remindDate"
          placeholder="选择月份"
          clearable
          style="width: 130px"
        >
          <el-option v-for="item in lunarMonths" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-else
          v-model="queryParams.remindDate"
          placeholder="请输入提醒日期"
          clearable
          style="width: 130px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="提醒邮箱" prop="remindEmail">
        <el-input
          v-model="queryParams.remindEmail"
          placeholder="请输入提醒邮箱"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
          <el-option
            v-for="dict in enable_state"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
<!--      <el-form-item label="创建者" prop="createBy">-->
<!--        <el-input-->
<!--          v-model="queryParams.createBy"-->
<!--          placeholder="请输入创建者"-->
<!--          clearable-->
<!--          readonly-->
<!--          @keyup.enter="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->

      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['main:reminder:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['main:reminder:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['main:reminder:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['main:reminder:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="reminderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
<!--      <el-table-column label="提醒ID" align="center" prop="id" />-->
      <el-table-column label="提醒标题" align="center" prop="title" />
      <el-table-column label="提醒日期" align="center" prop="remindDate" />
      <el-table-column label="历法类型" align="center" prop="calendarType">
        <template #default="scope">
          <dict-tag :options="sys_calendar_type" :value="scope.row.calendarType"/>
        </template>
      </el-table-column>
      <el-table-column label="提醒邮箱" align="center" prop="remindEmail" :show-overflow-tooltip="true"/>
      <el-table-column label="AI提示词" align="center" prop="aiPrompt" :show-overflow-tooltip="true" />
      <el-table-column label="倒计时(天)" align="center" prop="countdown" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="enable_state" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="创建者" align="center" prop="createByName" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新者" align="center" prop="updateByName" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['main:reminder:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['main:reminder:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改提醒管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="reminderRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="提醒标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入提醒标题" />
        </el-form-item>
        <el-form-item label="历法类型" prop="calendarType">
          <el-select v-model="form.calendarType" placeholder="请选择历法类型">
            <el-option
              v-for="dict in sys_calendar_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="提醒日期" prop="remindDate">
          <el-date-picker
            v-if="form.calendarType === '0'"
            v-model="form.remindDate"
            type="date"
            value-format="MM-DD"
            format="MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
          <template v-else-if="form.calendarType === '1'">
            <el-select v-model="lunarMonth" placeholder="选择月份" style="width: 120px; margin-right: 8px;">
              <el-option v-for="item in lunarMonths" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="lunarDay" placeholder="选择日期" style="width: 120px;">
              <el-option v-for="item in lunarDays" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
          <el-input v-else v-model="form.remindDate" placeholder="请先选择历法类型" />
        </el-form-item>
        <el-form-item label="提醒邮箱" prop="remindEmail">
          <el-input v-model="form.remindEmail" placeholder="请输入提醒邮箱" />
        </el-form-item>
        <el-form-item label="AI提示词" prop="aiPrompt">
          <el-input v-model="form.aiPrompt" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in enable_state"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Reminder">
import { watch } from 'vue'
import { listReminder, getReminder, delReminder, addReminder, updateReminder } from "@/api/main/reminder";
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance();
const { enable_state, sys_calendar_type } = proxy.useDict('enable_state', 'sys_calendar_type');

// 农历月份选项
const lunarMonths = [
  { value: '正月', label: '正月' },
  { value: '二月', label: '二月' },
  { value: '三月', label: '三月' },
  { value: '四月', label: '四月' },
  { value: '五月', label: '五月' },
  { value: '六月', label: '六月' },
  { value: '七月', label: '七月' },
  { value: '八月', label: '八月' },
  { value: '九月', label: '九月' },
  { value: '十月', label: '十月' },
  { value: '冬月', label: '冬月' },
  { value: '腊月', label: '腊月' },
]

// 农历日期选项
const lunarDayNames = [
  '初一','初二','初三','初四','初五','初六','初七','初八','初九','初十',
  '十一','十二','十三','十四','十五','十六','十七','十八','十九','二十',
  '廿一','廿二','廿三','廿四','廿五','廿六','廿七','廿八','廿九','三十',
]
const lunarDays = lunarDayNames.map(d => ({ value: d, label: d }))

const reminderList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const lunarMonth = ref(null);
const lunarDay = ref(null);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 20,
    title: null,
    remindDate: null,
    calendarType: null,
    remindEmail: null,
    aiPrompt: null,
    status: '1',
    createBy:null
  },
  rules: {
    title: [
      { required: true, message: "提醒标题不能为空", trigger: "blur" }
    ],
    remindDate: [
      { required: true, message: "提醒日期不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

// const userStore = useUserStore()
// const { id } = userStore
// data.queryParams.createBy = id===1?null:id;

/** 查询提醒管理列表 */
function getList() {
  loading.value = true;
  listReminder(queryParams.value).then(response => {
    reminderList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    title: null,
    remindDate: null,
    calendarType: null,
    remindEmail: null,
    aiPrompt: null,
    countdown:null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  lunarMonth.value = null;
  lunarDay.value = null;
  proxy.resetForm("reminderRef");
}

// 农历月日组合成 remindDate
watch([lunarMonth, lunarDay], ([month, day]) => {
  if (month && day) {
    form.value.remindDate = month + day
  } else if (!month && !day) {
    form.value.remindDate = null
  }
})

// 切换历法类型时清空提醒日期（排除初始加载场景）
watch(() => form.value.calendarType, (newType, oldType) => {
  if (oldType !== null && newType !== oldType) {
    form.value.remindDate = null
    lunarMonth.value = null
    lunarDay.value = null
  }
})

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加提醒管理";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getReminder(_id).then(response => {
    form.value = response.data;
    // 解析农历日期回填到下拉框
    if (form.value.calendarType === '1' && form.value.remindDate) {
      const monthNames = lunarMonths.map(m => m.value)
      for (const m of monthNames) {
        if (form.value.remindDate.startsWith(m)) {
          lunarMonth.value = m
          lunarDay.value = form.value.remindDate.substring(m.length)
          break
        }
      }
    }
    open.value = true;
    title.value = "修改提醒管理";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["reminderRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateReminder(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addReminder(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除提醒管理编号为"' + _ids + '"的数据项？').then(function() {
    return delReminder(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('main/reminder/export', {
    ...queryParams.value
  }, `reminder_${new Date().getTime()}.xlsx`)
}

getList();
</script>
