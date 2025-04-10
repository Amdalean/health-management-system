<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="接口编码" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入接口编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="接口名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入接口名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="插件全类名" prop="runclass">
        <el-input
          v-model="queryParams.runclass"
          placeholder="请输入插件全类名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="发送方" prop="sender">
        <el-input
          v-model="queryParams.sender"
          placeholder="请输入发送方"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="接收方" prop="receive">
        <el-input
          v-model="queryParams.receive"
          placeholder="请输入接收方"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="备注" prop="memo">
        <el-input
          v-model="queryParams.memo"
          placeholder="请输入备注"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="启用状态" prop="enablestate">
        <el-input
          v-model="queryParams.enablestate"
          placeholder="请输入启用状态"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="日志级别" prop="loglevel">
        <el-input
          v-model="queryParams.loglevel"
          placeholder="请输入日志级别"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="启用警告通知" prop="warn">
        <el-input
          v-model="queryParams.warn"
          placeholder="请输入启用警告通知"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="加密级别" prop="enlevel">
        <el-input
          v-model="queryParams.enlevel"
          placeholder="请输入加密级别"
          clearable
          @keyup.enter="handleQuery"
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
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['main:itfdoc:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['main:itfdoc:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['main:itfdoc:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['main:itfdoc:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="itfdocList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="接口档案主键" align="center" prop="pkItfdoc" />
      <el-table-column label="接口编码" align="center" prop="code" />
      <el-table-column label="接口名称" align="center" prop="name" />
      <el-table-column label="插件全类名" align="center" prop="runclass" />
      <el-table-column label="发送方" align="center" prop="sender" />
      <el-table-column label="接收方" align="center" prop="receive" />
      <el-table-column label="备注" align="center" prop="memo" />
      <el-table-column label="启用状态" align="center" prop="enablestate" />
      <el-table-column label="日志级别" align="center" prop="loglevel" />
      <el-table-column label="启用警告通知" align="center" prop="warn" />
      <el-table-column label="加密级别" align="center" prop="enlevel" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['main:itfdoc:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['main:itfdoc:remove']">删除</el-button>
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

    <!-- 添加或修改接口档案对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="itfdocRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="接口编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入接口编码" />
        </el-form-item>
        <el-form-item label="接口名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入接口名称" />
        </el-form-item>
        <el-form-item label="插件全类名" prop="runclass">
          <el-input v-model="form.runclass" placeholder="请输入插件全类名" />
        </el-form-item>
        <el-form-item label="发送方" prop="sender">
          <el-input v-model="form.sender" placeholder="请输入发送方" />
        </el-form-item>
        <el-form-item label="接收方" prop="receive">
          <el-input v-model="form.receive" placeholder="请输入接收方" />
        </el-form-item>
        <el-form-item label="备注" prop="memo">
          <el-input v-model="form.memo" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="启用状态" prop="enablestate">
          <el-input v-model="form.enablestate" placeholder="请输入启用状态" />
        </el-form-item>
        <el-form-item label="日志级别" prop="loglevel">
          <el-input v-model="form.loglevel" placeholder="请输入日志级别" />
        </el-form-item>
        <el-form-item label="启用警告通知" prop="warn">
          <el-input v-model="form.warn" placeholder="请输入启用警告通知" />
        </el-form-item>
        <el-form-item label="加密级别" prop="enlevel">
          <el-input v-model="form.enlevel" placeholder="请输入加密级别" />
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

<script setup name="Itfdoc">
import { listItfdoc, getItfdoc, delItfdoc, addItfdoc, updateItfdoc } from "@/api/main/itfdoc";

const { proxy } = getCurrentInstance();

const itfdocList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    code: null,
    name: null,
    runclass: null,
    sender: null,
    receive: null,
    memo: null,
    enablestate: null,
    loglevel: null,
    warn: null,
    enlevel: null
  },
  rules: {
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询接口档案列表 */
function getList() {
  loading.value = true;
  listItfdoc(queryParams.value).then(response => {
    itfdocList.value = response.rows;
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
    pkItfdoc: null,
    code: null,
    name: null,
    runclass: null,
    sender: null,
    receive: null,
    memo: null,
    enablestate: null,
    loglevel: null,
    warn: null,
    enlevel: null
  };
  proxy.resetForm("itfdocRef");
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
  ids.value = selection.map(item => item.pkItfdoc);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加接口档案";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _pkItfdoc = row.pkItfdoc || ids.value
  getItfdoc(_pkItfdoc).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改接口档案";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["itfdocRef"].validate(valid => {
    if (valid) {
      if (form.value.pkItfdoc != null) {
        updateItfdoc(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addItfdoc(form.value).then(response => {
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
  const _pkItfdocs = row.pkItfdoc || ids.value;
  proxy.$modal.confirm('是否确认删除接口档案编号为"' + _pkItfdocs + '"的数据项？').then(function() {
    return delItfdoc(_pkItfdocs);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('main/itfdoc/export', {
    ...queryParams.value
  }, `itfdoc_${new Date().getTime()}.xlsx`)
}

getList();
</script>
