<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="文件名称" prop="fileName">
        <el-input
          v-model="queryParams.fileName"
          placeholder="请输入文件名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="上传时间" prop="uploadTime">
        <el-date-picker clearable
          v-model="queryParams.uploadTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择上传时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="分类ID" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类ID" clearable>
          <el-option
            v-for="dict in category"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="上级id" prop="pid">
        <el-input
          v-model="queryParams.pid"
          placeholder="请输入上级id"
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
          v-hasPermi="['main:document:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Sort"
          @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="documentList"
      row-key="id"
      :default-expand-all="isExpandAll"
      :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
    >
      <el-table-column label="文件名称" prop="fileName" />
      <el-table-column label="文件类型" align="center" prop="fileType" />
      <el-table-column label="上传时间" align="center" prop="uploadTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.uploadTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="文件描述" align="center" prop="description" />
      <el-table-column label="分类ID" align="center" prop="categoryId">
        <template #default="scope">
          <dict-tag :options="category" :value="scope.row.categoryId"/>
        </template>
      </el-table-column>
      <el-table-column label="上级id" align="center" prop="pid" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="view" @click="handleFilePreview(scope.row)" v-hasPermi="['main:document:download']">查看</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['main:document:edit']">修改</el-button>
          <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['main:document:add']">新增</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['main:document:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改资料库管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="documentRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="资料名称" prop="fileName">
          <el-input v-model="form.fileName" placeholder="请输入文件名称" />
        </el-form-item>
        <el-form-item label="上传文件" prop="filePath">
          <file-upload v-model="form.filePath"/>
        </el-form-item>
        <el-form-item label="上传时间" prop="uploadTime">
          <el-date-picker clearable
            v-model="form.uploadTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择上传时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="文件描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="分类ID" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类ID">
            <el-option
              v-for="dict in category"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="上级id" prop="pid">
          <el-tree-select
            v-model="form.pid"
            :data="documentOptions"
            :props="{ value: 'id', label: 'fileName', children: 'children' }"
            value-key="id"
            placeholder="请选择上级id"
            check-strictly
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 预览附件 -->
    <el-dialog :title="title" v-model="filePreview" width="500px" append-to-body>
      <el-form ref="documentRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="资料清单" prop="filePath">
          <FilePreview v-model="form.filePath"/>
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

<script setup name="Document">
import { listDocument, getDocument, delDocument, addDocument, updateDocument } from "@/api/main/document";
import FileUpload from "@/components/FileUpload/index.vue";

const { proxy } = getCurrentInstance();
const { category } = proxy.useDict('category');

const documentList = ref([]);
const documentOptions = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const isExpandAll = ref(true);
const refreshTable = ref(true);
const filePreview = ref(false);

const data = reactive({
  form: {},
  queryParams: {
    fileName: null,
    fileType: null,
    uploadTime: null,
    categoryId: null,
    pid: null
  },
  rules: {
    fileName: [
      { required: true, message: "文件名称不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询资料库管理列表 */
function getList() {
  loading.value = true;
  listDocument(queryParams.value).then(response => {
    documentList.value = proxy.handleTree(response.data, "id", "pid");
    loading.value = false;
  });
}

/** 查询资料库管理下拉树结构 */
function getTreeselect() {
  listDocument().then(response => {
    documentOptions.value = [];
    const data = { id: 0, fileName: '顶级节点', children: [] };
    data.children = proxy.handleTree(response.data, "id", "pid");
    documentOptions.value.push(data);
  });
}
	
// 取消按钮
function cancel() {
  open.value = false;
  filePreview.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    fileName: null,
    filePath: null,
    fileType: null,
    uploadTime: null,
    description: null,
    categoryId: null,
    uploaderId: null,
    pid: null
  };
  proxy.resetForm("documentRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 新增按钮操作 */
function handleAdd(row) {
  reset();
  getTreeselect();
  if (row != null && row.id) {
    form.value.pid = row.id;
  } else {
    form.value.pid = 0;
  }
  form.value.uploadTime = new Date().toISOString().slice(0, 10); // 设置上传时间为当前日期
  open.value = true;
  title.value = "添加资料库管理";
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}

/** 修改按钮操作 */
async function handleUpdate(row) {
  reset();
  await getTreeselect();
  if (row != null) {
    form.value.pid = row.pid;
  }
  getDocument(row.id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改资料库管理";
  });
}
/** 预览按钮操作 */
async function handleFilePreview(row) {
  reset();
  await getTreeselect();
  if (row != null) {
    form.value.pid = row.pid;
  }
  getDocument(row.id).then(response => {
    form.value = response.data;
    filePreview.value = true;
    title.value = "预览资料库";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["documentRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateDocument(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addDocument(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除资料库管理编号为"' + row.id + '"的数据项？').then(function() {
    return delDocument(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>
