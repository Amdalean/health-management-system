<template>
  <view class="reminder-container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <uni-easyinput v-model="query.title" placeholder="请输入提醒标题" @confirm="handleQuery" />
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

    <!-- 提醒列表 -->
    <scroll-view class="reminder-list" scroll-y @scrolltolower="loadMore" :lower-threshold="100">
      <view v-if="list.length === 0 && !loading" class="empty-tip">
        <uni-icons type="info" size="40" color="#ccc"></uni-icons>
        <text class="empty-text">暂无提醒</text>
      </view>

      <view v-for="item in list" :key="item.id" class="reminder-card" @click="goEdit(item)">
        <view class="reminder-header">
          <view class="reminder-title">{{ item.title }}</view>
          <view class="reminder-status" :class="getStatusClass(item.status)">
            {{ getStatusText(item.status) }}
          </view>
        </view>

        <view class="reminder-content">
          <view class="reminder-item">
            <uni-icons type="calendar" size="16" color="#666"></uni-icons>
            <text class="reminder-text">{{ item.remindDate }}</text>
            <text v-if="item.calendarType" class="calendar-tag">
              {{ item.calendarType === '0' ? '公历' : '农历' }}
            </text>
          </view>

          <view v-if="item.remindEmail" class="reminder-item">
            <uni-icons type="email" size="16" color="#666"></uni-icons>
            <text class="reminder-text">{{ item.remindEmail }}</text>
          </view>

          <view v-if="item.countdown !== null && item.countdown !== undefined" class="reminder-item">
            <uni-icons type="clock" size="16" color="#666"></uni-icons>
            <text class="reminder-text countdown" :class="getCountdownClass(item.countdown)">
              倒计时 {{ item.countdown }} 天
            </text>
          </view>
        </view>

        <view class="reminder-actions">
          <view class="action-icon" @click.stop="goEdit(item)">
            <uni-icons type="compose" size="18" color="#e6a23c"></uni-icons>
          </view>
          <view class="action-icon" @click.stop="del(item)">
            <uni-icons type="trash" size="18" color="#f56c6c"></uni-icons>
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
import { listReminder, delReminder } from '@/api/main/reminder'

export default {
  data() {
    return {
      query: { 
        title: '',
        pageNum: 1,
        pageSize: 50
      },
      list: [],
      total: 0,
      loading: false,
      hasMore: true
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
      listReminder(this.query).then(res => {
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
      this.query.title = ''
      this.refreshList()
    },
    goAdd() {
      uni.navigateTo({ url: '/pages/reminder/add' })
    },
    goEdit(item) {
      uni.navigateTo({ url: '/pages/reminder/edit?id=' + item.id })
    },
    del(item) {
      uni.showModal({
        title: '提示',
        content: '确定删除该提醒吗？',
        success: (res) => {
          if (res.confirm) {
            delReminder(item.id).then(() => {
              uni.showToast({ title: '删除成功', icon: 'success' })
              this.refreshList()
            })
          }
        }
      })
    },
    getStatusClass(status) {
      return status === '0' ? 'status-disabled' : 'status-enabled'
    },
    getStatusText(status) {
      return status === '0' ? '已停用' : '已启用'
    },
    getCountdownClass(countdown) {
      if (countdown <= 0) return 'countdown-expired'
      if (countdown <= 3) return 'countdown-urgent'
      if (countdown <= 7) return 'countdown-soon'
      return ''
    }
  }
}
</script>

<style scoped>
.reminder-container {
  padding: 16px;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  background-color: #fff;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-btn {
  flex-shrink: 0;
}

.reset-btn {
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

.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 220px);
}

.reminder-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.reminder-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.reminder-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reminder-status {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  flex-shrink: 0;
  margin-left: 8px;
}

.status-enabled {
  background-color: #e1f3d8;
  color: #67c23a;
}

.status-disabled {
  background-color: #f4f4f5;
  color: #909399;
}

.reminder-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.reminder-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.reminder-text {
  font-size: 14px;
  color: #666;
  flex: 1;
}

.calendar-tag {
  font-size: 12px;
  color: #409eff;
  background-color: #ecf5ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.countdown {
  font-weight: 500;
}

.countdown-expired {
  color: #f56c6c;
}

.countdown-urgent {
  color: #e6a23c;
}

.countdown-soon {
  color: #409eff;
}

.reminder-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.action-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #f9f9f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.action-icon:active {
  background-color: #f0f0f0;
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
