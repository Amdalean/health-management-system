<template>
  <view class="detail-container">
    <view v-if="loading" class="loading-container">
      <uni-icons type="spinner-cycle" size="24" color="#409eff"></uni-icons>
      <text class="loading-text">加载中...</text>
    </view>

    <view v-else-if="record" class="detail-content">
      <!-- 基本信息卡片 -->
      <view class="info-card">
        <view class="card-title">基本信息</view>
        <view class="info-item">
          <text class="info-label">记录ID</text>
          <text class="info-value">{{ record.id }}</text>
        </view>
        <view v-if="record.courseName" class="info-item">
          <text class="info-label">课程</text>
          <text class="info-value">{{ record.courseName }}</text>
        </view>
        <view v-if="record.studentName" class="info-item">
          <text class="info-label">学生</text>
          <text class="info-value">{{ (record.studentNo ? record.studentNo + ' ' : '') + record.studentName }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">分析类型</text>
          <text class="info-value">{{ record.analysisType || '八段锦' }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">分析状态</text>
          <view class="status-badge" :class="getStatusClass(record.status)">
            {{ getStatusText(record.status) }}
          </view>
        </view>
        <view v-if="record.createTime" class="info-item">
          <text class="info-label">创建时间</text>
          <text class="info-value">{{ record.createTime }}</text>
        </view>
        <view v-if="record.updateTime" class="info-item">
          <text class="info-label">更新时间</text>
          <text class="info-value">{{ record.updateTime }}</text>
        </view>
      </view>

      <!-- 评分卡片 -->
      <view v-if="record.score" class="score-card">
        <view class="card-title">AI 评分</view>
        <view class="score-value">{{ record.score }}</view>
        <view class="score-desc">
          <uni-icons type="star-filled" size="20" color="#ffa500"></uni-icons>
          <text class="score-text">满分100分</text>
        </view>
      </view>

      <!-- AI 评语卡片 -->
      <view v-if="record.feedback" class="feedback-card">
        <view class="card-title">AI 评语</view>
        <view class="feedback-content">
          <text class="feedback-text">{{ record.feedback }}</text>
        </view>
      </view>

      <!-- 分析详情卡片 -->
      <view v-if="record.analysisDetails" class="details-card">
        <view class="card-title">分析详情</view>
        <view class="details-content">
          <text class="details-text">{{ record.analysisDetails }}</text>
        </view>
      </view>

      <!-- 文件信息卡片 -->
      <view v-if="record.fileName" class="file-card">
        <view class="card-title">文件信息</view>
        <view class="info-item">
          <text class="info-label">文件名</text>
          <text class="info-value">{{ record.fileName }}</text>
        </view>
        <view v-if="record.fileSize" class="info-item">
          <text class="info-label">文件大小</text>
          <text class="info-value">{{ formatFileSize(record.fileSize) }}</text>
        </view>
        <view v-if="record.filePath" class="info-item">
          <text class="info-label">文件路径</text>
          <text class="info-value file-path">{{ record.filePath }}</text>
        </view>
      </view>

      <!-- 备注卡片 -->
      <view v-if="record.remark" class="remark-card">
        <view class="card-title">备注</view>
        <view class="remark-content">
          <text class="remark-text">{{ record.remark }}</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-buttons">
        <view v-if="record.status === '0' || record.status === '3'" class="action-btn primary" @click="submitAnalysis">
          <uni-icons type="checkmarkempty" size="18" color="#fff"></uni-icons>
          <text class="btn-text">提交分析</text>
        </view>
        <view class="action-btn secondary" @click="goEdit">
          <uni-icons type="compose" size="18" color="#409eff"></uni-icons>
          <text class="btn-text">编辑</text>
        </view>
      </view>
    </view>

    <view v-else class="empty-container">
      <uni-icons type="info" size="40" color="#ccc"></uni-icons>
      <text class="empty-text">暂无数据</text>
    </view>
  </view>
</template>

<script>
import { getScoreRecord, submitForAnalysis } from '@/api/main/scoreRecord'

export default {
  data() {
    return {
      record: null,
      loading: false,
      id: null
    }
  },
  onLoad(options) {
    if (options.id) {
      this.id = options.id
      this.getDetail()
    }
  },
  methods: {
    getDetail() {
      this.loading = true
      getScoreRecord(this.id).then(res => {
        this.record = res.data
        this.loading = false
      }).catch(() => {
        this.loading = false
        uni.showToast({ title: '获取详情失败', icon: 'error' })
      })
    },
    submitAnalysis() {
      uni.showModal({
        title: '提示',
        content: '确认提交该记录进行 AI 分析吗？',
        success: (res) => {
          if (res.confirm) {
            const data = {
              id: this.record.id,
              courseId: this.record.courseId,
              filePath: this.record.filePath,
              fileName: this.record.fileName,
              fileSize: this.record.fileSize,
              analysisType: this.record.analysisType || '八段锦'
            }
            submitForAnalysis(data).then(() => {
              uni.showToast({ title: '提交成功，请稍后查看结果', icon: 'success' })
              setTimeout(() => {
                this.getDetail()
              }, 1000)
            })
          }
        }
      })
    },
    goEdit() {
      uni.navigateTo({ url: '/pages/scoreRecord/add?id=' + this.id })
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
    },
    formatFileSize(bytes) {
      if (!bytes) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
    }
  }
}
</script>

<style scoped>
.detail-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 16px;
}

.loading-container, .empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.loading-text, .empty-text {
  margin-top: 12px;
  font-size: 14px;
  color: #999;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-card, .score-card, .feedback-card, .details-card, .file-card, .remark-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.info-label {
  font-size: 14px;
  color: #666;
}

.info-value {
  font-size: 14px;
  color: #333;
  flex: 1;
  text-align: right;
  margin-left: 12px;
}

.file-path {
  font-size: 12px;
  color: #999;
  word-break: break-all;
}

.status-badge {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  margin-left: 12px;
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

.score-card {
  text-align: center;
}

.score-value {
  font-size: 48px;
  font-weight: 700;
  color: #ffa500;
  margin: 12px 0;
}

.score-desc {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.score-text {
  font-size: 14px;
  color: #999;
}

.feedback-content, .details-content, .remark-content {
  padding: 8px 0;
}

.feedback-text, .details-text, .remark-text {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.action-buttons {
  display: flex;
  gap: 12px;
  padding: 16px 0;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.action-btn:active {
  opacity: 0.8;
}

.action-btn.primary {
  background-color: #67c23a;
}

.action-btn.primary .btn-text {
  color: #fff;
}

.action-btn.secondary .btn-text {
  color: #409eff;
}

.btn-text {
  font-size: 14px;
  font-weight: 500;
}
</style>
