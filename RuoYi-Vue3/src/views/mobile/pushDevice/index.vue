<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="客户端ID" prop="clientId">
        <el-input
          v-model="queryParams.clientId"
          placeholder="请输入客户端ID"
          clearable
          style="width: 220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="平台" prop="platform">
        <el-select v-model="queryParams.platform" placeholder="请选择平台" clearable style="width: 120px">
          <el-option label="Android" value="android" />
          <el-option label="iOS" value="ios" />
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
          icon="Promotion"
          @click="handlePush"
          v-hasPermi="['mobile:pushDevice:send']"
        >发送推送</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:push:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="pushDeviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="用户ID" align="center" prop="userId" width="100" />
      <el-table-column label="客户端ID" align="center" prop="clientId" :show-overflow-tooltip="true" min-width="200" />
      <el-table-column label="平台" align="center" prop="platform" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.platform === 'android' ? 'success' : 'primary'">
            {{ scope.row.platform === 'android' ? 'Android' : 'iOS' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="应用版本" align="center" prop="appVersion" width="120" />
      <el-table-column label="微信Openid" align="center" prop="wxOpenid" :show-overflow-tooltip="true" min-width="180" />
      <el-table-column label="注册时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:push:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:push:remove']">删除</el-button>
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

    <!-- 修改推送设备对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="客户端ID" prop="clientId">
          <el-input v-model="form.clientId" placeholder="请输入客户端ID" />
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-radio-group v-model="form.platform">
            <el-radio label="android">Android</el-radio>
            <el-radio label="ios">iOS</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="应用版本" prop="appVersion">
          <el-input v-model="form.appVersion" placeholder="请输入应用版本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 发送推送对话框 -->
    <el-dialog title="发送推送" v-model="pushOpen" width="550px" append-to-body>
      <el-form ref="pushFormRef" :model="pushForm" :rules="pushRules" label-width="100px">
        <el-form-item label="推送范围" prop="pushType">
          <el-radio-group v-model="pushForm.pushType">
            <el-radio label="all">全部设备</el-radio>
            <el-radio label="single">指定设备</el-radio>
            <el-radio label="user">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="pushForm.pushType === 'single'" label="客户端ID" prop="clientId">
          <el-input v-model="pushForm.clientId" placeholder="请输入设备的clientId" />
        </el-form-item>
        <el-form-item v-if="pushForm.pushType === 'user'" label="用户ID" prop="userId">
          <el-input v-model="pushForm.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="通知标题" prop="title">
          <el-input v-model="pushForm.title" placeholder="请输入推送标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="通知内容" prop="body">
          <el-input v-model="pushForm.body" type="textarea" :rows="4" placeholder="请输入推送内容" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="pushSending" @click="submitPush">发 送</el-button>
          <el-button @click="pushOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PushDevice">
import { listPushDevice, getPushDevice, delPushDevice, updatePushDevice, pushToSingle, pushToAll, pushToUser } from "@/api/mobile/pushDevice";

const { proxy } = getCurrentInstance();

const pushDeviceList = ref([]);
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
    clientId: undefined,
    platform: undefined
  },
  rules: {
    clientId: [{ required: true, message: "客户端ID不能为空", trigger: "blur" }],
    platform: [{ required: true, message: "平台不能为空", trigger: "change" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

// 推送相关
const pushOpen = ref(false);
const pushSending = ref(false);
const pushFormRef = ref(null);

const pushData = reactive({
  pushForm: {
    pushType: 'all',
    clientId: '',
    userId: '',
    title: '',
    body: ''
  },
  pushRules: {
    title: [{ required: true, message: '通知标题不能为空', trigger: 'blur' }],
    body: [{ required: true, message: '通知内容不能为空', trigger: 'blur' }]
  }
});

const { pushForm, pushRules } = toRefs(pushData);

/** 打开发送推送对话框 */
function handlePush() {
  pushForm.value = {
    pushType: 'all',
    clientId: '',
    userId: '',
    title: '',
    body: ''
  };
  pushOpen.value = true;
}

/** 发送推送 */
function submitPush() {
  proxy.$refs['pushFormRef'].validate(valid => {
    if (valid) {
      pushSending.value = true;
      const data = { title: pushForm.value.title, body: pushForm.value.body };
      let request;

      if (pushForm.value.pushType === 'single') {
        data.clientId = pushForm.value.clientId;
        request = pushToSingle(data);
      } else if (pushForm.value.pushType === 'user') {
        data.userId = pushForm.value.userId;
        request = pushToUser(data);
      } else {
        request = pushToAll(data);
      }

      request.then(response => {
        proxy.$modal.msgSuccess('推送已发送');
        pushSending.value = false;
        pushOpen.value = false;
      }).catch(() => {
        pushSending.value = false;
        proxy.$modal.msgError('推送发送失败');
      });
    }
  });
}

/** 查询推送设备列表 */
function getList() {
  loading.value = true;
  listPushDevice(queryParams.value).then(response => {
    pushDeviceList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
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
    clientId: undefined,
    platform: undefined,
    appVersion: undefined
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

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const id = row.id || ids.value;
  getPushDevice(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改推送设备";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      updatePushDevice(form.value).then(response => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const deviceIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除设备编号为"' + deviceIds + '"的数据项？').then(function () {
    return delPushDevice(deviceIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>

<style scoped>
</style>
