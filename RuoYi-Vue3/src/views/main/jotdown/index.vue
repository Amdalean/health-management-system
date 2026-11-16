<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="说明" prop="description">
        <el-input
          v-model="queryParams.description"
          placeholder="请输入说明"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="金额" prop="amount">
        <el-input
          v-model="queryParams.amount"
          placeholder="请输入金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="日期" prop="date">
        <el-date-picker clearable
          v-model="queryParams.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="费用类型" prop="expenseType">
        <el-select v-model="queryParams.expenseType" placeholder="请选择费用类型" clearable style="width: 200px">
          <el-option
              v-for="dict in jotdown_expense_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          ></el-option>
        </el-select>
      </el-form-item>
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
          v-hasPermi="['main:jotdown:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['main:jotdown:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['main:jotdown:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['main:jotdown:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="jotdownList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="说明" align="center" prop="description" />
      <el-table-column label="金额" align="center" prop="amount" />
      <el-table-column label="日期" align="center" prop="date" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.date, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="费用类型" align="center" prop="expenseType">
        <template #default="scope">
          <dict-tag :options="jotdown_expense_type" :value="scope.row.expenseType"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remarks" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['main:jotdown:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['main:jotdown:remove']">删除</el-button>
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

    <!-- 添加或修改随手记对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="jotdownRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item>
          <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="handleFileChange" />
          <el-button type="warning" plain :loading="aiLoading" @click="openFileInput">
            图片识别录入
          </el-button>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" placeholder="请输入说明" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="form.amount" placeholder="请输入金额" />
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker clearable
            v-model="form.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="费用类型" prop="expenseType">
          <el-select v-model="form.expenseType" placeholder="请选择费用类型">
            <el-option
                v-for="dict in jotdown_expense_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remarks">
          <el-input v-model="form.remarks" type="textarea" placeholder="请输入内容" />
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

<script setup name="Jotdown">
import { listJotdown, getJotdown, delJotdown, addJotdown, updateJotdown, aiRecognizeJotdown } from "@/api/main/jotdown";
import useUserStore from '@/store/modules/user'

const { proxy } = getCurrentInstance();
const { jotdown_expense_type } = proxy.useDict('jotdown_expense_type');
const userStore = useUserStore();

const jotdownList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const fileInputRef = ref(null);
const aiLoading = ref(false);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    description: null,
    amount: null,
    date: null,
    expenseType: null,
  },
  rules: {
    description: [
      { required: true, message: "说明不能为空", trigger: "blur" }
    ],
    amount: [
      { required: true, message: "金额不能为空", trigger: "blur" }
    ],
    date: [
      { required: true, message: "日期不能为空", trigger: "blur" }
    ],
    expenseType: [
      { required: true, message: "费用类型不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询随手记列表 */
function getList() {
  loading.value = true;
  listJotdown(queryParams.value).then(response => {
    jotdownList.value = response.rows;
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
    personnel: null,
    description: null,
    amount: null,
    date: null,
    expenseType: null,
    remarks: null,
  };
  proxy.resetForm("jotdownRef");
}

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
  form.value.personnel = userStore.id;
  open.value = true;
  title.value = "添加随手记";
}

/** 打开文件选择器 */
function openFileInput() {
  fileInputRef.value.click();
}

/** 图片上传并AI识别 */
async function handleFileChange(e) {
  const file = e.target.files[0];
  if (!file) return;
  aiLoading.value = true;
  try {
    const res = await aiRecognizeJotdown(file);
    const data = res.data;
    if (data.description) form.value.description = data.description;
    if (data.amount) form.value.amount = data.amount;
    if (data.date) form.value.date = data.date;
    if (data.expenseType) form.value.expenseType = String(data.expenseType);
    if (data.remarks) form.value.remarks = data.remarks;
    proxy.$modal.msgSuccess("AI识别完成，请核对后提交");
  } catch (err) {
    proxy.$modal.msgError("AI识别失败，请重试");
  } finally {
    aiLoading.value = false;
    fileInputRef.value.value = '';
  }
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getJotdown(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改随手记";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["jotdownRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateJotdown(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addJotdown(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除随手记编号为"' + _ids + '"的数据项？').then(function() {
    return delJotdown(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('main/jotdown/export', {
    ...queryParams.value
  }, `jotdown_${new Date().getTime()}.xlsx`)
}

getList();
</script>
