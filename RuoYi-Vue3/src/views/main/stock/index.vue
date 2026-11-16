<template>
  <div class="app-container">
    <!-- 顶部概览卡片 -->
    <el-row :gutter="16" class="mb16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">总资产</div>
          <div class="stat-value">¥{{ overview.totalAssets || 0 }}</div>
          <div class="stat-sub">持仓 {{ overview.stockCount || 0 }} 只</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">持仓市值</div>
          <div class="stat-value">¥{{ overview.totalValue || 0 }}</div>
          <div class="stat-sub" :class="overview.totalPl > 0 ? 'profit' : overview.totalPl < 0 ? 'loss' : ''">
            浮动盈亏 ¥{{ overview.totalPl || 0 }}
          </div>
          <div class="stat-sub" :class="equityOverCap ? 'loss' : ''">
            仓位 {{ overview.equityPct || 0 }}%<span v-if="overview.maxEquityPct"> / 上限 {{ overview.maxEquityPct }}%</span><span v-if="equityOverCap">（已满，不加仓）</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">可用现金</div>
          <div class="stat-value">¥{{ formatNumber(overview.availableCash || 0) }}</div>
          <div class="stat-sub">账户资金与参数见下方</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :class="{ 'has-alert': overview.pendingCount > 0 }">
          <div class="stat-label">待处理建议</div>
          <div class="stat-value" style="color: #e6a23c">{{ overview.pendingCount || 0 }}</div>
          <div class="stat-sub">条待操作</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 顾问参数（与 Python 顾问同源 config.json）+ 账户资金 -->
    <el-row :gutter="16" class="mb16">
      <el-col :span="24">
        <el-card shadow="never" body-style="padding: 0 16px 12px">
          <el-collapse v-model="advisorParamOpen" style="border-top:none;border-bottom:none">
            <el-collapse-item name="ap" title="顾问参数">
              <div class="param-sec">
                <div class="param-sec-title">
                  账户资金
                  <el-button link type="success" size="small" @click="openCashDialog(1)">入金</el-button>
                  <el-button link type="danger" size="small" @click="openCashDialog(-1)">出金</el-button>
                  <el-button link type="info" size="small" @click="openCashLog">流水</el-button>
                </div>
                <el-row :gutter="8">
                  <el-col :span="6" class="param-item">
                    <span class="param-label">可用现金</span>
                    <span class="param-value">¥{{ formatNumber(overview.availableCash || 0) }}</span>
                  </el-col>
                  <el-col :span="6" class="param-item">
                    <span class="param-label">总资产</span>
                    <span class="param-value">¥{{ formatNumber(overview.totalAssets || 0) }}</span>
                  </el-col>
                </el-row>
              </div>
              <div v-for="sec in advisorParamSections" :key="sec.title" class="param-sec">
                <div class="param-sec-title">{{ sec.title }}</div>
                <el-row :gutter="8">
                  <el-col v-for="it in sec.items" :key="sec.title + it.label" :span="6" class="param-item">
                    <span class="param-label">{{ it.label }}</span>
                    <span class="param-value">{{ it.value }}</span>
                  </el-col>
                </el-row>
              </div>
              <el-alert v-if="advisorParams.exists === false" type="info" :closable="false" show-icon
                        :title="advisorParams.message || '暂无顾问参数'" />
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" type="border-card" class="mb16">
      <!-- 持仓操作 Tab -->
      <el-tab-pane label="持仓操作" name="operation">
        <!-- 操作栏 -->
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" :icon="RefreshRight" @click="fetchOperationData" :loading="loading">刷新</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" :icon="Plus" @click="handleAddHolding">新增持仓</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" :icon="Cpu" @click="handleAnalyze" :loading="analyzing">
              {{ analyzing ? '分析中...' : '触发分析' }}
            </el-button>
          </el-col>
          <el-col :span="12">
            <span v-if="overview.lastAnalyzeTime" class="last-time">上次分析：{{ formatTime(overview.lastAnalyzeTime) }}</span>
            <span v-if="overview.lastScanTime" class="last-time" style="margin-left: 16px">上次扫描：{{ formatTime(overview.lastScanTime) }}</span>
          </el-col>
        </el-row>

        <!-- 持仓表格 -->
        <el-card shadow="never" class="mb16">
          <template #header><span>持仓明细</span></template>
          <el-table :data="holdings" v-loading="loading" stripe size="small" :default-sort="{ prop: 'marketValue', order: 'descending' }">
            <el-table-column prop="symbol" label="代码" width="90" sortable>
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="jumpToKline(row.symbol)" style="font-size:13px">
                  {{ row.symbol }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" width="100" sortable />
            <el-table-column prop="ownerName" label="归属人" width="100" show-overflow-tooltip sortable />
            <el-table-column prop="shares" label="股数" width="70" align="right" sortable />
            <el-table-column prop="costPrice" label="成本" width="80" align="right" sortable>
              <template #default="{ row }">¥{{ row.costPrice?.toFixed(3) }}</template>
            </el-table-column>
            <el-table-column prop="currentPrice" label="现价" width="80" align="right" sortable>
              <template #default="{ row }">¥{{ row.currentPrice?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="marketValue" label="市值" width="110" align="right" sortable>
              <template #default="{ row }">¥{{ row.marketValue?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="仓位占比" width="90" align="right" sortable
                             :sort-method="(a, b) => (a.marketValue || 0) - (b.marketValue || 0)">
              <template #default="{ row }">{{ positionPct(row) }}</template>
            </el-table-column>
            <el-table-column prop="profitLossPct" label="盈亏" width="130" align="right" sortable>
              <template #default="{ row }">
                <span :style="{ color: row.profitLossPct > 0 ? '#67c23a' : row.profitLossPct < 0 ? '#f56c6c' : '' }">
                  {{ row.profitLossPct > 0 ? '+' : '' }}{{ row.profitLossPct?.toFixed(2) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="regime" label="市场状态" width="100" align="center" sortable>
              <template #default="{ row }">
                <el-tag v-if="row.regime === 'bull'" type="danger" size="small">牛市</el-tag>
                <el-tag v-else-if="row.regime === 'bear'" type="info" size="small">熊市</el-tag>
                <el-tag v-else-if="row.regime === 'oscillating'" type="warning" size="small">震荡</el-tag>
                <el-tag v-else-if="row.regime === 'transitional'" size="small">过渡</el-tag>
                <span v-else style="color:#999">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="assignedStrategy" label="分配策略" min-width="140" sortable>
              <template #default="{ row }">
                <el-tag v-if="row.assignedStrategy === 'bollinger_mean_reversion'" type="success" size="small">布林带回归</el-tag>
                <el-tag v-else-if="row.assignedStrategy === 'double_ma_trend'" type="danger" size="small">双均线趋势</el-tag>
                <el-tag v-else-if="row.assignedStrategy === 'bollinger_grid'" type="warning" size="small">布林带网格</el-tag>
                <el-tag v-else-if="row.assignedStrategy === 'hold' || row.assignedStrategy === 'hold_and_wait'" type="info" size="small">持有观望</el-tag>
                <el-tag v-else size="small">{{ row.assignedStrategy || '—' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="handleEditHolding(row)">编辑</el-button>
                <el-button type="danger" size="small" link @click="handleDeleteHolding(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 建议表格 -->
        <el-card shadow="never">
          <template #header><span>操作建议</span></template>
          <el-table :data="recommendations" v-loading="loading" stripe size="small"
                    :default-sort="{ prop: 'createdTime', order: 'descending' }"
                    @sort-change="handleRecSortChange">
            <el-table-column prop="createdTime" label="时间" width="150" sortable="custom">
              <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
            </el-table-column>
            <el-table-column prop="symbol" label="代码" width="90" sortable="custom" />
            <el-table-column prop="name" label="名称" width="90" show-overflow-tooltip sortable="custom" />
            <el-table-column prop="ownerName" label="归属人" width="100" show-overflow-tooltip sortable="custom" />
            <el-table-column prop="action" label="操作" width="90" align="center" sortable="custom">
              <template #default="{ row }">
                <el-tag v-if="row.action === 'BUY'" type="success" size="small">买入</el-tag>
                <el-tag v-else-if="row.action === 'SELL'" type="danger" size="small">卖出</el-tag>
                <el-tag v-else-if="row.action === 'REDUCE'" type="warning" size="small">减仓</el-tag>
                <el-tag v-else-if="row.action === 'ADD'" type="primary" size="small">加仓</el-tag>
                <el-tag v-else size="small">{{ row.action }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="priceHint" label="建议价" width="90" align="right" sortable="custom">
              <template #default="{ row }">¥{{ row.priceHint || '—' }}</template>
            </el-table-column>
            <el-table-column prop="suggestedShares" label="建议股数" width="90" align="right" sortable="custom">
              <template #default="{ row }">{{ row.suggestedShares || '—' }}</template>
            </el-table-column>
            <el-table-column prop="suggestedAmount" label="建议金额" width="110" align="right" sortable="custom">
              <template #default="{ row }">{{ row.suggestedAmount ? '¥' + row.suggestedAmount.toLocaleString() : '—' }}</template>
            </el-table-column>
            <el-table-column prop="actualPrice" label="实际单价" width="90" align="right" sortable="custom">
              <template #default="{ row }">
                <template v-if="row.status === 'executed' && row.actualPrice">¥{{ row.actualPrice.toFixed(3) }}</template>
                <span v-else style="color:#c0c4cc">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="actualShares" label="实际股数" width="90" align="right" sortable="custom">
              <template #default="{ row }">
                <template v-if="row.status === 'executed' && row.actualShares">{{ row.actualShares }}</template>
                <span v-else style="color:#c0c4cc">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="理由" min-width="200" show-overflow-tooltip sortable="custom" />
            <el-table-column prop="confidence" label="置信度" width="80" align="center" sortable="custom">
              <template #default="{ row }">
                <el-tag v-if="row.confidence === 'high'" type="danger" size="small">高</el-tag>
                <el-tag v-else-if="row.confidence === 'medium'" type="warning" size="small">中</el-tag>
                <el-tag v-else size="small">低</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center" sortable="custom">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'pending'" type="warning" size="small">待处理</el-tag>
                <el-tag v-else-if="row.status === 'executed'" type="success" size="small">已执行</el-tag>
                <el-tag v-else-if="row.status === 'blocked'" type="info" size="small">已拦截</el-tag>
                <el-tag v-else size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'pending' && row.action !== 'HOLD'"
                  type="success" size="small" @click="handleExecute(row)">已执行</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:12px;text-align:right">
            <el-pagination
              v-model:current-page="recPage"
              v-model:page-size="recPageSize"
              :total="recTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="loadRecommendations"
              @size-change="loadRecommendations" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 数据分析 Tab -->
      <el-tab-pane label="数据分析" name="analysis">
        <!-- 操作栏 -->
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" :icon="RefreshRight" @click="fetchAnalysisData" :loading="analysisLoading">刷新</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button :icon="DataLine" @click="switchView('equity')" :type="view === 'equity' ? 'primary' : 'default'">权益曲线</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button :icon="List" @click="switchView('trades')" :type="view === 'trades' ? 'primary' : 'default'">交易记录</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button :icon="TrendCharts" @click="switchView('kline')" :type="view === 'kline' ? 'primary' : 'default'">K线信号</el-button>
          </el-col>
          <el-col :span="6" v-if="view === 'kline'">
            <el-select
              v-model="selectedSymbol"
              filterable
              remote
              reserve-keyword
              placeholder="输入代码或名称搜索已下载标的"
              :remote-method="searchKlineSymbol"
              :loading="klineSearchLoading"
              style="width:260px"
              @change="loadKline"
            >
              <el-option
                v-for="item in klineSearchResults"
                :key="item.symbol"
                :label="item.name + ' (' + item.symbol + ')'"
                :value="item.symbol"
              />
            </el-select>
          </el-col>
        </el-row>

        <!-- 权益曲线图 -->
        <el-card v-show="view === 'equity'" shadow="never" class="mb16">
          <template #header>
            <span>组合权益曲线</span>
            <span style="font-size:12px;color:#909399;margin-left:12px">基于交易流水重建 · 时间加权（已剔除出入金）</span>
          </template>
          <div ref="equityChartRef" style="height:420px;width:100%"></div>
        </el-card>

        <!-- 交易记录 -->
        <el-card v-show="view === 'trades'" shadow="never" class="mb16">
          <template #header><span>交易记录</span></template>
          <el-table :data="tradeLog" v-loading="analysisLoading" stripe size="small" empty-text="暂无交易记录">
            <el-table-column prop="tradeTime" label="时间" width="150" sortable>
              <template #default="{ row }">{{ formatTime(row.tradeTime) }}</template>
            </el-table-column>
            <el-table-column prop="symbol" label="代码" width="80" sortable />
            <el-table-column prop="name" label="名称" width="100" sortable />
            <el-table-column prop="action" label="操作" width="80" align="center" sortable>
              <template #default="{ row }">
                <el-tag :type="row.action === 'BUY' || row.action === 'ADD' ? 'success' : row.action === 'SELL' ? 'danger' : 'warning'" size="small">
                  {{ row.action }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" width="80" align="right" sortable>
              <template #default="{ row }">¥{{ row.price?.toFixed(3) }}</template>
            </el-table-column>
            <el-table-column prop="shares" label="股数" width="70" align="right" sortable />
            <el-table-column prop="amount" label="金额" width="110" align="right" sortable>
              <template #default="{ row }">¥{{ row.amount?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="fee" label="费用" width="80" align="right" sortable>
              <template #default="{ row }">¥{{ row.fee?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="note" label="备注" min-width="160" show-overflow-tooltip sortable />
          </el-table>
        </el-card>

        <!-- K线信号 -->
        <el-card v-if="view === 'kline'" shadow="never" class="mb16">
          <template #header>
            <span>{{ selectedSymbol ? (selectedName + ' (' + selectedSymbol + ')') : '请选择标的' }} K线 — MA10/MA30 + 金叉/死叉</span>
          </template>
          <div ref="klineChartRef" style="height:480px;width:100%"></div>
          <div style="margin-top:12px">
            <el-tag type="danger" size="small">▲ 金叉</el-tag>
            <el-tag type="success" size="small" style="margin-left:8px">▼ 死叉</el-tag>
            <el-tag type="primary" size="small" style="margin-left:8px">— MA10</el-tag>
            <el-tag type="warning" size="small" style="margin-left:8px">— MA30</el-tag>
          </div>
        </el-card>

        <!-- 系统状态（精简版） -->
        <el-card shadow="never">
          <template #header>
            <span>系统状态</span>
            <el-tag v-if="systemStatus" :type="systemStatus.tushareReachable ? 'success' : 'danger'" size="small" style="margin-left:12px">
              Tushare {{ systemStatus.tushareReachable ? '正常' : '不可达' }}
            </el-tag>
            <el-tag v-if="systemStatus" :type="systemStatus.mailConfigured ? 'success' : 'warning'" size="small" style="margin-left:8px">
              邮件 {{ systemStatus.mailConfigured ? '已配置' : '未配置' }}
            </el-tag>
          </template>
          <div v-if="systemStatus" class="system-status-compact">
            <span>行情日: {{ systemStatus.latestTradeDate || '—' }}</span>
            <span>同步: {{ systemStatus.lastSyncTime ? formatTime(systemStatus.lastSyncTime) : '—' }}</span>
            <span>分析: {{ systemStatus.lastAnalyzeTime ? formatTime(systemStatus.lastAnalyzeTime) : '—' }}</span>
            <span>扫描: {{ systemStatus.lastScanTime ? formatTime(systemStatus.lastScanTime) : '—' }}</span>
          </div>
          <div v-else style="color:#909399;font-size:13px">加载中...</div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑持仓对话框 -->
    <el-dialog v-model="holdingDialogVisible" :title="holdingForm.symbol ? '编辑持仓' : '新增持仓'" width="460px">
      <el-form :model="holdingForm" :rules="holdingRules" ref="holdingFormRef" label-width="80px">
        <el-form-item label="股票代码" prop="symbol">
          <el-input v-model="holdingForm.symbol" placeholder="如 600000（失焦自动带出名称）"
                    :disabled="!!holdingForm._edit" @blur="fillNameBySymbol" />
        </el-form-item>
        <el-form-item label="股票名称" prop="name">
          <el-input v-model="holdingForm.name" placeholder="如 浦发银行（失焦自动带出代码）"
                    :disabled="!!holdingForm._edit" @blur="fillSymbolByName" />
        </el-form-item>
        <el-form-item label="持仓股数" prop="shares">
          <el-input-number v-model="holdingForm.shares" :min="100" :step="100" />
        </el-form-item>
        <el-form-item label="成本价" prop="costPrice">
          <el-input-number v-model="holdingForm.costPrice" :min="0.01" :precision="3" :step="0.1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="holdingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHolding" :loading="holdingSubmitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 实际执行对话框 -->
    <el-dialog v-model="executeDialogVisible" title="实际执行" width="460px">
      <el-form :model="executeForm" :rules="executeRules" ref="executeFormRef" label-width="80px">
        <el-form-item label="标的">
          <span>{{ executeForm.name }} ({{ executeForm.symbol }})</span>
          <el-tag v-if="executeForm.action === 'BUY'" type="success" size="small" style="margin-left:8px">买入</el-tag>
          <el-tag v-else-if="executeForm.action === 'SELL'" type="danger" size="small" style="margin-left:8px">卖出</el-tag>
          <el-tag v-else-if="executeForm.action === 'REDUCE'" type="warning" size="small" style="margin-left:8px">减仓</el-tag>
          <el-tag v-else-if="executeForm.action === 'ADD'" type="primary" size="small" style="margin-left:8px">加仓</el-tag>
          <el-tag v-else size="small" style="margin-left:8px">{{ executeForm.action }}</el-tag>
        </el-form-item>
        <el-form-item label="实际单价" prop="price">
          <el-input-number v-model="executeForm.price" :min="0.001" :precision="3" :step="0.01" style="width:100%" />
        </el-form-item>
        <el-form-item label="实际股数" prop="shares">
          <el-input-number v-model="executeForm.shares" :min="1" :step="100" style="width:100%" />
        </el-form-item>
        <el-form-item label="成交金额">
          <span style="color:#409EFF;font-weight:600">¥{{ formatNumber((executeForm.price || 0) * (executeForm.shares || 0)) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExecute" :loading="executeSubmitting">确认执行</el-button>
      </template>
    </el-dialog>

    <!-- 现金入金/出金对话框 -->
    <el-dialog v-model="cashDialogVisible" title="现金调整" width="420px">
      <el-form :model="cashForm" :rules="cashRules" ref="cashFormRef" label-width="80px">
        <el-form-item label="方向">
          <el-radio-group v-model="cashForm.direction">
            <el-radio :value="1">入金</el-radio>
            <el-radio :value="-1">出金</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="cashForm.amount" :min="0.01" :precision="2" :step="1000" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="cashForm.note" maxlength="100" placeholder="选填，如 工资定投 / 生活支出" />
        </el-form-item>
        <el-form-item label="当前余额">
          <span style="font-weight:600">¥{{ formatNumber(overview.availableCash || 0) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cashDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCash" :loading="cashSubmitting">
          {{ cashForm.direction > 0 ? '确认入金' : '确认出金' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 现金流水对话框 -->
    <el-dialog v-model="cashLogVisible" title="现金流水" width="640px">
      <el-table :data="cashLog" size="small" stripe max-height="420">
        <el-table-column prop="create_time" label="时间" width="160" sortable>
          <template #default="{ row }">{{ formatTime(row.create_time) }}</template>
        </el-table-column>
        <el-table-column prop="change_type" label="类型" width="90" sortable>
          <template #default="{ row }">
            <el-tag :type="cashTagType(row.change_type)" size="small">{{ cashTypeLabel(row.change_type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="change_amount" label="变动" width="120" align="right" sortable
                         :sort-method="(a, b) => Number(a.change_amount || 0) - Number(b.change_amount || 0)">
          <template #default="{ row }">
            <span :style="{ color: Number(row.change_amount) >= 0 ? '#67C23A' : '#F56C6C' }">
              {{ Number(row.change_amount) >= 0 ? '+' : '' }}{{ formatNumber(row.change_amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="余额" width="120" align="right" sortable
                         :sort-method="(a, b) => Number(a.balance || 0) - Number(b.balance || 0)">
          <template #default="{ row }">¥{{ formatNumber(row.balance) }}</template>
        </el-table-column>
        <el-table-column prop="note" label="备注" show-overflow-tooltip sortable>
          <template #default="{ row }">{{ row.note || '—' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="StockAdvisor">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshRight, Cpu, DataLine, List, TrendCharts, Plus } from '@element-plus/icons-vue'
import { getOverview, getHoldings, getRecommendations, runAnalysis, executeRecommendation, getTradeLog, getPerformance, getKlineData, getSystemStatus, addHolding, updateHolding, deleteHolding, searchSymbols, lookupSymbols, adjustCash, getCashLog, getAdvisorParams } from '@/api/main/stock'
import { ElMessage, ElNotification, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const router = useRouter()

// Tab 状态
const activeTab = ref('operation')

// 持仓操作 Tab 数据
const loading = ref(false)
const analyzing = ref(false)
const overview = ref({})
const holdings = ref([])
const recommendations = ref([])
const recPage = ref(1)
const recPageSize = ref(20)
const recTotal = ref(0)
// 服务端排序（与分页配合，避免只排当前页）
const recSort = ref({ prop: 'createdTime', order: 'descending' })

// 数据分析 Tab 数据
const analysisLoading = ref(false)
const view = ref('equity')
const tradeLog = ref([])
const performance = ref([])
const selectedSymbol = ref('')
const selectedName = ref('')
const systemStatus = ref(null)

const equityChartRef = ref(null)
const klineChartRef = ref(null)
let equityChart = null
let klineChart = null
let refreshTimer = null

// K线搜索（支持非持仓已下载，防下拉滥用）add by CYQ 2026-08-31
const klineSearchLoading = ref(false)
const klineSearchResults = ref([])

// 持仓管理对话框
const holdingDialogVisible = ref(false)
const holdingFormRef = ref(null)
const holdingSubmitting = ref(false)
const holdingForm = ref({ symbol: '', name: '', shares: 100, costPrice: 0, _edit: false })
const holdingRules = {
  symbol: [{ required: true, message: '请输入股票代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入股票名称', trigger: 'blur' }],
  shares: [{ required: true, message: '请输入持仓股数', trigger: 'blur' }],
  costPrice: [{ required: true, message: '请输入成本价', trigger: 'blur' }]
}

// 实际执行对话框
const executeDialogVisible = ref(false)
const executeFormRef = ref(null)
const executeSubmitting = ref(false)
const executeForm = ref({ id: null, symbol: '', name: '', action: '', price: null, shares: null })
const executeRules = {
  price: [{ required: true, message: '请输入实际单价', trigger: 'blur' }],
  shares: [{ required: true, message: '请输入实际股数', trigger: 'blur' }]
}

// 现金入金/出金
const cashDialogVisible = ref(false)
const cashFormRef = ref(null)
const cashSubmitting = ref(false)
const cashForm = ref({ direction: 1, amount: 1000, note: '' })
const cashRules = {
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }]
}
const cashLogVisible = ref(false)
const cashLog = ref([])

function openCashDialog(direction) {
  cashForm.value = { direction, amount: 1000, note: '' }
  cashDialogVisible.value = true
  nextTick(() => cashFormRef.value?.clearValidate())
}

function submitCash() {
  cashFormRef.value.validate(valid => {
    if (!valid) return
    const signed = Math.abs(cashForm.value.amount || 0) * (cashForm.value.direction || 1)
    cashSubmitting.value = true
    adjustCash(signed, cashForm.value.note).then(r => {
      ElMessage.success(`已${signed > 0 ? '入金' : '出金'} ¥${formatNumber(Math.abs(signed))}，余额 ¥${formatNumber(r.data?.balance)}`)
      cashDialogVisible.value = false
      fetchOperationData()
    }).catch(() => {
      ElMessage.error('现金调整失败')
    }).finally(() => {
      cashSubmitting.value = false
    })
  })
}

function openCashLog() {
  getCashLog(50).then(r => {
    cashLog.value = r.data || []
    cashLogVisible.value = true
  })
}

function cashTypeLabel(t) {
  return { deposit: '入金', withdraw: '出金', buy: '买入', sell: '卖出' }[t] || t || '—'
}

function cashTagType(t) {
  if (t === 'deposit') return 'success'
  if (t === 'withdraw') return 'danger'
  if (t === 'buy') return 'warning'
  if (t === 'sell') return 'primary'
  return 'info'
}

/* ---------------- 顾问参数（与 Python 顾问同源 config.json） ---------------- */
const advisorParamOpen = ref(['ap'])
const advisorParams = ref({})
const ADVISOR_PARAM_LABELS = {
  target_position_pct: '单只目标权重 %',
  max_single_position_pct: '单票上限 %',
  max_positions: '最大持仓数',
  max_equity_pct: '权益上限 %',
  min_position_amount: '单只金额下限(元)',
  market_top_n: '实时扫描成交额 TopN',
  reentry_cooldown_days: '清仓后再入冷却(交易日)',
  min_avg_amount: '最小20日均成交额(元)',
  buy_position_pct: '单笔买入比例 %',
  commission_rate: '佣金率',
  min_commission: '最低佣金(元)',
  stamp_tax_rate: '印花税(卖出)',
  slippage_pct: '滑点',
  risk_free_rate: '无风险利率',
  fail_max_drawdown_pct: '回撤 FAIL 线 %',
  fail_return_pct: '亏损 FAIL 线 %',
  fail_excess_return_pct: '跑输基准 FAIL 线 %',
  min_days: '最少样本天数',
  primary: '主基准',
  ep_min: 'E/P 下限',
  pb_max: 'P/B 上限',
  lev_max: '资产负债率上限',
  coverage_min: '利息保障倍数下限',
  credit_hard: '偿债闸门硬约束',
  block_buy: '估值闸门拦截买入',
  enabled: '启用',
  require_profit_for_band_sell: '上轨/网格只在水上卖出',
  min_profit_pct: '卖出最低浮盈 %',
  grid_add_floor_pct: '网格补仓浮亏下限 %',
  sell_at_pct: '硬止损卖出线 %',
  reduce_at_pct: '硬止损减仓线 %',
  atr_period: 'ATR周期',
  atr_mult: 'ATR倍数',
  min_pct: 'ATR最小距离 %',
  max_pct: 'ATR最大距离 %',
  emergency_pct: 'ATR应急最大回撤 %',
}
function fmtParam(v){
  if (v === null || v === undefined) return '—'
  if (typeof v === 'boolean') return v ? '是' : '否'
  if (Array.isArray(v)) return v.length ? v.join(' / ') : '—'
  if (typeof v === 'number') return Number.isInteger(v) ? String(v) : String(Number(v.toFixed(6)))
  return String(v)
}
function _paramRows(obj){
  const out = []
  Object.keys(obj || {}).forEach(k => out.push({ label: ADVISOR_PARAM_LABELS[k] || k, value: fmtParam(obj[k]) }))
  return out
}
const advisorParamSections = computed(() => {
  const p = advisorParams.value || {}
  const secs = []
  if (p.portfolio) secs.push({ title: '组合规则', items: _paramRows(p.portfolio) })
  if (p.cost_model) secs.push({ title: '交易成本', items: _paramRows(p.cost_model) })
  if (p.risk) {
    const items = []
    Object.keys(p.risk).forEach(g => {
      const grp = p.risk[g] || {}
      Object.keys(grp).forEach(k => items.push({
        label: (g === 'hard_stop_loss' ? '硬止损·' : g === 'atr_stop_loss' ? 'ATR·' : '') + (ADVISOR_PARAM_LABELS[k] || k),
        value: g === 'atr_stop_loss' && ['min_pct', 'max_pct', 'emergency_pct'].includes(k)
          ? fmtParam(Number(grp[k]) * 100) : fmtParam(grp[k]),
      }))
    })
    secs.push({ title: '风控', items })
  }
  if (p.valuation) secs.push({ title: '估值闸门', items: _paramRows(p.valuation) })
  if (p.sell_discipline) secs.push({ title: '卖出纪律', items: _paramRows(p.sell_discipline) })
  if (p.evaluation) secs.push({ title: '评价函数', items: _paramRows(p.evaluation) })
  if (p.benchmark) secs.push({ title: '基准', items: [
    { label: '主基准', value: p.benchmark.primary || '—' },
    { label: '启用', value: fmtParam(p.benchmark.enabled) },
    { label: '候选指数', value: fmtParam(p.benchmark.indices) },
  ]})
  return secs
})

function formatTime(t) {
  if (!t) return '—'
  if (Array.isArray(t)) {
    const [y, m, d, hh, mm] = t
    return `${y}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')} ${String(hh||0).padStart(2,'0')}:${String(mm||0).padStart(2,'0')}`
  }
  return String(t).replace('T', ' ').substring(0, 19)
}

function jumpToKline(symbol) {
  router.push({ path: '/finance/stock/kline', query: { symbol } })
}

// 持仓操作 Tab 方法
function handleRecSortChange({ prop, order }) {
  const next = { prop: prop || 'createdTime', order: order || 'descending' }
  if (next.prop === recSort.value.prop && next.order === recSort.value.order) return
  recSort.value = next
  recPage.value = 1
  loadRecommendations()
}

function loadRecommendations() {
  return getRecommendations({
    pageNum: recPage.value,
    pageSize: recPageSize.value,
    orderByColumn: recSort.value.prop,
    isAsc: recSort.value.order === 'ascending' ? 'asc' : 'desc'
  }).then(r => {
    recommendations.value = r.rows || []
    recTotal.value = r.total || 0
  })
}

function fetchOperationData() {
  loading.value = true
  Promise.all([
    getOverview().then(r => { overview.value = r.data }),
    getHoldings().then(r => { holdings.value = r.data }),
    getAdvisorParams().then(r => { advisorParams.value = r.data || {} }).catch(() => {}),
    loadRecommendations()
  ]).finally(() => { loading.value = false })
}

function handleAnalyze() {
  analyzing.value = true
  runAnalysis().then(r => {
    const output = r.msg || r.data || ''
    const stockMatch = output.match(/持仓总数:\s*(\d+)/)
    const stockCount = stockMatch ? stockMatch[1] : '?'
    const hasAction = /建议:.*(BUY|SELL|REDUCE|ADD)/.test(output)

    if (hasAction) {
      ElNotification({ title: '分析完成', message: `共分析 ${stockCount} 只股票，发现新的操作建议`, type: 'warning', duration: 5000 })
    } else {
      ElMessage.success(`分析完成，共 ${stockCount} 只股票，暂无操作建议`)
    }
    fetchOperationData()
  }).catch(() => {
    ElMessage.error('分析失败，请检查日志')
  }).finally(() => { analyzing.value = false })
}

function handleExecute(row) {
  executeForm.value = {
    id: row.id,
    symbol: row.symbol,
    name: row.name,
    action: row.action,
    price: row.priceHint || null,
    shares: row.suggestedShares || null
  }
  executeDialogVisible.value = true
  nextTick(() => executeFormRef.value?.clearValidate())
}

function submitExecute() {
  executeFormRef.value.validate(valid => {
    if (!valid) return
    executeSubmitting.value = true
    const form = executeForm.value
    executeRecommendation(form.id, form.price, form.shares).then(() => {
      ElMessage.success('已标记为执行')
      executeDialogVisible.value = false
      fetchOperationData()
    }).catch(() => {
      ElMessage.error('执行失败')
    }).finally(() => {
      executeSubmitting.value = false
    })
  })
}

// 持仓管理方法
function handleAddHolding() {
  holdingForm.value = { symbol: '', name: '', shares: 100, costPrice: 0, _edit: false }
  holdingDialogVisible.value = true
  nextTick(() => holdingFormRef.value?.clearValidate())
}

/* 代码/名称互查：输入一项，失焦自动带出另一项（全 A 股）add by CYQ 2026-10-08 */
function _pickSymbolList(res) {
  let list = res && (res.data || res.rows || res)
  if (!Array.isArray(list)) list = []
  return list.filter(x => x && x.symbol)
}
async function fillNameBySymbol() {
  if (holdingForm.value._edit) return
  const sym = (holdingForm.value.symbol || '').trim().toUpperCase()
  if (!sym) return
  try {
    const list = _pickSymbolList(await lookupSymbols(sym, 10))
    if (!list.length) return
    const hit = list.find(x => String(x.symbol).toUpperCase() === sym) || list[0]
    if (hit && hit.name && hit.name !== hit.symbol) holdingForm.value.name = hit.name
  } catch (e) { /* 查不到就让用户手填 */ }
}
async function fillSymbolByName() {
  if (holdingForm.value._edit) return
  const nm = (holdingForm.value.name || '').trim()
  if (!nm) return
  if ((holdingForm.value.symbol || '').trim()) return   // 代码已填则不覆盖
  try {
    const list = _pickSymbolList(await lookupSymbols(nm, 10))
    if (!list.length) return
    const hit = list.find(x => x.name === nm) || list[0]
    if (hit && hit.symbol) holdingForm.value.symbol = hit.symbol
  } catch (e) { /* 查不到就让用户手填 */ }
}

function handleEditHolding(row) {
  holdingForm.value = {
    symbol: row.symbol,
    name: row.name,
    shares: row.shares,
    costPrice: row.costPrice,
    _edit: true
  }
  holdingDialogVisible.value = true
}

function handleDeleteHolding(row) {
  ElMessageBox.confirm(`确定删除 ${row.name}(${row.symbol}) 的持仓记录？`, '确认删除', {
    type: 'warning'
  }).then(() => {
    deleteHolding(row.symbol).then(() => {
      ElMessage.success('已删除')
      fetchOperationData()
    })
  }).catch(() => {})
}

function submitHolding() {
  holdingFormRef.value.validate(valid => {
    if (!valid) return
    holdingSubmitting.value = true
    const form = holdingForm.value
    const promise = form._edit
      ? updateHolding(form.symbol, { shares: form.shares, costPrice: form.costPrice })
      : addHolding({ symbol: form.symbol, name: form.name, shares: form.shares, costPrice: form.costPrice })

    promise.then(() => {
      ElMessage.success(form._edit ? '已更新' : '已添加')
      holdingDialogVisible.value = false
      fetchOperationData()
    }).catch(() => {
      ElMessage.error('操作失败')
    }).finally(() => {
      holdingSubmitting.value = false
    })
  })
}

// 数据分析 Tab 方法
function fetchAnalysisData() {
  analysisLoading.value = true
  Promise.all([
    getHoldings().then(r => {
      holdings.value = r.data || []
      klineSearchResults.value = holdings.value.map(h => ({ symbol: h.symbol, name: h.name })).slice(0, 20)
      if (holdings.value.length > 0 && !selectedSymbol.value) {
        selectedSymbol.value = holdings.value[0].symbol
        selectedName.value = holdings.value[0].name
      }
    }),
    getTradeLog().then(r => { tradeLog.value = r.data || [] }),
    getPerformance().then(r => {
      performance.value = r.data || []
      if (performance.value.length > 0 && selectedSymbol.value && !klineChart) {
        loadKline()
      }
    }),
    getSystemStatus().then(r => { systemStatus.value = r.data || null })
  ]).finally(() => {
    analysisLoading.value = false
    nextTick(() => {
      if (view.value === 'equity') renderEquityChart()
    })
  })
}

function switchView(v) {
  view.value = v
  nextTick(() => {
    if (v === 'equity') renderEquityChart()
    if (v === 'kline') {
      if (!selectedSymbol.value && holdings.value.length > 0) {
        selectedSymbol.value = holdings.value[0].symbol
        selectedName.value = holdings.value[0].name
        setTimeout(() => loadKline(), 50)
      } else if (selectedSymbol.value) {
        setTimeout(() => {
          if (klineChartRef.value) {
            if (klineChart) klineChart.resize()
            else loadKline()
          }
        }, 50)
      }
    }
  })
}

function formatNumber(n) {
  if (n == null) return '—'
  const v = Number(n)
  if (Number.isNaN(v)) return '—'
  return v.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

// 单只持仓占总资产比例（仓位），与后端 equityPct 同源口径
// add by CYQ 2026-10-15 持仓顾问仓位显示
function positionPct(row) {
  const ta = Number(overview.value.totalAssets) || 0
  const mv = Number(row?.marketValue) || 0
  if (ta <= 0) return '—'
  return (mv / ta * 100).toFixed(1) + '%'
}

// 权益占比是否已达上限（达上限时新增/加仓会被组合规则拦为 0 股）
const equityOverCap = computed(() => {
  const pct = Number(overview.value.equityPct)
  const cap = Number(overview.value.maxEquityPct)
  return cap > 0 && pct >= cap
})

// 搜索已下载标的（支持非持仓）add by CYQ 2026-08-31
function searchKlineSymbol(query) {
  if (!query || query.trim().length < 1) {
    klineSearchResults.value = holdings.value.map(h => ({ symbol: h.symbol, name: h.name })).slice(0, 20)
    return
  }
  klineSearchLoading.value = true
  const q = query.trim()
  searchSymbols(q, 20).then(res => {
    let list = res.data || res.rows || res || []
    if (!Array.isArray(list)) list = []
    if (list.length > 0) {
      klineSearchResults.value = list.slice(0, 20)
    } else {
      const uq = q.toUpperCase()
      const matches = holdings.value.filter(h => h.symbol.includes(uq) || (h.name && h.name.includes(q)))
        .map(h => ({ symbol: h.symbol, name: h.name }))
      klineSearchResults.value = matches.slice(0, 20)
    }
  }).catch(() => {
    const uq = q.toUpperCase()
    klineSearchResults.value = holdings.value.filter(h => h.symbol.includes(uq) || (h.name && h.name.includes(q)))
      .map(h => ({ symbol: h.symbol, name: h.name })).slice(0, 20)
  }).finally(() => { klineSearchLoading.value = false })
}

function loadKline() {
  if (!selectedSymbol.value) return
  const h = holdings.value.find(x => x.symbol === selectedSymbol.value)
  // 优先持仓名称，其次搜索结果名称，最后回退 symbol
  if (h) selectedName.value = h.name
  else {
    const sr = klineSearchResults.value.find(x => x.symbol === selectedSymbol.value)
    selectedName.value = sr ? sr.name : selectedSymbol.value
  }
  getKlineData(selectedSymbol.value, new Date().getFullYear().toString()).then(r => {
    // 后端已通过 stock_basic_cache 回退名称
    if (r.data && r.data.name && r.data.name !== selectedSymbol.value) selectedName.value = r.data.name
    nextTick(() => renderKlineChart(r.data))
  })
}

function renderEquityChart() {
  if (!equityChartRef.value || performance.value.length === 0) return
  if (equityChart) equityChart.dispose()
  equityChart = echarts.init(equityChartRef.value)

  const dates = performance.value.map(p => p.date)
  const values = performance.value.map(p => p.portfolioValue)
  const stocks = performance.value.map(p => p.stockValue)
  const returns = performance.value.map(p => p.totalReturnPct)

  equityChart.setOption({
    tooltip: { trigger: 'axis', confine: true,
      formatter: params => {
        const d = params[0].axisValue
        const p = performance.value.find(x => x.date === d)
        if (!p) return d
        return `<b>${d}</b><br/>组合值: ¥${formatNumber(p.portfolioValue)}<br/>持仓市值: ¥${formatNumber(p.stockValue)}<br/>现金: ¥${formatNumber(p.cash)}<br/>累计收益率(时间加权): <span style="color:${p.totalReturnPct >= 0 ? '#67c23a' : '#f56c6c'}">${p.totalReturnPct >= 0 ? '+' : ''}${p.totalReturnPct.toFixed(2)}%</span>`
      }
    },
    legend: { data: ['组合价值', '持仓市值', '累计收益率'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 45, fontSize: 10 }, boundaryGap: false },
    yAxis: [
      { type: 'value', name: '市值 (¥)', position: 'left' },
      { type: 'value', name: '收益率 (%)', position: 'right', axisLabel: { formatter: '{value}%' }, splitLine: { show: false } }
    ],
    dataZoom: [
      { type: 'inside', start: Math.max(0, 100 - 60 * 100 / dates.length), end: 100 },
      { type: 'slider', start: Math.max(0, 100 - 60 * 100 / dates.length), end: 100, height: 20, bottom: 0 }
    ],
    series: [
      { name: '组合价值', type: 'line', data: values, smooth: true, symbol: 'none',
        lineStyle: { width: 2, color: '#409EFF' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.25)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }
        ])}
      },
      { name: '持仓市值', type: 'line', data: stocks, smooth: true, symbol: 'none',
        lineStyle: { width: 1, color: '#A0CFFF', type: 'dashed' }
      },
      { name: '累计收益率', type: 'line', yAxisIndex: 1, data: returns, smooth: true, symbol: 'none',
        lineStyle: { width: 1.5, color: '#67C23A' }
      }
    ]
  })
}

function renderKlineChart(data) {
  if (!klineChartRef.value || !data) return
  if (!Array.isArray(data?.bars) || data.bars.length < 30) {
    if (!data?.bars && data?.klineBars) data.bars = data.klineBars
  }
  if (!data?.bars || data.bars.length < 30) return

  if (klineChart) klineChart.dispose()
  klineChart = echarts.init(klineChartRef.value)

  const bars = data.bars
  const dates = bars.map(b => b.date)
  const kData = bars.map(b => [b.open, b.close, b.low, b.high])
  const ma10Values = bars.map(b => b.ma10)
  const ma30Values = bars.map(b => b.ma30)
  const volumeData = bars.map(b => b.volume || 0)

  const signals = data.signals || []
  const gcMarkData = [], dcMarkData = []
  for (const sig of signals) {
    const idx = dates.indexOf(sig.date)
    if (idx >= 0) {
      const item = { coord: [idx, sig.price], value: sig.price, symbol: 'pin' }
      if (sig.type === 'golden_cross') {
        gcMarkData.push({ ...item, symbol: 'arrow', symbolRotate: 0, symbolSize: [14, 14],
          itemStyle: { color: '#f56c6c' }, label: { formatter: '金叉', color: '#f56c6c', fontSize: 10, position: 'top' } })
      } else if (sig.type === 'death_cross') {
        dcMarkData.push({ ...item, symbol: 'arrow', symbolRotate: 180, symbolSize: [14, 14],
          itemStyle: { color: '#67c23a' }, label: { formatter: '死叉', color: '#67c23a', fontSize: 10, position: 'bottom' } })
      }
    }
  }

  klineChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' }, confine: true,
      formatter: params => {
        const b = bars[params[0]?.dataIndex ?? 0]
        if (!b) return ''
        return `<b>${b.date}</b><br/>O: ${b.open.toFixed(2)} H: ${b.high.toFixed(2)}<br/>L: ${b.low.toFixed(2)} C: ${b.close.toFixed(2)}<br/>MA10: ${b.ma10?.toFixed(2) ?? '—'} MA30: ${b.ma30?.toFixed(2) ?? '—'}`
      }
    },
    legend: { data: ['K线', 'MA10', 'MA30'], top: 0 },
    grid: [{ left: '3%', right: '4%', top: '10%', height: '60%' }, { left: '3%', right: '4%', top: '78%', height: '14%' }],
    xAxis: [
      { type: 'category', data: dates, axisLabel: { rotate: 45, fontSize: 9, interval: Math.max(1, Math.floor(dates.length / 30)) },
        axisLine: { onZero: false }, splitLine: { show: false }, min: 'dataMin', max: 'dataMax', gridIndex: 0 },
      { type: 'category', data: dates, gridIndex: 1, axisLabel: { show: false }, axisLine: { show: false }, splitLine: { show: false } }
    ],
    yAxis: [
      { scale: true, gridIndex: 0, splitArea: { show: true, areaStyle: { color: ['rgba(250,250,250,0.3)', 'rgba(200,200,200,0.1)'] } } },
      { scale: true, gridIndex: 1, splitNumber: 2, axisLabel: { show: false }, splitLine: { show: false } }
    ],
    dataZoom: [
      { type: 'inside', xAxisIndex: [0, 1], start: Math.max(0, 100 - 90 * 100 / dates.length), end: 100 },
      { type: 'slider', xAxisIndex: [0, 1], start: Math.max(0, 100 - 90 * 100 / dates.length), end: 100, height: 16, bottom: 0 }
    ],
    series: [
      { name: 'K线', type: 'candlestick', data: kData, xAxisIndex: 0, yAxisIndex: 0,
        itemStyle: { color: '#ef5350', color0: '#26a69a', borderColor: '#ef5350', borderColor0: '#26a69a' },
        markPoint: { data: [...gcMarkData, ...dcMarkData], symbol: 'pin', symbolSize: 18, itemStyle: { color: '#f56c6c' }, label: { show: true, fontSize: 9 } }
      },
      { name: 'MA10', type: 'line', data: ma10Values, smooth: true, symbol: 'none', lineStyle: { width: 1, color: '#409EFF' }, xAxisIndex: 0, yAxisIndex: 0 },
      { name: 'MA30', type: 'line', data: ma30Values, smooth: true, symbol: 'none', lineStyle: { width: 1, color: '#E6A23C' }, xAxisIndex: 0, yAxisIndex: 0 },
      { name: '成交量', type: 'bar', data: volumeData, xAxisIndex: 1, yAxisIndex: 1,
        itemStyle: { color: p => { const b = bars[p.dataIndex]; return b && b.close >= b.open ? '#ef5350' : '#26a69a' } } }
    ]
  })
  nextTick(() => klineChart.resize())
}

function handleResize() {
  if (equityChart) equityChart.resize()
  if (klineChart) klineChart.resize()
}

watch(activeTab, (newTab) => {
  if (newTab === 'analysis' && !tradeLog.value.length) fetchAnalysisData()
})

onMounted(() => {
  fetchOperationData()
  window.addEventListener('resize', handleResize)
  refreshTimer = setInterval(() => {
    if (activeTab.value === 'operation') fetchOperationData()
    else fetchAnalysisData()
  }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (equityChart) { equityChart.dispose(); equityChart = null }
  if (klineChart) { klineChart.dispose(); klineChart = null }
})
</script>

<style scoped>
.stat-card { text-align: center; cursor: default; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 24px; font-weight: 600; color: #303133; }
.stat-sub { font-size: 12px; color: #909399; margin-top: 6px; }
.stat-sub.profit { color: #67c23a; }
.stat-sub.loss { color: #f56c6c; }
.param-sec { margin-bottom: 10px; }
.param-sec-title { font-weight: 600; font-size: 13px; color: #303133; margin: 4px 0 6px; }
.param-item { font-size: 12px; line-height: 22px; margin-bottom: 2px; }
.param-label { color: #909399; margin-right: 6px; }
.param-value { color: #303133; }
.has-alert { border-top: 3px solid #e6a23c; }
.mb16 { margin-bottom: 16px; }
.mb8 { margin-bottom: 8px; }
.last-time { font-size: 12px; color: #909399; margin-left: 8px; line-height: 32px; }
.system-status-compact { display: flex; gap: 24px; font-size: 13px; color: #606266; flex-wrap: wrap; }
.system-status-compact span { white-space: nowrap; }
</style>
