<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="年度" prop="year">
        <el-select v-model="queryParams.year" placeholder="请选择年度" clearable>
          <el-option
              v-for="dict in year"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="月份" prop="month">
        <el-select v-model="queryParams.month" placeholder="请选择月份" clearable>
          <el-option
              v-for="dict in month"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="收入" prop="income">
        <el-input
            v-model="queryParams.income"
            placeholder="请输入收入"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支出" prop="expense">
        <el-input
            v-model="queryParams.expense"
            placeholder="请输入支出"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="结余" prop="balance">
        <el-input
            v-model="queryParams.balance"
            placeholder="请输入结余"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="月初存款" prop="startDeposit">
        <el-input
            v-model="queryParams.startDeposit"
            placeholder="请输入月初存款"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="月底存款" prop="endDeposit">
        <el-input
            v-model="queryParams.endDeposit"
            placeholder="请输入月底存款"
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
            v-hasPermi="['main:summary:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['main:summary:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['main:summary:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
            v-hasPermi="['main:summary:export']"
        >导出</el-button>
      </el-col>
      <!-- 财务报表弹窗 -->
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="Download"
            @click="forms"
            v-hasPermi="['main:summary:export']"
        >统计报表</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="summaryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="年度" align="center" prop="year">
        <template #default="scope">
          <dict-tag :options="year" :value="scope.row.year"/>
        </template>
      </el-table-column>
      <el-table-column label="月份" align="center" prop="month">
        <template #default="scope">
          <dict-tag :options="month" :value="scope.row.month"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="收入" align="center" prop="income" />
      <el-table-column label="支出" align="center" prop="expense" />
      <el-table-column label="结余" align="center" prop="balance" />
      <el-table-column label="月初存款" align="center" prop="startDeposit" />
      <el-table-column label="月底存款" align="center" prop="endDeposit" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['main:summary:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['main:summary:remove']">删除</el-button>
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

    <!-- 添加或修改财务汇总主对话框 -->
    <el-dialog :title="title" v-model="open" width="80%" append-to-body>
      <el-form ref="summaryRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="年度" prop="year">
          <el-select v-model="form.year" placeholder="请选择年度">
            <el-option
                v-for="dict in year"
                :key="dict.value"
                :label="dict.label"
                :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="月份" prop="month">
          <el-select v-model="form.month" placeholder="请选择月份">
            <el-option
                v-for="dict in month"
                :key="dict.value"
                :label="dict.label"
                :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="收入" prop="income">
          <el-input v-model="form.income" placeholder="请输入收入" />
        </el-form-item>
        <el-form-item label="支出" prop="expense">
          <el-input v-model="form.expense" placeholder="请输入支出" />
        </el-form-item>
        <el-form-item label="结余" prop="balance">
          <el-input v-model="form.balance" placeholder="请输入结余" disabled/>
        </el-form-item>
        <el-form-item label="月初存款" prop="startDeposit">
          <el-input v-model="form.startDeposit" placeholder="请输入月初存款" disabled />
        </el-form-item>
        <el-form-item label="月底存款" prop="endDeposit">
          <el-input v-model="form.endDeposit" placeholder="请输入月底存款" disabled />
        </el-form-item>
        <el-divider content-position="center">财务明细子表信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="Plus" @click="handleAddHsmDetail">添加</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="Delete" @click="handleDeleteHsmDetail">删除</el-button>
          </el-col>
        </el-row>
        <el-table :data="hsmDetailList" :row-class-name="rowHsmDetailIndex" @selection-change="handleHsmDetailSelectionChange" ref="hsmDetail">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" align="center" prop="index" width="50"/>

          <el-table-column label="名称" prop="name" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.name" placeholder="请输入名称" />
            </template>
          </el-table-column>

          <el-table-column label="所属类别" prop="category" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.category" placeholder="请选择所属类别">
                <el-option
                    v-for="dict in cktype"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                ></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="类型" prop="type" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.type" placeholder="请选择类型">
                <el-option
                    v-for="dict in sztype"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                ></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="金额" prop="amount" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.amount" placeholder="请输入金额" @input="afterEvent(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="变化量" prop="changeAmount" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.changeAmount" placeholder="请输入变化量" disabled/>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.remark" placeholder="请输入备注" />
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 在template的el-dialog之后添加统计报表弹窗 -->
    <el-dialog
        title="财务统计报表"
        v-model="showChartDialog"
        width="90%"
        top="5vh"
        destroy-on-close
    >
    
    <div ref="container" id="container"></div>
      <FinanceChart :data=chartData v-if="showChartDialog" :chart-data="chartData" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showChartDialog = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
 <!-- <G2Demo /> -->
</template>

<script setup name="Summary">
import {listSummary, getSummary, delSummary, addSummary, updateSummary, initSummary,formsSummary} from "@/api/main/summary";
// import FinanceChart from '@/components/FinanceChart';
import FinanceChart from '@/components/G2Demo';
import { Chart } from '@antv/g2'

const {proxy} = getCurrentInstance();
const {year, cktype, sztype, month} = proxy.useDict('year', 'cktype', 'sztype', 'month');

const summaryList = ref([]);
const hsmDetailList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const checkedHsmDetail = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
// 新增弹窗控制变量和图表组件
const showChartDialog = ref(false);
const chartData = ref({ rows: [] });


const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    year: null,
    month: null,
    income: null,
    expense: null,
    balance: null,
    startDeposit: null,
    endDeposit: null
  },
  rules: {
    year: [
      {required: true, message: "年度不能为空", trigger: "change"}
    ],
    month: [
      {required: true, message: "月份不能为空", trigger: "change"}
    ],
    createTime: [
      {required: true, message: "制单时间不能为空", trigger: "blur"}
    ],
    updateTime: [
      {required: true, message: "更新时间不能为空", trigger: "blur"}
    ],
    income: [
      {required: true, message: "收入不能为空", trigger: "blur"}
    ],
    expense: [
      {required: true, message: "支出不能为空", trigger: "blur"}
    ],
    balance: [
      {required: true, message: "结余不能为空", trigger: "blur"}
    ],
    startDeposit: [
      {required: true, message: "月初存款不能为空", trigger: "blur"}
    ],
    endDeposit: [
      {required: true, message: "月底存款不能为空", trigger: "blur"}
    ]
  }
});

const {queryParams, form, rules} = toRefs(data);

/** 查询财务汇总主列表 */
function getList() {
  loading.value = true;
  listSummary(queryParams.value).then(response => {
    summaryList.value = response.rows;
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
    year: null,
    month: null,
    remark: null,
    createTime: null,
    updateTime: null,
    income: 0,
    expense: 0,
    balance: 0,
    startDeposit: 0,
    endDeposit: 0
  };
  hsmDetailList.value = [];
  proxy.resetForm("summaryRef");
}
/**add by CYQ 2025年2月5日 增加默认赋值 */
function setDefault() {


  hsmDetailList.value =[
    {
      "summaryId": 9,
      "category": "1",
      "type": "1",
      "amount": 0,
      "changeAmount": 0,
      "index": 1
    },
    {
      "summaryId": 9,
      "category": "2",
      "type": "1",
      "amount": 0,
      "changeAmount": 0,
      "index": 2
    },
    {
      "summaryId": 9,
      "category": "7",
      "type": "1",
      "amount": 0,
      "changeAmount": null,
      "name": null,
      "index": 3
    },
    {
      "summaryId": 9,
      "category": "4",
      "type": "4",
      "amount": 0,
      "changeAmount": null,
      "name": null,
      "index": 4
    },
    {
      "summaryId": 9,
      "category": "8",
      "type": "3",
      "amount": 0,
      "changeAmount": null,
      "name": null,
      "index": 5
    },
    {
      "summaryId": 9,
      "category": "3",
      "type": "2",
      "amount": 0,
      "changeAmount": null,
      "name": null,
      "index": 6
    },
    {
      "summaryId": 9,
      "category": "5",
      "type": "5",
      "amount": 0,
      "changeAmount": null,
      "name": null,
      "index": 7
    },
    {
      "summaryId": 9,
      "category": "6",
      "type": "5",
      "amount": 0,
      "changeAmount": null,
      "name": null,
      "index": 8
    }
  ]
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
  addAfter();
  // setDefault();
  open.value = true;
  title.value = "添加财务汇总主表";
}
/** 新增前自动填充上月数据 */
function addAfter(){
  // const month = row.month
  initSummary().then(response=>{
    form.value.startDeposit = response.data.head.endDeposit;
    let date = new Date(response.data.head.year, response.data.head.month - 1);
    // 设置为下一个月的第一天
    date.setMonth(date.getMonth() + 1);
    // 获取年份和月份，getMonth()返回值需要加1以转换为实际月份
    let nextYear = date.getFullYear();
    let nextMonth = date.getMonth() + 1;

    form.value.year = nextYear;
    form.value.month = nextMonth;
    let items = response.data.items;
    items.forEach((detail, index) => {
      detail.index = index + 1;
      detail.summaryId = null; // 清空主表ID
      detail.id = null; // 清空ID
      detail.changeAmount = detail.amount * -1; // 设置变化量
      detail.changeAmountOld = detail.amount * -1; // 设置原始变化量
      detail.amount = 0; // 清空金额
    });
    hsmDetailList.value = items;
  })
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getSummary(_id).then(response => {
    form.value = response.data;
    hsmDetailList.value = response.data.hsmDetailList;
    open.value = true;
    title.value = "修改财务汇总主表";
  });
}
/** add by CYQ 2025年2月5日 编辑后回写表头金额 */
function afterEvent(row) {
  let sum = sumAmount(hsmDetailList.value);//汇总表体获得月底余额
  form.value.endDeposit = sum;//月底余额
  form.value.balance = subtractValues(sum,form.value.startDeposit)//计算结余，月底余额-月初余额
  //回写表体变化量
  hsmDetailList.value.forEach((item) => {
    if(item.index == row.index){
      let amount = item.changeAmountOld===null?0:item.changeAmountOld;
      item.changeAmount = amount + (Number(row.amount) || 0); // 设置金额
    }
  });

}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["summaryRef"].validate(valid => {
    if (valid) {
      form.value.hsmDetailList = hsmDetailList.value;
      if (form.value.id != null) {
        updateSummary(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addSummary(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}
function sumAmount(jsonArray) {
  const total = jsonArray.reduce((accumulator, currentValue) => {
    // 尝试将 amount 字段转换为数值
    const amount = Number(currentValue.amount);

    // 检查转换结果是否为有效数字（排除 NaN）
    if (!isNaN(amount)) {
      return accumulator + amount;
    }
    return accumulator;
  }, 0); // 初始值为 0

  // 返回保留两位小数的结果，格式化为字符串
  return total.toFixed(2);
}
function subtractValues(value1, value2) {
  // 将输入值转换为数值
  const num1 = Number(value1);
  const num2 = Number(value2);

  // 检查是否为有效数字
  if (isNaN(num1) || isNaN(num2)) {
    console.error('输入包含无效数字');
    return null; // 或者可以根据需要抛出错误或返回其他值
  }

  // 计算差值
  const difference = num1 - num2;

  // 返回保留两位小数的结果
  return difference.toFixed(2);
}
/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除财务汇总主编号为"' + _ids + '"的数据项？').then(function () {
    return delSummary(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 财务明细子表序号 */
function rowHsmDetailIndex({row, rowIndex}) {
  row.index = rowIndex + 1;
}

/** 财务明细子表添加按钮操作 */
function handleAddHsmDetail() {
  let obj = {};
  obj.category = "";
  obj.type = "";
  obj.amount = "";
  obj.changeAmount = "";
  obj.remark = "";
  hsmDetailList.value.push(obj);
}

/** 财务明细子表删除按钮操作 */
function handleDeleteHsmDetail() {
  if (checkedHsmDetail.value.length == 0) {
    proxy.$modal.msgError("请先选择要删除的财务明细子表数据");
  } else {
    const hsmDetails = hsmDetailList.value;
    const checkedHsmDetails = checkedHsmDetail.value;
    hsmDetailList.value = hsmDetails.filter(function (item) {
      return checkedHsmDetails.indexOf(item.index) == -1
    });
  }
}

/** 复选框选中数据 */
function handleHsmDetailSelectionChange(selection) {
  checkedHsmDetail.value = selection.map(item => item.index)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('main/summary/export', {
    ...queryParams.value
  }, `summary_${new Date().getTime()}.xlsx`)
}
/** 显示统计报表 */
function forms() {
  formsSummary().then(response => {
    // response示例如下：
    // {
    //   "msg": "操作成功",
    //     "code": 200,
    //     "data": [
    //   {
    //     "date": "2025-1",
    //     "expense": 12651.10
    //   },
    //   {
    //     "date": "2025-2",
    //     "expense": 5380.58
    //   },
    //   {
    //     "date": "2025-3",
    //     "expense": 2937.48
    //   },
    //   {
    //     "date": "2025-4",
    //     "expense": 7315.94
    //   }
    // ]
    // }
    chartData.value = response.data;
    debugger
    showChartDialog.value = true;

  }).catch(error => {
    proxy.$modal.msgError("获取报表数据失败：" + error.message);
  });
}

getList();
</script>
