<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="72px">
      <el-form-item label="学号" prop="studentNo">
        <el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入姓名" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="性别" prop="gender">
        <el-select v-model="queryParams.gender" placeholder="请选择性别" clearable style="width: 100px">
          <el-option v-for="dict in sys_user_sex" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="年级" prop="grade">
        <el-input v-model="queryParams.grade" placeholder="请输入年级" clearable style="width: 120px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="专业" prop="major">
        <el-input v-model="queryParams.major" placeholder="请输入专业" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="账号状态" prop="hasAccount">
        <el-select v-model="queryParams.hasAccount" placeholder="请选择" clearable style="width: 120px">
          <el-option label="未生成" :value="0" />
          <el-option label="已生成" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['main:student:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['main:student:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['main:student:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['main:student:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="User" :disabled="multiple" @click="handleBatchGenerate" v-hasPermi="['main:student:generateUser']">批量生成账号</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="studentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学号" align="center" prop="studentNo" width="110" />
      <el-table-column label="姓名" align="center" prop="name" width="100" />
      <el-table-column label="性别" align="center" prop="gender" width="70">
        <template #default="scope">
          <dict-tag :options="sys_user_sex" :value="scope.row.gender" />
        </template>
      </el-table-column>
      <el-table-column label="出生日期" align="center" prop="birthDate" width="110" />
      <el-table-column label="专业" align="center" prop="major" :show-overflow-tooltip="true" />
      <el-table-column label="年级" align="center" prop="grade" width="90" />
      <el-table-column label="联系电话" align="center" prop="phone" width="120" />
      <el-table-column label="入学时间" align="center" prop="enrollmentDate" width="110" />
      <el-table-column label="系统账号" align="center" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.userId" type="success">已生成</el-tag>
          <el-tag v-else type="info">未生成</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['main:student:edit']">修改</el-button>
          <el-button
            v-if="!scope.row.userId"
            link
            type="success"
            icon="User"
            @click="handleGenerate(scope.row)"
            v-hasPermi="['main:student:generateUser']"
          >生成账号</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['main:student:remove']">删除</el-button>
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

    <!-- 添加或修改学生建档对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="studentRef" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="form.studentNo" placeholder="请输入学号（生成账号时作为用户名）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择性别" style="width: 100%">
                <el-option v-for="dict in sys_user_sex" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择出生日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="专业" prop="major">
              <el-input v-model="form.major" placeholder="请输入专业" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级" prop="grade">
              <el-input v-model="form.grade" placeholder="请输入年级" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入学时间" prop="enrollmentDate">
              <el-date-picker v-model="form.enrollmentDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择入学时间" style="width: 100%" />
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

    <!-- 生成账号结果对话框 -->
    <el-dialog title="生成账号结果" v-model="resultOpen" width="620px" append-to-body>
      <el-alert :title="resultSummary" :type="resultFailure > 0 ? 'warning' : 'success'" :closable="false" show-icon />
      <el-scrollbar height="260px" style="margin-top: 12px">
        <div v-for="(item, index) in resultDetails" :key="index" style="line-height: 24px">
          <span :style="{ color: item.indexOf('【失败】') === 0 ? '#f56c6c' : '#67c23a' }">{{ item }}</span>
        </div>
      </el-scrollbar>
      <template #footer>
        <el-button type="primary" @click="resultOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Student">
import { listStudent, getStudent, delStudent, addStudent, updateStudent, generateStudentUser, batchGenerateStudentUser } from "@/api/main/student";

const { proxy } = getCurrentInstance();
const { sys_user_sex } = proxy.useDict('sys_user_sex');

const studentList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const studentNos = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const resultOpen = ref(false);
const resultSummary = ref("");
const resultDetails = ref([]);
const resultFailure = ref(0);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    studentNo: null,
    name: null,
    gender: null,
    grade: null,
    major: null,
    hasAccount: null
  },
  rules: {
    studentNo: [{ required: true, message: "学号不能为空", trigger: "blur" }],
    name: [{ required: true, message: "姓名不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询学生建档列表 */
function getList() {
  loading.value = true;
  listStudent(queryParams.value).then(response => {
    studentList.value = response.rows;
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
    studentId: null,
    studentNo: null,
    name: null,
    gender: null,
    birthDate: null,
    major: null,
    grade: null,
    phone: null,
    enrollmentDate: null,
    userId: null,
    remark: null
  };
  proxy.resetForm("studentRef");
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
  ids.value = selection.map(item => item.studentId);
  studentNos.value = selection.map(item => item.studentNo);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加学生建档";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _studentId = row.studentId || ids.value[0];
  getStudent(_studentId).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改学生建档";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["studentRef"].validate(valid => {
    if (valid) {
      if (form.value.studentId != null) {
        updateStudent(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addStudent(form.value).then(() => {
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
  const _studentIds = row.studentId || ids.value;
  proxy.$modal.confirm('是否确认删除学生编号为"' + _studentIds + '"的数据项？').then(function () {
    return delStudent(_studentIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('main/student/export', {
    ...queryParams.value
  }, `student_${new Date().getTime()}.xlsx`);
}

/** 生成账号（单条） */
function handleGenerate(row) {
  proxy.$modal.confirm('确认为学生【' + row.name + '】生成系统账号吗？\n用户名：' + row.studentNo + '\n初始密码：系统配置 sys.user.initPassword').then(() => {
    return generateStudentUser(row.studentId);
  }).then(() => {
    proxy.$modal.msgSuccess("账号生成成功");
    getList();
  }).catch(() => {});
}

/** 批量生成账号 */
function handleBatchGenerate() {
  proxy.$modal.confirm('确认为选中的 ' + ids.value.length + ' 名学生批量生成系统账号吗？\n用户名 = 学号，已生成过账号的记录会被自动跳过。').then(() => {
    return batchGenerateStudentUser(ids.value);
  }).then(response => {
    const result = response.data || {};
    resultSummary.value = result.summary || "处理完成";
    resultDetails.value = result.details || [];
    resultFailure.value = result.failure || 0;
    resultOpen.value = true;
    getList();
  }).catch(() => {});
}

getList();
</script>
