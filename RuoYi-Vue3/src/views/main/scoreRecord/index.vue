<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生 ID" prop="studentId">
        <el-input
          v-model="queryParams.studentId"
          placeholder="请输入学生 ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="AI 评分" prop="score">
        <el-input
          v-model="queryParams.score"
          placeholder="请输入 AI 评分"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分析状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择分析状态" clearable style="width: 120px">
          <el-option
            v-for="dict in score_state"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
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
          v-hasPermi="['main:scoreRecord:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['main:scoreRecord:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['main:scoreRecord:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['main:scoreRecord:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="scoreRecordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键 ID" align="center" prop="id" />
      <el-table-column label="学生 ID" align="center" prop="studentId" />
      <el-table-column label="AI 评分" align="center" prop="score" />
      <el-table-column label="分析状态" align="center" prop="status" >
        <template #default="scope">
          <dict-tag :options="score_state" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="AI 评语" align="center" prop="feedback" :show-overflow-tooltip="true"  />
      <el-table-column label="分析详情" align="center" prop="analysisDetails" :show-overflow-tooltip="true"  />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button 
            v-if="scope.row.status === '0' || scope.row.status === '3'"
            link 
            type="success" 
            icon="CircleCheck" 
            @click="handleSubmit(scope.row)" 
            v-hasPermi="['main:scoreRecord:submit']"
          >提交分析</el-button>
          <el-button 
            v-if="scope.row.status === '1'"
            link 
            type="warning" 
            icon="Loading" 
            disabled
          >分析中</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['main:scoreRecord:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['main:scoreRecord:remove']">删除</el-button>
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

    <!-- 添加或修改成绩分析记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="scoreRecordRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="上传视频" prop="filePath">
          <file-upload v-model="form.filePath" :file-type="['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv']" :limit="1"/>
        </el-form-item>
        <el-form-item label="AI 评分" prop="score">
          <el-input v-model="form.score" placeholder="请输入 AI 评分" disabled />
        </el-form-item>
        <el-form-item label="分析状态" prop="status">
          <el-radio-group v-model="form.status" disabled>
            <el-radio
              v-for="dict in score_state"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="AI 评语" prop="feedback">
          <el-input v-model="form.feedback" type="textarea" placeholder="请输入内容" disabled />
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

<script setup name="ScoreRecord">
import { listScoreRecord, getScoreRecord, delScoreRecord, addScoreRecord, updateScoreRecord, submitForAnalysis } from "@/api/main/scoreRecord";

const { proxy } = getCurrentInstance();
const { score_state } = proxy.useDict('score_state');

const scoreRecordList = ref([]);
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
    studentId: null,
    score: null,
    status: null,
    feedback: null,
  },
  rules: {
    courseId: [
      { required: true, message: "课程 ID 不能为空", trigger: "blur" }
    ],
    studentId: [
      { required: true, message: "学生 ID 不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询成绩分析记录列表 */
function getList() {
  loading.value = true;
  listScoreRecord(queryParams.value).then(response => {
    scoreRecordList.value = response.rows;
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
    courseId: '001',
    studentId: null,
    filePath: null,
    fileName: null,
    fileSize: null,
    analysisType: '八段锦',
    score: null,
    status: '0',
    feedback: null,
    analysisDetails: null,
    taskId: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null,
    delFlag: null
  };
  proxy.resetForm("scoreRecordRef");
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
  open.value = true;
  title.value = "添加成绩分析记录";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getScoreRecord(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改成绩分析记录";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["scoreRecordRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateScoreRecord(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addScoreRecord(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除成绩分析记录编号为"' + _ids + '"的数据项？').then(function() {
    return delScoreRecord(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('main/scoreRecord/export', {
    ...queryParams.value
  }, `scoreRecord_${new Date().getTime()}.xlsx`)
}

/** 提交 AI 分析 */
function handleSubmit(row) {
  const data = {
    id: row.id,
    courseId: row.courseId,
    filePath: row.filePath,
    fileName: row.fileName,
    fileSize: row.fileSize,
    analysisType: row.analysisType || '八段锦'
  };
  
  proxy.$modal.confirm('确认提交该记录进行 AI 分析吗？').then(() => {
    return submitForAnalysis(data);
  }).then(() => {
    proxy.$modal.msgSuccess("提交成功，正在分析中，请稍后刷新查看结果");
    getList();
  }).catch(() => {});
}

getList();
</script>
