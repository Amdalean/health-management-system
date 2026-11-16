<template>
  <view class="page-container" :class="themeClass">
    <image class="logo" src="@/static/logo.png"></image>
    <view class="title-row">
      <text class="title">Hello Caelum</text>
    </view>
    <view class="subtitle-row">
      <text class="subtitle">你的生活助手</text>
    </view>
  </view>
</template>

<script>
import themeMixin from '@/mixins/theme'
import { getToken } from '@/utils/auth'
// #ifdef APP-HARMONY
import { startFlashBall, isFlashBallShowing, checkFlashBallClicked } from '@/utils/flash-ball.js'
// #endif

const FLOAT_WINDOW_KEY = 'hms_float_window_enabled'

export default {
  mixins: [themeMixin],
  data() {
    return {
      recognizing: false,
      flashBallPollTimer: null
    }
  },
  onShow: function () {
    // 未登录跳转登录页
    if (!getToken()) {
      uni.reLaunch({ url: '/pages/login' })
      return
    }
    // #ifdef APP-HARMONY
    // 自动恢复闪控球
    if (uni.getStorageSync(FLOAT_WINDOW_KEY) && !isFlashBallShowing()) {
      console.log('[首页] 自动恢复闪控球')
      startFlashBall().then(() => {
        console.log('[首页] 闪控球已恢复')
      }).catch((e) => {
        console.warn('[首页] 闪控球恢复失败:', e)
      })
    }

    // 立即检测一次闪控球点击标记
    this.checkAndHandleFlashBall()
    // 启动轮询：闪控球点击后应用可能已在前台，onShow 不会再次触发
    this.startFlashBallPoll()
    // #endif
  },
  onHide() {
    this.stopFlashBallPoll()
  },
  onUnload() {
    this.stopFlashBallPoll()
  },
  methods: {
    startFlashBallPoll() {
      this.stopFlashBallPoll()
      this.flashBallPollTimer = setInterval(() => {
        this.checkAndHandleFlashBall()
      }, 800)
    },
    stopFlashBallPoll() {
      if (this.flashBallPollTimer) {
        clearInterval(this.flashBallPollTimer)
        this.flashBallPollTimer = null
      }
    },
    checkAndHandleFlashBall() {
      const result = checkFlashBallClicked()
      if (result.clicked && !this.recognizing) {
        this.handleFlashBallCapture(result)
      }
    },
    handleFlashBallCapture(clickResult) {
      if (this.recognizing) return
      const filePath = clickResult.screenshotPath || ''
      if (!filePath) return

      this.recognizing = true
      this.stopFlashBallPoll()
      console.log('[首页] 闪控球点击 → 跳转随手记:', filePath)
      uni.navigateTo({
        url: `/pages/jotdown/add?screenshotPath=${encodeURIComponent(filePath)}`
      })
      this.recognizing = false
    }
  }
}
</script>

<style>
.page-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--bg-gradient, var(--bg-start, #F3F5F9));
}
.logo {
  height: 200rpx;
  width: 200rpx;
  margin: 200rpx auto 50rpx auto;
}
.title-row {
  display: flex;
  justify-content: center;
}
.title {
  font-size: 36rpx;
  color: var(--color-text, #1D2129);
}
.subtitle-row {
  display: flex;
  justify-content: center;
  margin-top: 12rpx;
}
.subtitle {
  font-size: 24rpx;
  color: var(--color-text-secondary, #909399);
}
</style>
