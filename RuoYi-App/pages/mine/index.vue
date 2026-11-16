<template>
  <view class="page-body" :class="themeClass" :style="{ minHeight: windowHeight + 'px' }">
    <!--顶部个人信息栏-->
    <view class="header-bar">
      <view class="header-row">
        <view class="user-info">
          <view v-if="!avatar" class="avatar-placeholder">
            <view class="iconfont icon-people avatar-icon"></view>
          </view>
          <image v-if="avatar" @click="handleToAvatar" :src="avatar" class="avatar-img" mode="widthFix"></image>
          <view v-if="!name" @click="handleToLogin" class="login-text">点击登录</view>
          <view v-if="name" @click="handleToInfo" class="name-text">用户名：{{ name }}</view>
        </view>
        <view @click="handleToInfo" class="info-link">
          <text>个人信息</text>
          <text class="iconfont icon-right"></text>
        </view>
      </view>
    </view>

    <view class="content-area">
      <!-- 宫格菜单 -->
      <view class="grid-menu">
        <view class="grid-item" @click="switchTheme">
          <view class="iconfont icon-setting grid-icon purple"></view>
          <view class="grid-label">主题皮肤</view>
        </view>
        <!-- 在线客服：微信小程序走原生客服会话（后台「客服 → 客服人员」接待），其他端暂未开放 add by CYQ 2026-11-10 -->
        <!-- #ifdef MP-WEIXIN -->
        <button class="grid-item grid-btn" open-type="contact" session-from="mine-grid">
          <view class="iconfont icon-service grid-icon blue"></view>
          <view class="grid-label">在线客服</view>
        </button>
        <!-- #endif -->
        <!-- #ifndef MP-WEIXIN -->
        <view class="grid-item" @click="handleHelp">
          <view class="iconfont icon-service grid-icon blue"></view>
          <view class="grid-label">在线客服</view>
        </view>
        <!-- #endif -->
        <view class="grid-item" @click="handleBuilding">
          <view class="iconfont icon-community grid-icon yellow"></view>
          <view class="grid-label">反馈社区</view>
        </view>
        <view class="grid-item" @click="handleBuilding">
          <view class="iconfont icon-dianzan grid-icon green"></view>
          <view class="grid-label">点赞我们</view>
        </view>
      </view>

      <!-- 列表菜单 -->
      <view class="menu-list">
        <view class="list-cell list-cell-arrow" @click="handleToNotification">
          <view class="menu-item-box">
            <uni-icons type="notification" size="18" color="#007aff" class="menu-icon"></uni-icons>
            <view>消息通知</view>
            <view class="badge" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleToEditInfo">
          <view class="menu-item-box">
            <view class="iconfont icon-user menu-icon"></view>
            <view>编辑资料</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleHelp">
          <view class="menu-item-box">
            <view class="iconfont icon-help menu-icon"></view>
            <view>常见问题</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleAbout">
          <view class="menu-item-box">
            <view class="iconfont icon-aixin menu-icon"></view>
            <view>关于我们</view>
          </view>
        </view>

        <view class="list-cell list-cell-arrow" @click="handleToSetting">
          <view class="menu-item-box">
            <view class="iconfont icon-setting menu-icon"></view>
            <view>应用设置</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 主题选择弹窗 -->
    <view v-if="showThemePicker" class="theme-overlay" @click="closeThemePicker">
      <view class="theme-picker" @click.stop>
        <view class="theme-picker__bar"></view>
        <view class="theme-picker__header">
          <text class="theme-picker__title">切换主题</text>
          <text class="theme-picker__close" @click="closeThemePicker">✕</text>
        </view>
        <view class="theme-picker__list">
          <view
            v-for="t in themes"
            :key="t.id"
            :class="['theme-picker__item', { active: currentTheme === t.id }]"
            @click="selectTheme(t.id)"
          >
            <view class="theme-picker__preview" :style="{ background: t.preview }">
              <view class="theme-picker__dot" :style="{ background: t.accent }"></view>
            </view>
            <view class="theme-picker__info">
              <text class="theme-picker__name">{{ t.name }}</text>
            </view>
            <view v-if="currentTheme === t.id" class="theme-picker__check">
              <text>✓</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import storage from '@/utils/storage'
import Push from '@/utils/push.js'
import themeMixin from '@/mixins/theme'

export default {
  mixins: [themeMixin],
  data() {
    return {
      name: this.$store.state.user.name,
      version: getApp().globalData.config.appInfo.version,
      unreadCount: 0,
      themes: [
        { id: 'default', name: '经典蓝', preview: 'linear-gradient(135deg,#F3F5F9,#E8EDF5)', accent: '#409EFF' },
        { id: 'pink', name: '粉白', preview: 'linear-gradient(135deg,#FFF5F7,#FFE4EC)', accent: '#FF9ECD' },
        { id: 'kuromi', name: '库洛米', preview: 'linear-gradient(135deg,#07080D,#1A1230)', accent: '#E84393' },
        { id: 'pixel', name: '泰拉瑞亚', preview: 'linear-gradient(135deg,#6AB7E8 0%,#5BAF4B 38%,#8B5A2B 100%)', accent: '#8B5A2B' }
      ],
      showThemePicker: false
    }
  },
  computed: {
    avatar() {
      return this.$store.state.user.avatar
    },
    windowHeight() {
      return uni.getWindowInfo().windowHeight - 50
    },
    currentTheme() {
      return this.$store.state.theme.theme
    }
  },
  onShow() {
    this.unreadCount = Push.getUnreadCount()
  },
  methods: {
    switchTheme() {
      this.showThemePicker = true
    },
    selectTheme(id) {
      this.$store.commit('theme/SET_THEME', id)
      // #ifdef H5
      document.documentElement.className = 'theme-' + id
      // #endif
      this.showThemePicker = false
    },
    closeThemePicker() {
      this.showThemePicker = false
    },
    handleToNotification() {
      this.$tab.navigateTo('/pages/mine/notification/index')
    },
    handleToInfo() {
      this.$tab.navigateTo('/pages/mine/info/index')
    },
    handleToEditInfo() {
      this.$tab.navigateTo('/pages/mine/info/edit')
    },
    handleToSetting() {
      this.$tab.navigateTo('/pages/mine/setting/index')
    },
    handleToLogin() {
      this.$tab.reLaunch('/pages/login')
    },
    handleToAvatar() {
      this.$tab.navigateTo('/pages/mine/avatar/index')
    },
    handleLogout() {
      this.$modal.confirm('确定注销并退出系统吗？').then(() => {
        this.$store.dispatch('LogOut').then(() => {
          this.$tab.reLaunch('/pages/index')
        })
      })
    },
    handleHelp() {
      this.$tab.navigateTo('/pages/mine/help/index')
    },
    handleAbout() {
      this.$tab.navigateTo('/pages/mine/about/index')
    },
    handleBuilding() {
      this.$modal.showToast('模块建设中~')
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: var(--bg-start, #f5f6f7);
}

.page-body {
  width: 100%;
}

.header-bar {
  background: linear-gradient(135deg, var(--color-primary, #3c96f3) 0%, var(--color-primary-dim, #5aa5f5) 100%);
  color: #fff;
  padding: 16rpx 16rpx 48rpx;
  .header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .user-info {
    display: flex;
    align-items: center;
  }
  .avatar-placeholder {
    border: 4rpx solid var(--card-border, #eaeaea);
    border-radius: 50%;
    width: 64px;
    height: 64px;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .avatar-icon {
    color: #999;
    font-size: 40px;
  }
  .avatar-img {
    border: 4rpx solid var(--card-border, #eaeaea);
    border-radius: 50%;
    width: 64px;
    height: 64px;
  }
  .login-text {
    font-size: 18px;
    margin-left: 10rpx;
  }
  .name-text {
    font-size: 18px;
    margin-left: 16rpx;
  }
  .info-link {
    display: flex;
    align-items: center;
    font-size: 14px;
  }
}

.content-area {
  margin-top: -30rpx;
}

.grid-menu {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  text-align: center;
  background: var(--card-bg, #fff);
  border-radius: 8rpx;
  margin: 0 16rpx;
  padding: 20rpx 0;
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 小程序 open-type="contact" 按钮：重置原生样式，保持与宫格一致 add by CYQ 2026-11-10 */
.grid-btn {
  width: 100%;
  margin: 0;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 0;
  line-height: normal;
  font-size: inherit;
  color: inherit;

  &::after {
    border: none;
  }
}
.grid-icon {
  font-size: 28px;
  margin-bottom: 8rpx;
  &.pink { color: #db2777; }
  &.blue { color: #2563eb; }
  &.yellow { color: #ca8a04; }
  &.green { color: #16a34a; }
  &.purple { color: #8B5CF6; }
}
.grid-label {
  font-size: 12px;
  color: var(--color-text, #333);
}

.menu-list {
  margin: 15px 15px;

  .menu-item-box {
    width: 100%;
    display: flex;
    align-items: center;

    .menu-icon {
      color: var(--color-primary, #007aff);
      font-size: 16px;
      margin-right: 5px;
    }

    .text-right {
      margin-left: auto;
      margin-right: 34rpx;
      color: #999;
    }
  }
}

.badge {
  background: #e74c3c;
  color: #fff;
  font-size: 20rpx;
  min-width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  border-radius: 18rpx;
  padding: 0 8rpx;
  margin-left: auto;
  margin-right: 20rpx;
}

.list-cell-arrow::before {
  content: " ";
  height: 10px;
  width: 10px;
  border-width: 2px 2px 0 0;
  border-color: var(--color-text-secondary, #c0c0c0);
  border-style: solid;
  transform: rotate(45deg);
  position: absolute;
  top: 50%;
  margin-top: -6px;
  right: 30rpx;
}

.list-cell {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  background-color: var(--card-bg, #fff);
  color: var(--color-text, #333);
  padding: 26rpx 30rpx;
}

.list-cell:first-child {
  border-radius: 8rpx 8rpx 0 0;
}

.list-cell:last-child {
  border-radius: 0 0 8rpx 8rpx;
}

.list-cell::after {
  content: "";
  position: absolute;
  border-bottom: 1px solid var(--card-border, #eaeef1);
  transform: scaleY(0.5);
  transform-origin: 0 100%;
  bottom: 0;
  right: 0;
  left: 0;
  pointer-events: none;
}

/* ===== 主题选择弹窗 ===== */
.theme-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.4);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn {
  0% { opacity: 0; }
  100% { opacity: 1; }
}
.theme-picker {
  width: 100%;
  background: var(--card-bg, #fff);
  border-radius: 24rpx 24rpx 0 0;
  padding: 0 0 40rpx;
  animation: slideUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes slideUp {
  0% { transform: translateY(100%); }
  100% { transform: translateY(0); }
}
.theme-picker__bar {
  width: 48rpx;
  height: 6rpx;
  background: var(--skeleton-2, #ddd);
  border-radius: 3rpx;
  margin: 16rpx auto;
}
.theme-picker__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32rpx 20rpx;
}
.theme-picker__title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--color-text, #333);
}
.theme-picker__close {
  font-size: 32rpx;
  color: var(--color-text-secondary, #999);
  padding: 8rpx;
}
.theme-picker__list {
  padding: 0 32rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.theme-picker__item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  transition: all 0.2s ease;
  position: relative;
}
.theme-picker__item:active {
  background: var(--skeleton-2, #f5f6f7);
  transform: scale(0.98);
}
.theme-picker__item.active {
  background: var(--color-primary-dim, #f0f0ff);
}
.theme-picker__preview {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1);
  flex-shrink: 0;
}
.theme-picker__dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  box-shadow: 0 0 8rpx rgba(255,255,255,0.6);
}
.theme-picker__info {
  flex: 1;
}
.theme-picker__name {
  font-size: 28rpx;
  color: var(--color-text, #333);
  font-weight: 600;
}
.theme-picker__check {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: var(--color-primary, #007aff);
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>