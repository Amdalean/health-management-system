<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="72px">
      <el-form-item label="学号" prop="studentNo">
        <el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="学生姓名" prop="studentName">
        <el-input v-model="queryParams.studentName" placeholder="请输入学生姓名" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="课程" prop="courseId">
        <el-select v-model="queryParams.courseId" placeholder="请选择课程" clearable filterable style="width: 200px">
          <el-option v-for="c in allCourses" :key="c.courseId" :label="c.courseName" :value="c.courseId" />
        </el-select>
      </el-form-item>
      <el-form-item label="AI 评分" prop="score">
        <el-input v-model="queryParams.score" placeholder="请输入 AI 评分" clearable style="width: 130px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分析状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择分析状态" clearable style="width: 130px">
          <el-option v-for="dict in score_state" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['main:scoreRecord:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['main:scoreRecord:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['main:scoreRecord:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['main:scoreRecord:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="scoreRecordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学号" align="center" prop="studentNo" width="110">
        <template #default="scope">
          <span v-if="scope.row.studentNo">{{ scope.row.studentNo }}</span>
          <el-tag v-else type="info" size="small">未关联</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="学生姓名" align="center" prop="studentName" width="110">
        <template #default="scope">
          <span v-if="scope.row.studentName">{{ scope.row.studentName }}</span>
          <el-tag v-else type="warning" size="small">ID:{{ scope.row.studentId }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="课程" align="center" prop="courseName" min-width="150" :show-overflow-tooltip="true">
        <template #default="scope">
          <span v-if="scope.row.courseName">{{ scope.row.courseName }}</span>
          <el-tag v-else type="warning" size="small">ID:{{ scope.row.courseId }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="AI 评分" align="center" prop="score" width="90" />
      <el-table-column label="分析状态" align="center" width="100">
        <template #default="scope">
          <dict-tag :options="score_state" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="AI 评语" align="center" prop="feedback" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="分析详情" align="center" prop="analysisDetails" :show-overflow-tooltip="true" min-width="120" />
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === '0' || scope.row.status === '3'"
            link
            type="success"
            icon="CircleCheck"
            @click="handleSubmit(scope.row)"
            v-hasPermi="['main:scoreRecord:submit']"
          >提交分析</el-button>
          <el-button v-if="scope.row.status === '1'" link type="warning" icon="Loading" disabled>分析中</el-button>
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

    <!-- 添加或修改成绩智能分析对话框 -->
    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="scoreRecordRef" :model="form" :rules="rules" label-width="90px">
        <el-alert
          title="学生与课程来自「教学管理」下的建档数据，且该学生必须已选修所选课程。"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        />
        <el-form-item label="学生" prop="studentId">
          <el-select
            v-model="form.studentId"
            placeholder="请选择学生（输入学号或姓名可搜索）"
            filterable
            clearable
            style="width: 100%"
            @change="handleStudentChange"
          >
            <el-option
              v-for="s in studentOptions"
              :key="s.studentId"
              :label="s.studentNo + ' ' + s.name + (s.grade ? '（' + s.grade + '）' : '')"
              :value="s.studentId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="课程" prop="courseId">
          <el-select
            v-model="form.courseId"
            :placeholder="form.studentId ? '请选择该学生已选课程' : '请先选择学生'"
            filterable
            clearable
            :disabled="!form.studentId"
            style="width: 100%"
          >
            <el-option
              v-for="c in formCourseOptions"
              :key="c.courseId"
              :label="c.courseName + (c.semester ? '（' + c.semester + '）' : '')"
              :value="c.courseId"
            />
          </el-select>
          <div v-if="form.studentId && formCourseOptions.length === 0" style="color: #e6a23c; font-size: 12px; line-height: 20px">
            该学生暂无已选课程，请先到「教学管理 → 课程管理 → 选课」为其选课。
          </div>
        </el-form-item>
        <el-form-item label="上传视频" prop="filePath">
          <file-upload v-model="form.filePath" :file-type="['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv']" :limit="1" />
        </el-form-item>
        <el-form-item label="分析类型" prop="analysisType">
          <el-input v-model="form.analysisType" placeholder="默认为八段锦" />
        </el-form-item>
        <el-form-item label="AI 评分" prop="score">
          <el-input v-model="form.score" placeholder="提交分析后由 AI 自动回填" disabled />
        </el-form-item>
        <el-form-item label="分析状态" prop="status">
          <el-radio-group v-model="form.status" disabled>
            <el-radio v-for="dict in score_state" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="AI 评语" prop="feedback">
          <el-input v-model="form.feedback" type="textarea" placeholder="AI 自动回填" disabled />
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
import { listScoreRecord, getScoreRecord, delScoreRecord, addScoreRecord, updateScoreRecord, submitForAnalysis, scoreRecordOptions, coursesByStudent } from "@/api/main/scoreRecord";

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

// 联动数据
const studentOptions = ref([]);
const allCourses = ref([]);
const formCourseOptions = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    studentNo: null,
    studentName: null,
    courseId: null,
    score: null,
    status: null
  },
  rules: {
    studentId: [{ required: true, message: "请选择学生", trigger: "change" }],
    courseId: [{ required: true, message: "请选择课程", trigger: "change" }],
    filePath: [{ required: true, message: "请上传视频文件", trigger: "change" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询成绩智能分析列表 */
function getList() {
  loading.value = true;
  return listScoreRecord(queryParams.value).then(response => {
    scoreRecordList.value = response.rows;
    total.value = response.total;
    loading.value = false;
    // 存在「分析中」的记录时自动轮询，无需手动刷新
    syncAnalysisPolling();
  });
}

/* ==================== AI 分析状态自动轮询 ==================== */
// add by CYQ 2026-11-15 说明: 提交分析后记录为 1(分析中)，后台异步打分完成后自动刷新列表
const ANALYSIS_POLL_INTERVAL = 5000;
let analysisPollTimer = null;

/** 静默刷新（不显示骨架/loading，避免轮询时页面闪烁） */
function refreshListSilently() {
  listScoreRecord(queryParams.value).then(response => {
    scoreRecordList.value = response.rows;
    total.value = response.total;
    syncAnalysisPolling();
  }).catch(() => {});
}

/** 根据当前列表是否存在「分析中」记录，启停轮询 */
function syncAnalysisPolling() {
  const hasRunning = (scoreRecordList.value || []).some(item => item.status === '1');
  if (hasRunning && !analysisPollTimer) {
    analysisPollTimer = setInterval(refreshListSilently, ANALYSIS_POLL_INTERVAL);
  } else if (!hasRunning && analysisPollTimer) {
    clearInterval(analysisPollTimer);
    analysisPollTimer = null;
  }
}

onUnmounted(() => {
  if (analysisPollTimer) {
    clearInterval(analysisPollTimer);
    analysisPollTimer = null;
  }
});

/** 加载联动下拉数据（学生 + 全部课程） */
function getOptions() {
  scoreRecordOptions().then(response => {
    studentOptions.value = response.students || [];
    allCourses.value = response.courses || [];
  });
}

/** 按学生加载其已选课程；keepCourseId 用于编辑时保留原课程 */
function loadCourseOptions(studentId, keepCourseId) {
  if (!studentId) {
    formCourseOptions.value = [];
    return;
  }
  coursesByStudent(studentId).then(response => {
    let list = response.data || [];
    // 历史数据可能未被选课关系覆盖，编辑时兜底保留原课程，避免下拉框显示为空
    if (keepCourseId && !list.some(c => c.courseId === keepCourseId)) {
      const fallback = allCourses.value.find(c => c.courseId === keepCourseId);
      if (fallback) {
        list = [fallback, ...list];
      }
    }
    formCourseOptions.value = list;
  });
}

/** 学生变更 → 联动过滤课程 */
function handleStudentChange(studentId) {
  form.value.courseId = null;
  loadCourseOptions(studentId, null);
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
    courseId: null,
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
    remark: null
  };
  formCourseOptions.value = [];
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
  title.value = "添加成绩智能分析";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value[0];
  getScoreRecord(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改成绩智能分析";
    if (form.value.studentId) {
      loadCourseOptions(form.value.studentId, form.value.courseId);
    }
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["scoreRecordRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateScoreRecord(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addScoreRecord(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除成绩智能分析编号为"' + _ids + '"的数据项？').then(function () {
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
  }, `scoreRecord_${new Date().getTime()}.xlsx`);
}

/** 提交 AI 分析 */
function handleSubmit(row) {
  const data = {
    id: row.id,
    courseId: row.courseId,
    studentId: row.studentId,
    filePath: row.filePath,
    fileName: row.fileName,
    fileSize: row.fileSize,
    analysisType: row.analysisType || '八段锦'
  };
  proxy.$modal.confirm('确认提交该记录进行 AI 分析吗？').then(() => {
    // 先本地置为「分析中」，避免提交后需要手动刷新才能看到状态变化
    row.status = '1';
    row.score = null;
    row.feedback = null;
    row.analysisDetails = null;
    syncAnalysisPolling();
    return submitForAnalysis(data);
  }).then(() => {
    proxy.$modal.msgSuccess("已提交，正在分析中，完成后自动刷新");
    getList();
  }).catch(() => {
    // 失败（含超时）时以服务端真实状态为准
    refreshListSilently();
  });
}

getOptions();
getList();
</script>
