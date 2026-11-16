<template>
  <view class="score-record-container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <uni-easyinput v-model="query.score" placeholder="请输入AI评分" @confirm="handleQuery" />
      <uni-data-select
        v-model="query.status"
        :localdata="statusOptions"
        placeholder="请选择分析状态"
        @change="handleQuery"
      />
      <button class="search-btn" type="primary" size="mini" @click="handleQuery">搜索</button>
      <button class="reset-btn" type="default" size="mini" @click="resetQuery">重置</button>
    </view>

    <!-- 操作按钮 -->
    <view class="action-bar">
      <button class="add-btn" type="primary" size="mini" @click="goAdd">
        <uni-icons type="plusempty" size="14" color="#fff"></uni-icons>
        <text>新增</text>
      </button>
      <view class="total-text">共 {{ total }} 条</view>
    </view>

    <!-- 成绩智能分析列表 -->
    <scroll-view class="record-list" scroll-y @scrolltolower="loadMore" :lower-threshold="100">
      <view v-if="list.length === 0 && !loading" class="empty-tip">
        <uni-icons type="info" size="40" color="#ccc"></uni-icons>
        <text class="empty-text">暂无成绩智能分析</text>
      </view>

      <view v-for="item in list" :key="item.id" class="record-card" @click="goDetail(item)">
        <view class="record-header">
          <view class="record-id">ID: {{ item.id }}</view>
          <view class="record-status" :class="getStatusClass(item.status)">
            {{ getStatusText(item.status) }}
          </view>
        </view>

        <view class="record-content">
          <view v-if="item.score" class="record-item">
            <uni-icons type="star-filled" size="16" color="#ffa500"></uni-icons>
            <text class="record-text">AI评分: <text class="highlight">{{ item.score }}</text></text>
          </view>

          <view v-if="item.courseName" class="record-item">
            <uni-icons type="list" size="16" color="#666"></uni-icons>
            <text class="record-text">课程: {{ item.courseName }}</text>
          </view>

          <view v-if="item.analysisType" class="record-item">
            <uni-icons type="list" size="16" color="#666"></uni-icons>
            <text class="record-text">分析类型: {{ item.analysisType }}</text>
          </view>

          <view v-if="item.feedback" class="record-item">
            <uni-icons type="chat" size="16" color="#666"></uni-icons>
            <text class="record-text feedback">{{ item.feedback }}</text>
          </view>

          <view v-if="item.createTime" class="record-item">
            <uni-icons type="calendar" size="16" color="#666"></uni-icons>
            <text class="record-text">创建时间: {{ item.createTime }}</text>
          </view>
        </view>

        <view class="record-actions">
          <view v-if="item.status === '0' || item.status === '3'" class="action-btn" @click.stop="submitAnalysis(item)">
            <uni-icons type="checkmarkempty" size="16" color="#67c23a"></uni-icons>
            <text class="action-text">提交分析</text>
          </view>
          <view v-if="item.status === '1'" class="action-btn disabled" @click.stop="">
            <uni-icons type="spinner-cycle" size="16" color="#e6a23c"></uni-icons>
            <text class="action-text">分析中</text>
          </view>
          <view class="action-btn" @click.stop="goEdit(item)">
            <uni-icons type="compose" size="16" color="#409eff"></uni-icons>
            <text class="action-text">编辑</text>
          </view>
          <view class="action-btn" @click.stop="del(item)">
            <uni-icons type="trash" size="16" color="#f56c6c"></uni-icons>
            <text class="action-text">删除</text>
          </view>
        </view>
      </view>

      <!-- 加载更多提示 -->
      <view v-if="loading" class="loading-tip">
        <uni-icons type="spinner-cycle" size="16" color="#999"></uni-icons>
        <text class="loading-text">加载中...</text>
      </view>

      <view v-if="!hasMore && list.length > 0" class="no-more-tip">
        <text class="no-more-text">没有更多了</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { listScoreRecord, delScoreRecord, submitForAnalysis } from '@/api/main/scoreRecord'

export default {
  data() {
    return {
      query: {
        pageNum: 1,
        pageSize: 10,
        score: null,
        status: null
      },
      list: [],
      total: 0,
      loading: false,
      hasMore: true,
      statusOptions: [
        { text: '待分析', value: '0' },
        { text: '分析中', value: '1' },
        { text: '已完成', value: '2' },
        { text: '分析失败', value: '3' }
      ]
    }
  },
  onShow() {
    this.refreshList()
  },
  onPullDownRefresh() {
    this.refreshList()
  },
  methods: {
    // 刷新列表（重置分页）
    refreshList() {
      this.query.pageNum = 1
      this.list = []
      this.hasMore = true
      this.getList()
    },
    // 获取列表数据
    getList() {
      if (this.loading) return
      if (!this.hasMore) return

      this.loading = true
      listScoreRecord(this.query).then(res => {
        const rows = res.rows || []
        this.total = res.total || 0

        if (this.query.pageNum === 1) {
          this.list = rows
        } else {
          this.list = [...this.list, ...rows]
        }

        // 判断是否还有更多数据
        this.hasMore = this.list.length < this.total

        this.loading = false
        uni.stopPullDownRefresh()
      }).catch(() => {
        this.loading = false
        uni.stopPullDownRefresh()
      })
    },
    // 加载更多（翻页）
    loadMore() {
      if (this.hasMore && !this.loading) {
        this.query.pageNum++
        this.getList()
      }
    },
    // 搜索
    handleQuery() {
      this.refreshList()
    },
    // 重置查询
    resetQuery() {
      this.query.score = null
      this.query.status = null
      this.refreshList()
    },
    goAdd() {
      uni.navigateTo({ url: '/pages/scoreRecord/add' })
    },
    goDetail(item) {
      uni.navigateTo({ url: '/pages/scoreRecord/detail?id=' + item.id })
    },
    goEdit(item) {
      uni.navigateTo({ url: '/pages/scoreRecord/add?id=' + item.id })
    },
    del(item) {
      uni.showModal({
        title: '提示',
        content: '确定删除该成绩智能分析吗？',
        success: (res) => {
          if (res.confirm) {
            delScoreRecord(item.id).then(() => {
              uni.showToast({ title: '删除成功', icon: 'success' })
              this.refreshList()
            })
          }
        }
      })
    },
    submitAnalysis(item) {
      uni.showModal({
        title: '提示',
        content: '确认提交该记录进行 AI 分析吗？',
        success: (res) => {
          if (res.confirm) {
            const data = {
              id: item.id,
              courseId: item.courseId,
              filePath: item.filePath,
              fileName: item.fileName,
              fileSize: item.fileSize,
              analysisType: item.analysisType || '八段锦'
            }
            submitForAnalysis(data).then(() => {
              uni.showToast({ title: '提交成功，请稍后查看结果', icon: 'success' })
              this.refreshList()
            })
          }
        }
      })
    },
    getStatusClass(status) {
      switch (status) {
        case '0': return 'status-pending'
        case '1': return 'status-analyzing'
        case '2': return 'status-completed'
        case '3': return 'status-failed'
        default: return 'status-pending'
      }
    },
    getStatusText(status) {
      switch (status) {
        case '0': return '待分析'
        case '1': return '分析中'
        case '2': return '已完成'
        case '3': return '分析失败'
        default: return '未知状态'
      }
    }
  }
}
</script>

<style scoped>
.score-record-container {
  padding: 16px;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  background-color: #fff;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-btn, .reset-btn {
  flex-shrink: 0;
}

.action-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.total-text {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 260px);
}

.record-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.record-id {
  font-size: 14px;
  color: #999;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-status {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  flex-shrink: 0;
  margin-left: 8px;
}

.status-pending {
  background-color: #f4f4f5;
  color: #909399;
}

.status-analyzing {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.status-completed {
  background-color: #e1f3d8;
  color: #67c23a;
}

.status-failed {
  background-color: #fef0f0;
  color: #f56c6c;
}

.record-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.record-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.record-text {
  font-size: 13px;
  color: #666;
  flex: 1;
  line-height: 1.5;
}

.record-text.highlight {
  color: #ffa500;
  font-weight: 600;
}

.record-text.feedback {
  color: #409eff;
}

.record-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.action-btn:active {
  background-color: #f0f0f0;
}

.action-btn.disabled {
  opacity: 0.6;
}

.action-text {
  font-size: 12px;
  color: #666;
}

.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  background-color: #fff;
  border-radius: 12px;
}

.empty-text {
  margin-top: 12px;
  font-size: 14px;
  color: #999;
}

.loading-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  gap: 8px;
}

.loading-text {
  font-size: 13px;
  color: #999;
}

.no-more-tip {
  text-align: center;
  padding: 16px;
}

.no-more-text {
  font-size: 13px;
  color: #ccc;
}
</style>
