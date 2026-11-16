<template>
  <view class="notification-page">
    <!-- 顶部操作栏 -->
    <view class="action-bar" v-if="notifications.length > 0">
      <text class="action-btn" @tap="markAllRead" v-if="unreadCount > 0">全部已读</text>
      <text class="action-btn action-btn--danger" @tap="handleClearAll">清空</text>
    </view>

    <!-- 通知列表 -->
    <view v-if="notifications.length > 0" class="notification-list">
      <view
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ 'notification-item--unread': !item.read }"
        @tap="handleItemClick(item)"
      >
        <!-- 未读标记点 -->
        <view class="unread-dot" v-if="!item.read"></view>

        <!-- 内容区 -->
        <view class="item-content">
          <view class="item-header">
            <text class="item-title">{{ item.title }}</text>
            <text class="item-time">{{ formatTime(item.time) }}</text>
          </view>
          <text class="item-body">{{ item.content }}</text>
        </view>

        <!-- 右箭头 -->
        <text class="item-arrow">›</text>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <image class="empty-icon" src="/static/images/empty.png" mode="aspectFit" />
      <text class="empty-text">暂无通知消息</text>
      <text class="empty-tip">应用收到的推送通知会显示在这里</text>
    </view>
  </view>
</template>

<script>
import Push from '@/utils/push.js'

export default {
  data() {
    return {
      notifications: [],
      unreadCount: 0
    }
  },
  onShow() {
    this.loadNotifications()
  },
  onPullDownRefresh() {
    this.loadNotifications()
    uni.stopPullDownRefresh()
  },
  methods: {
    loadNotifications() {
      this.notifications = Push.getNotifications()
      this.unreadCount = this.notifications.filter(item => !item.read).length
    },
    formatTime(timeStr) {
      if (!timeStr) return ''
      try {
        const date = new Date(timeStr)
        const now = new Date()
        const diff = now - date
        if (diff < 60000) return '刚刚'
        if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
        if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
        if (diff < 172800000) return '昨天'
        const month = (date.getMonth() + 1).toString().padStart(2, '0')
        const day = date.getDate().toString().padStart(2, '0')
        return month + '-' + day
      } catch (e) {
        return ''
      }
    },
    handleItemClick(item) {
      // 标记为已读
      if (!item.read) {
        item.read = true
        Push.markAllRead()
        this.loadNotifications()
      }
      // 如果有 payload 页面跳转信息，执行跳转
      try {
        const payload = item.payload ? JSON.parse(item.payload) : null
        if (payload && payload.page) {
          uni.navigateTo({ url: payload.page })
        }
      } catch (e) {
        // 忽略
      }
    },
    markAllRead() {
      Push.markAllRead()
      this.loadNotifications()
      uni.showToast({ title: '已全部标为已读', icon: 'success', duration: 1500 })
    },
    handleClearAll() {
      uni.showModal({
        title: '确认清空',
        content: '确定要清空所有通知消息吗？',
        confirmColor: '#e74c3c',
        success: (res) => {
          if (res.confirm) {
            Push.clearAll()
            this.loadNotifications()
            uni.showToast({ title: '已清空', icon: 'success', duration: 1500 })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.notification-page {
  min-height: 100vh;
  background: #f5f6fa;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  padding: 20rpx 30rpx 10rpx;
  gap: 30rpx;
}

.action-btn {
  font-size: 26rpx;
  color: #409EFF;
  padding: 8rpx 16rpx;
}

.action-btn--danger {
  color: #e74c3c;
}

.notification-list {
  padding: 0 24rpx;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 16rpx;
  position: relative;
  transition: background 0.2s;
}

.notification-item--unread {
  background: #f0f5ff;
}

.notification-item:active {
  background: #f0f0f0;
}

.unread-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #409EFF;
  margin-right: 16rpx;
  margin-top: 8rpx;
  flex-shrink: 0;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.item-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-time {
  font-size: 22rpx;
  color: #c0c4cc;
  margin-left: 16rpx;
  flex-shrink: 0;
}

.item-body {
  font-size: 26rpx;
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.item-arrow {
  font-size: 36rpx;
  color: #c0c4cc;
  margin-left: 12rpx;
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 30rpx;
  opacity: 0.3;
}

.empty-text {
  font-size: 30rpx;
  color: #909399;
  margin-bottom: 12rpx;
}

.empty-tip {
  font-size: 24rpx;
  color: #c0c4cc;
}
</style>
