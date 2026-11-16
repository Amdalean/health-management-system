<template>
  <view class="push-demo-page">
    <!-- 连接状态 -->
    <view class="status-card">
      <view class="status-row">
        <view class="status-dot" :class="{ active: clientId }" />
        <text class="status-label">{{ clientId ? '已连接' : '未连接' }}</text>
        <text class="status-refresh" @tap="refreshStatus">刷新</text>
      </view>
      <view class="cid-box" v-if="clientId">
        <text class="cid-label">ClientID</text>
        <text class="cid-value" @longpress="copyCid">{{ clientId }}</text>
      </view>
      <view class="cid-box" v-else>
        <text class="cid-placeholder">请在真机/模拟器上运行以获取 ClientID</text>
      </view>
    </view>

    <!-- 在线消息列表 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">在线推送消息</text>
        <text class="section-count">{{ messages.length }} 条</text>
      </view>
      <view v-if="messages.length === 0" class="empty">暂无消息，点击下方按钮模拟</view>
      <view v-for="(msg, i) in messages" :key="i" class="msg-item receive">
        <view class="msg-top">
          <text class="msg-title">{{ msg.title || '无标题' }}</text>
          <text class="msg-time">{{ msg._time }}</text>
        </view>
        <text class="msg-body">{{ msg.content || msg.payload || JSON.stringify(msg) }}</text>
      </view>
    </view>

    <!-- 通知点击记录 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">通知点击记录</text>
        <text class="section-count">{{ clicks.length }} 条</text>
      </view>
      <view v-if="clicks.length === 0" class="empty">暂无记录</view>
      <view v-for="(msg, i) in clicks" :key="i" class="msg-item click">
        <view class="msg-top">
          <text class="msg-title">{{ msg.title || '无标题' }}</text>
          <text class="msg-time">{{ msg._time }}</text>
        </view>
        <text class="msg-body">{{ msg.content || msg.payload || JSON.stringify(msg) }}</text>
      </view>
    </view>

    <!-- 测试按钮 -->
    <view class="section">
      <view class="section-title" style="margin-bottom:20rpx">功能测试（开发调试）</view>
      <view class="btn-grid">
        <button class="test-btn" @tap="mockReceive">模拟在线消息</button>
        <button class="test-btn" @tap="mockClick">模拟通知点击</button>
        <button class="test-btn" @tap="mockHealth">模拟健康提醒</button>
        <button class="test-btn" @tap="clearAll">清空记录</button>
      </view>
    </view>
  </view>
</template>

<script>
import Push from '@/utils/push.js'

export default {
  data() {
    return {
      clientId: '',
      messages: [],
      clicks: [],
    }
  },
  onLoad() {
    this.refreshStatus()
    this.hookPush()
  },
  methods: {
    refreshStatus() {
      this.clientId = Push.clientId || ''
    },
    hookPush() {
      // 包装原始的 receive 回调，追加到本地列表
      const _origReceive = Push.onPushReceive
      const self = this
      Push.onPushReceive = function(msg) {
        _origReceive.call(Push, msg)
        self.messages.unshift({ ...msg, _time: self.now() })
      }
      // 包装原始的 click 回调
      const _origClick = Push.onPushClick
      Push.onPushClick = function(msg) {
        _origClick.call(Push, msg)
        self.clicks.unshift({ ...msg, _time: self.now() })
      }
    },
    now() {
      const d = new Date()
      return d.getHours().toString().padStart(2, '0') + ':' +
             d.getMinutes().toString().padStart(2, '0') + ':' +
             d.getSeconds().toString().padStart(2, '0')
    },
    copyCid() {
      uni.setClipboardData({ data: this.clientId })
      uni.showToast({ title: '已复制', icon: 'success' })
    },
    mockReceive() {
      const msg = {
        type: 'receive',
        title: '健康提醒',
        content: '您有一份新的体检报告已生成',
        payload: JSON.stringify({ route: '', type: 'report' }),
      }
      Push.onPushReceive(msg)
      uni.showToast({ title: '已模拟', icon: 'success', duration: 1000 })
    },
    mockClick() {
      const msg = {
        type: 'click',
        title: '报告就绪',
        content: '血常规报告已出，点击查看',
        payload: JSON.stringify({ route: '', type: 'report' }),
      }
      Push.onPushClick(msg)
      uni.showToast({ title: '已模拟', icon: 'success', duration: 1000 })
    },
    mockHealth() {
      const list = [
        { title: '用药提醒', content: '您有用药计划已到期，请及时服药' },
        { title: '复诊提醒', content: '预约的复诊时间即将到来' },
        { title: '健康周报', content: '本周健康数据汇总已完成，各项指标正常' },
        { title: '运动提醒', content: '今日运动目标尚未完成，继续加油' },
      ]
      const item = list[Math.floor(Math.random() * list.length)]
      Push.onPushReceive({ type: 'receive', ...item })
      uni.showToast({ title: '已模拟: ' + item.title, icon: 'success', duration: 1000 })
    },
    clearAll() {
      this.messages = []
      this.clicks = []
      uni.showToast({ title: '已清空', icon: 'success', duration: 1000 })
    },
  },
}
</script>

<style scoped>
.push-demo-page {
  min-height: 100vh;
  background: #f5f6fa;
  padding: 24rpx;
}

.status-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
}
.status-row {
  display: flex;
  align-items: center;
}
.status-dot {
  width: 16rpx; height: 16rpx;
  border-radius: 50%;
  background: #ccc;
  margin-right: 12rpx;
}
.status-dot.active {
  background: #4cd964;
  box-shadow: 0 0 10rpx rgba(76,217,100,.5);
}
.status-label {
  flex: 1;
  font-size: 30rpx; font-weight: 600; color: #333;
}
.status-refresh {
  font-size: 26rpx; color: #007aff;
}
.cid-box {
  margin-top: 20rpx;
  background: #f8f8f8;
  border-radius: 10rpx;
  padding: 20rpx;
}
.cid-label {
  font-size: 22rpx; color: #999; margin-bottom: 8rpx; display: block;
}
.cid-value {
  font-size: 24rpx; color: #666; word-break: break-all; font-family: monospace;
}
.cid-placeholder {
  font-size: 24rpx; color: #bbb;
}

.section {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
}
.section-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20rpx;
}
.section-title {
  font-size: 30rpx; font-weight: 600; color: #333;
}
.section-count {
  font-size: 24rpx; color: #999;
}

.empty {
  text-align: center;
  font-size: 26rpx; color: #bbb;
  padding: 40rpx 0;
}

.msg-item {
  padding: 20rpx;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
}
.msg-item.receive { background: #e8f5e9; border-left: 6rpx solid #4cd964; }
.msg-item.click   { background: #fff3e0; border-left: 6rpx solid #ff9500; }
.msg-top {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx;
}
.msg-title { font-size: 28rpx; font-weight: 600; color: #333; }
.msg-time  { font-size: 22rpx; color: #999; }
.msg-body  { font-size: 26rpx; color: #666; line-height: 1.6; }

.btn-grid {
  display: flex; flex-wrap: wrap; gap: 16rpx;
}
.test-btn {
  flex: 1 1 calc(50% - 8rpx);
  min-width: 200rpx;
  border: none;
  border-radius: 12rpx;
  font-size: 26rpx;
  padding: 24rpx 0;
  color: #fff;
  background: linear-gradient(135deg, #42a5f5, #1e88e5);
}
.test-btn:nth-child(2) { background: linear-gradient(135deg, #66bb6a, #43a047); }
.test-btn:nth-child(3) { background: linear-gradient(135deg, #ffa726, #fb8c00); }
.test-btn:nth-child(4) { background: linear-gradient(135deg, #78909c, #546e7a); }
</style>
