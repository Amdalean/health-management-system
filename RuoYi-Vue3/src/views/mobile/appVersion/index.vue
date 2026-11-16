<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="版本号" prop="version">
        <el-input
          v-model="queryParams.version"
          placeholder="请输入版本号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="平台" prop="platform">
        <el-select v-model="queryParams.platform" placeholder="请选择平台" clearable style="width: 150px">
          <el-option label="Android" value="android" />
          <el-option label="iOS" value="ios" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="禁用" :value="0" />
          <el-option label="启用" :value="1" />
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
          v-hasPermi="['mobile:appVersion:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mobile:appVersion:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mobile:appVersion:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="appVersionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="版本号" align="center" prop="version" width="100" />
      <el-table-column label="版本码" align="center" prop="versionCode" width="100" />
      <el-table-column label="平台" align="center" prop="platform" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.platform === 'android' ? 'success' : 'primary'">
            {{ scope.row.platform === 'android' ? 'Android' : 'iOS' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件名称" align="center" prop="fileName" :show-overflow-tooltip="true" />
      <el-table-column label="文件大小" align="center" prop="fileSize" width="120">
        <template #default="scope">
          {{ formatFileSize(scope.row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="强制更新" align="center" prop="forceUpdate" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.forceUpdate === 1 ? 'danger' : 'info'">
            {{ scope.row.forceUpdate === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新日志" align="center" prop="releaseNotes" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mobile:appVersion:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mobile:appVersion:remove']">删除</el-button>
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

    <!-- 添加或修改APP版本对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="请输入版本号, 如: 1.2.0" />
        </el-form-item>
        <el-form-item label="版本码" prop="versionCode">
          <el-input-number v-model="form.versionCode" :min="1" placeholder="请输入版本码" style="width: 100%" />
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-radio-group v-model="form.platform">
            <el-radio label="android">Android</el-radio>
            <el-radio label="ios">iOS</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="上传安装包" prop="filePath">
          <file-upload v-model="form.filePath" :file-size="500" :file-type="['apk', 'ipa']" :limit="1" @file-info="handleFileInfo" />
        </el-form-item>
        <el-form-item label="文件名称" prop="fileName">
          <el-input v-model="form.fileName" placeholder="请输入文件名称" />
        </el-form-item>
        <el-form-item label="强制更新" prop="forceUpdate">
          <el-radio-group v-model="form.forceUpdate">
            <el-radio :label="0">否</el-radio>
            <el-radio :label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">禁用</el-radio>
            <el-radio :label="1">启用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="更新日志" prop="releaseNotes">
          <el-input v-model="form.releaseNotes" type="textarea" :rows="4" placeholder="请输入更新日志" />
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

<script setup name="AppVersion">
import { listAppVersion, getAppVersion, delAppVersion, addAppVersion, updateAppVersion } from "@/api/mobile/appVersion";
import FileUpload from "@/components/FileUpload/index.vue";

const { proxy } = getCurrentInstance();

const appVersionList = ref([]);
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
    version: undefined,
    platform: undefined,
    status: undefined
  },
  rules: {
    version: [{ required: true, message: "版本号不能为空", trigger: "blur" }],
    versionCode: [{ required: true, message: "版本码不能为空", trigger: "blur" }],
    platform: [{ required: true, message: "平台不能为空", trigger: "change" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询APP版本列表 */
function getList() {
  loading.value = true;
  listAppVersion(queryParams.value).then(response => {
    appVersionList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** 格式化文件大小 */
function formatFileSize(size) {
  if (!size) return '-';
  if (size < 1024) return size + ' B';
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB';
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB';
  return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    version: undefined,
    versionCode: undefined,
    platform: 'android',
    fileName: undefined,
    filePath: undefined,
    fileSize: undefined,
    forceUpdate: 0,
    releaseNotes: undefined,
    status: 1
  };
  proxy.resetForm("formRef");
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

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加APP版本";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const id = row.id || ids.value;
  getAppVersion(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改APP版本";
  });
}

/** 文件上传成功回调 */
function handleFileInfo(fileInfo) {
  form.value.fileName = fileInfo.name;
  form.value.fileSize = fileInfo.size;
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      if (form.value.id !== undefined) {
        updateAppVersion(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addAppVersion(form.value).then(response => {
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
  const versionIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除版本编号为"' + versionIds + '"的数据项？').then(function () {
    return delAppVersion(versionIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>

<style scoped>
</style>
