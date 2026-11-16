<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="72px">
      <el-form-item label="课程名称" prop="courseName">
        <el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="授课教师" prop="teacherId">
        <el-select v-model="queryParams.teacherId" placeholder="请选择授课教师" clearable filterable style="width: 180px">
          <el-option v-for="t in teacherOptions" :key="t.teacherId" :label="t.name" :value="t.teacherId" />
        </el-select>
      </el-form-item>
      <el-form-item label="学期" prop="semester">
        <el-input v-model="queryParams.semester" placeholder="如 2026-2027-1" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['main:course:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['main:course:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['main:course:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['main:course:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="courseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="课程名称" align="center" prop="courseName" :show-overflow-tooltip="true" min-width="160" />
      <el-table-column label="授课教师" align="center" prop="teacherName" width="110" />
      <el-table-column label="学分" align="center" prop="credit" width="80" />
      <el-table-column label="学时" align="center" prop="hours" width="80" />
      <el-table-column label="学期" align="center" prop="semester" width="130" />
      <el-table-column label="选课人数" align="center" prop="studentCount" width="100" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="250" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['main:course:edit']">修改</el-button>
          <el-button link type="success" icon="User" @click="openAssign(scope.row)" v-hasPermi="['main:course:assign']">选课</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['main:course:remove']">删除</el-button>
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

    <!-- 添加或修改课程对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="courseRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="授课教师" prop="teacherId">
              <el-select v-model="form.teacherId" placeholder="请选择授课教师" clearable filterable style="width: 100%">
                <el-option v-for="t in teacherOptions" :key="t.teacherId" :label="t.name + (t.title ? '（' + t.title + '）' : '')" :value="t.teacherId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学期" prop="semester">
              <el-input v-model="form.semester" placeholder="如 2026-2027-1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="学分" prop="credit">
              <el-input-number v-model="form.credit" :min="0" :max="20" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学时" prop="hours">
              <el-input-number v-model="form.hours" :min="0" :max="200" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
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

    <!-- 选课管理对话框 -->
    <el-dialog :title="assignTitle" v-model="assignOpen" width="760px" append-to-body>
      <el-alert
        title="左侧为尚未选课的学生，右侧为已选学生；在两侧之间移动后点击「保存」生效。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <div v-loading="assignLoading" style="display: flex; justify-content: center">
        <el-transfer
          v-model="assignKeys"
          :data="assignData"
          :titles="['未选学生', '已选学生']"
          filterable
          filter-placeholder="输入学号或姓名搜索"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAssign">保 存</el-button>
          <el-button @click="assignOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Course">
import { listCourse, getCourse, delCourse, addCourse, updateCourse, listCourseStudents, listCandidates, assignStudents, removeStudents } from "@/api/main/course";
import { listTeacher } from "@/api/main/teacher";

const { proxy } = getCurrentInstance();

const courseList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const teacherOptions = ref([]);

// 选课管理
const assignOpen = ref(false);
const assignLoading = ref(false);
const assignTitle = ref("");
const assignData = ref([]);
const assignKeys = ref([]);
const originalKeys = ref([]);
const assignCourse = ref({});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    courseName: null,
    teacherId: null,
    semester: null
  },
  rules: {
    courseName: [{ required: true, message: "课程名称不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询课程列表 */
function getList() {
  loading.value = true;
  listCourse(queryParams.value).then(response => {
    courseList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** 加载授课教师下拉 */
function getTeacherOptions() {
  listTeacher({ pageNum: 1, pageSize: 1000 }).then(response => {
    teacherOptions.value = response.rows || [];
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
    courseId: null,
    courseName: null,
    teacherId: null,
    credit: null,
    hours: null,
    semester: null,
    remark: null
  };
  proxy.resetForm("courseRef");
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
  ids.value = selection.map(item => item.courseId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加课程";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _courseId = row.courseId || ids.value[0];
  getCourse(_courseId).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改课程";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["courseRef"].validate(valid => {
    if (valid) {
      if (form.value.courseId != null) {
        updateCourse(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addCourse(form.value).then(() => {
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
  const _courseIds = row.courseId || ids.value;
  proxy.$modal.confirm('是否确认删除课程编号为"' + _courseIds + '"的数据项？\n删除后该课程的选课记录也会一并清除。').then(function () {
    return delCourse(_courseIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('main/course/export', {
    ...queryParams.value
  }, `course_${new Date().getTime()}.xlsx`);
}

/** 打开选课管理 */
function openAssign(row) {
  assignCourse.value = row;
  assignTitle.value = "选课管理 - " + row.courseName;
  assignData.value = [];
  assignKeys.value = [];
  originalKeys.value = [];
  assignLoading.value = true;
  assignOpen.value = true;
  Promise.all([listCourseStudents(row.courseId), listCandidates(row.courseId)]).then(([selRes, candRes]) => {
    const selected = selRes.data || [];
    const candidates = candRes.data || [];
    originalKeys.value = selected.map(s => s.studentId);
    assignKeys.value = [...originalKeys.value];
    assignData.value = [
      ...selected.map(s => ({ key: s.studentId, label: s.studentNo + " " + s.studentName + (s.grade ? "（" + s.grade + "）" : "") })),
      ...candidates.map(s => ({ key: s.studentId, label: s.studentNo + " " + s.name + (s.grade ? "（" + s.grade + "）" : "") }))
    ];
    assignLoading.value = false;
  }).catch(() => {
    assignLoading.value = false;
  });
}

/** 保存选课结果 */
function submitAssign() {
  const now = assignKeys.value || [];
  const added = now.filter(k => !originalKeys.value.includes(k));
  const removed = originalKeys.value.filter(k => !now.includes(k));
  if (added.length === 0 && removed.length === 0) {
    proxy.$modal.msgSuccess("选课信息未变更");
    assignOpen.value = false;
    return;
  }
  const tasks = [];
  if (added.length > 0) {
    tasks.push(assignStudents(assignCourse.value.courseId, added));
  }
  if (removed.length > 0) {
    tasks.push(removeStudents(assignCourse.value.courseId, removed));
  }
  Promise.all(tasks).then(() => {
    proxy.$modal.msgSuccess("选课信息已更新（新增 " + added.length + " 人，移除 " + removed.length + " 人）");
    assignOpen.value = false;
    getList();
  });
}

getTeacherOptions();
getList();
</script>
