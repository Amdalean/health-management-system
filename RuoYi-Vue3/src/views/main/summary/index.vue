<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="年度" prop="year">
        <el-select v-model="queryParams.year" placeholder="请选择年度" clearable style="width: 120px">
          <el-option
              v-for="dict in year"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="月份" prop="month">
        <el-select v-model="queryParams.month" placeholder="请选择月份" clearable style="width: 120px">
          <el-option
              v-for="dict in month"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
<!--      <el-form-item label="制单人" prop="createBy">-->
<!--        <el-input-->
<!--            v-model="queryParams.createBy"-->
<!--            placeholder="制单人"-->
<!--            clearable-->
<!--            readonly-->
<!--            @keyup.enter="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
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
      <!-- 计划执行情况 -->
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="DataAnalysis"
            @click="showDepositPlan"
            v-hasPermi="['main:summary:export']"
        >计划执行情况</el-button>
      </el-col>
      <!-- 计划执行情况 -->
<!--      <el-col :span="1.5">-->
<!--        <el-button-->
<!--            type="success"-->
<!--            plain-->
<!--            icon="DataAnalysis"-->
<!--            @click="showDepositPlan"-->
<!--            v-hasPermi="['main:summary:export']"-->
<!--        >计划执行预测</el-button>-->
<!--      </el-col>-->

      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="summaryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
<!--      <el-table-column label="主键ID" align="center" prop="id" />-->
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

      <el-table-column label="创建者" align="center" prop="createByName" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新者" align="center" prop="updateByName" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
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
          <el-input v-model="form.income" placeholder="请输入收入" @input="calculateExpenseOnInput" />
        </el-form-item>
        <el-form-item label="支出" prop="expense">
          <el-input v-model="form.expense" placeholder="系统自动计算" disabled />
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
              <el-input 
                v-model="scope.row.amount" 
                placeholder="金额或公式(如=1+1，回车计算)" 
                @input="handleAmountInput(scope.row)" 
                @keyup.enter="calculateFormulaOnEnter(scope.row)"
              />
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
      <FinanceChart :data="chartData" v-if="showChartDialog" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeChartDialog">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 计划执行情况弹窗 -->
    <el-dialog
        title="存款计划执行情况与预测"
        v-model="showDepositPlanDialog"
        width="98%"
        top="2vh"
        destroy-on-close
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        class="deposit-plan-dialog"
    >
      <DepositPlanChart :data="depositPlanData" :predictionData="predictionData" v-if="showDepositPlanDialog" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDepositPlanDialog">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 执行预测弹窗 -->
<!--    <el-dialog-->
<!--        title="存款执行预测"-->
<!--        v-model="showPredictionDialog"-->
<!--        width="90%"-->
<!--        top="5vh"-->
<!--        destroy-on-close-->
<!--    >-->
<!--      <DepositPredictionChart :data="predictionData" v-if="showPredictionDialog" />-->
<!--      <template #footer>-->
<!--        <div class="dialog-footer">-->
<!--          <el-button @click="closePredictionDialog">关 闭</el-button>-->
<!--        </div>-->
<!--      </template>-->
<!--    </el-dialog>-->
  </div>
  <!-- <G2Demo /> -->
</template>

<script setup name="Summary">
import {listSummary, getSummary, delSummary, addSummary, updateSummary, initSummary,formsSummary, getDepositPlanData, getDepositPrediction} from "@/api/main/summary";
import FinanceChart from '@/components/FinanceChart';
import DepositPlanChart from '@/components/DepositPlanChart';
// import DepositPredictionChart from '@/components/DepositPredictionChart';
import useUserStore from '@/store/modules/user'
// import FinanceChart from '@/components/G2Demo';
// import jwtDecode from 'jwt-decode';
// import store from "@/store";

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

// 计划执行情况相关变量
const showDepositPlanDialog = ref(false);
const depositPlanData = ref([]);

// 执行预测相关变量
const showPredictionDialog = ref(false);
const predictionData = ref([]);


// const token = localStorage.getItem('user-token');
// const decoded = jwtDecode(token);
// const userId = decoded.userId;
// import store from "@/store";
// const userId = store.state.user.id//将缓存的用户id赋值给userId
// const userStore = store()
// const { id: userId } = userStore



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
    endDeposit: null,
    createBy:null
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

// const userStore = useUserStore()
// const { id } = userStore
// data.queryParams.createBy = id===1?null:id;
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
    
    // 在修改模式下，确保每个明细项都有changeAmountOld值，避免计算错误
    hsmDetailList.value.forEach((item) => {
      if (item.changeAmountOld === undefined || item.changeAmountOld === null) {
        // 如果后端没有返回changeAmountOld，则设置为0以避免计算错误
        item.changeAmountOld = item.changeAmount || 0;
      }
    });
    
    open.value = true;
    title.value = "修改财务汇总主表";
  });
}
/** add by CYQ 2025年2月5日 编辑后回写表头金额 */
function afterEvent(row) {
  let sum = sumAmount(hsmDetailList.value);//汇总表体获得月底余额
  form.value.endDeposit = sum;//月底余额
  form.value.balance = subtractValues(sum,form.value.startDeposit)//计算结余，月底余额-月初余额
  
  // 当结余变化时，自动计算支出
  calculateExpense();
  
  //回写表体变化量
  hsmDetailList.value.forEach((item) => {
    if(item.index == row.index){
      let amount = item.changeAmountOld || 0; // 使用默认值0，避免null问题
      item.changeAmount = amount + (Number(row.amount) || 0); // 设置金额
    }
  });
}

/**
 * 处理金额输入（不触发公式计算）
 */
function handleAmountInput(row) {
  // 只更新变化量，不计算公式
  let sum = sumAmount(hsmDetailList.value);
  form.value.endDeposit = sum;
  form.value.balance = subtractValues(sum,form.value.startDeposit);
  
  // 当结余变化时，自动计算支出
  calculateExpense();
  
  hsmDetailList.value.forEach((item) => {
    if(item.index == row.index){
      let amount = item.changeAmountOld || 0; // 使用默认值0，避免null问题
      item.changeAmount = amount + (Number(row.amount) || 0);
    }
  });
}

/**
 * 回车键触发公式计算
 */
function calculateFormulaOnEnter(row) {
  // 检查是否为公式输入（以等号开头）
  if (row.amount && typeof row.amount === 'string' && row.amount.startsWith('=')) {
    try {
      const result = calculateSimpleFormula(row.amount)
      if (result !== null) {
        row.amount = result
        // 计算完成后更新汇总
        afterEvent(row)
      }
    } catch (error) {
      console.error('公式计算错误:', error)
    }
  }
}

/**
 * 计算简单公式（类似Excel）
 * @param {string} formula 公式字符串，如 "=1+1" 或 "=A1+B2"
 * @returns {number|null} 计算结果
 */
function calculateSimpleFormula(formula) {
  if (!formula.startsWith('=')) {
    return null
  }
  
  let expression = formula.substring(1)
  
  // 替换变量引用 A1, A2, A3... 为实际数值
  expression = expression.replace(/A(\d+)/g, (match, index) => {
    const rowIndex = parseInt(index) - 1
    if (hsmDetailList.value[rowIndex] && hsmDetailList.value[rowIndex].amount) {
      return parseFloat(hsmDetailList.value[rowIndex].amount) || 0
    }
    return 0
  })
  
  // 安全计算
  try {
    const result = Function('"use strict"; return (' + expression + ')')()
    return isNaN(result) ? null : parseFloat(result.toFixed(2))
  } catch (error) {
    console.error('公式计算错误:', error)
    return null
  }
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["summaryRef"].validate(valid => {
    if (valid) {
      // 校验收入不能为0
      const income = parseFloat(form.value.income) || 0;
      if (income <= 0) {
        proxy.$modal.msgError("收入不能为0或负数，请检查输入");
        return;
      }
      
      // 自动计算支出
      calculateExpense();
      
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

/**
 * 自动计算支出
 * 支出 = 收入 - 结余
 */
function calculateExpense() {
  const income = parseFloat(form.value.income) || 0;
  const balance = parseFloat(form.value.balance) || 0;
  
  // 支出 = 收入 - 结余
  const expense = income - balance;
  
  // 确保支出不为负数
  if (expense >= 0) {
    form.value.expense = expense.toFixed(2);
  } else {
    // 如果计算结果为负数，给出提示
    proxy.$modal.msgWarning("计算出的支出为负数，请检查收入和结余数据");
    form.value.expense = "0.00";
  }
}

/**
 * 收入输入时实时计算支出
 */
function calculateExpenseOnInput() {
  // 延迟计算，避免频繁更新
  setTimeout(() => {
    calculateExpense();
  }, 100);
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
  obj.changeAmountOld = 0; // 初始化原始变化量
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
    chartData.value = response.data;
    showChartDialog.value = true;
  }).catch(error => {
    proxy.$modal.msgError("获取报表数据失败：" + error.message);
  });
}

/** 关闭统计报表 */
function closeChartDialog() {
  showChartDialog.value = false;
  chartData.value = { rows: [] };
}

/** 显示计划执行情况 */
function showDepositPlan() {
  // 同时获取计划数据和预测数据
  Promise.all([
    getDepositPlanData(),
    getDepositPrediction()
  ]).then(([planResponse, predictionResponse]) => {
    depositPlanData.value = planResponse.data;
    predictionData.value = predictionResponse.data;
    showDepositPlanDialog.value = true;
  }).catch(error => {
    proxy.$modal.msgError("获取数据失败：" + error.message);
  });
}

/** 关闭计划执行情况弹窗 */
function closeDepositPlanDialog() {
  showDepositPlanDialog.value = false;
  depositPlanData.value = [];
}

/** 执行预测 */
function showDepositPrediction() {
  getDepositPrediction().then(response => {
    predictionData.value = response.data;
    showPredictionDialog.value = true;
  }).catch(error => {
    proxy.$modal.msgError("获取预测数据失败：" + error.message);
  });
}

/** 关闭执行预测弹窗 */
function closePredictionDialog() {
  showPredictionDialog.value = false;
  predictionData.value = {};
}

getList();
</script>

<style scoped>
.deposit-plan-dialog :deep(.el-dialog) {
  height: 90vh;
  max-height: 90vh;
}

.deposit-plan-dialog :deep(.el-dialog__body) {
  height: calc(90vh - 120px);
  overflow-y: auto;
  padding: 0;
}

.deposit-plan-dialog :deep(.el-dialog__header) {
  padding: 20px 20px 10px 20px;
}

.deposit-plan-dialog :deep(.el-dialog__footer) {
  padding: 10px 20px 20px 20px;
}
</style>
