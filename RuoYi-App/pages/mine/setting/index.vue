<template>
  <view class="setting-container" :style="{height: `${windowHeight}px`}">
    <view class="menu-list">
      <view class="list-cell list-cell-arrow" @click="handleToPwd">
        <view class="menu-item-box">
          <view class="iconfont icon-password menu-icon"></view>
          <view>修改密码</view>
        </view>
      </view>
      <!-- #ifdef MP-WEIXIN -->
      <!-- 手动关联微信：关联后登录页才显示微信一键登录 -->
      <!-- add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录 -->
      <view class="list-cell list-cell-arrow" @click="handleWxBindToggle">
        <view class="menu-item-box">
          <view class="iconfont icon-user menu-icon"></view>
          <view>{{ wxBound ? '解除微信关联' : '关联微信' }}</view>
          <view class="text-right">{{ wxBound ? '已关联' : '未关联' }}</view>
        </view>
      </view>
      <!-- #endif -->
      <view class="list-cell list-cell-arrow" @click="checkUpdate">
        <view class="menu-item-box">
          <view class="iconfont icon-refresh menu-icon"></view>
          <view>检查更新</view>
        </view>
      </view>
      <view class="list-cell list-cell-arrow" @click="handleCleanTmp">
        <view class="menu-item-box">
          <view class="iconfont icon-clean menu-icon"></view>
          <view>清理缓存</view>
        </view>
      </view>
      <!-- 悬浮窗截屏识图开关 -->
      <view class="list-cell">
        <view class="menu-item-box menu-item-box--switch">
          <uni-icons type="camera-filled" size="22" color="#409EFF" class="switch-icon"></uni-icons>
          <view class="switch-text">
            <view class="switch-title">悬浮窗截屏识图</view>
            <view class="switch-desc">切到后台后，双击悬浮按钮截屏并AI识图记账</view>
          </view>
          <switch
            :checked="floatWindowEnabled"
            color="#409EFF"
            @change="handleFloatWindowToggle"
            class="switch-right"
          />
        </view>
      </view>
    </view>
    <view class="cu-list menu">
      <view class="cu-item item-box">
        <view class="content text-center" @click="handleLogout">
          <text class="text-black">退出登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
  import { startFloatWindow, stopFloatWindow, checkFloatWindowPermission } from '@/utils/float-window.js'
  import { startFlashBall, stopFlashBall, checkFlashBallClicked } from '@/utils/flash-ball.js'
  import { checkUpdate } from '@/utils/updateManager'
  import { getWxBindStatus, bindWx, unbindWx } from '@/api/login'
  import { code2session } from '@/api/system/clockin'
  import { getToken } from '@/utils/auth'

  const FLOAT_WINDOW_KEY = 'hms_float_window_enabled'

  export default {
    data() {
      return {
        windowHeight: uni.getWindowInfo().windowHeight,
        floatWindowEnabled: !!uni.getStorageSync(FLOAT_WINDOW_KEY),
        recognizing: false,
        flashBallPollTimer: null,
        // 当前账号是否已关联微信
        // add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
        wxBound: false,
        wxBinding: false
      }
    },
    onShow() {
      // 立即检测一次闪控球点击标记
      this.checkAndHandleFlashBall()
      // 启动轮询：应用已在前台时 onShow 不会再次触发
      this.startFlashBallPoll()
      this.refreshWxStatus()
    },
    onHide() {
      this.stopFlashBallPoll()
    },
    onUnload() {
      this.stopFlashBallPoll()
    },
    methods: {
      handleToPwd() {
        this.$tab.navigateTo('/pages/mine/pwd/index')
      },
      // 查询当前账号微信关联状态（静默，需登录）
      // add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
      refreshWxStatus() {
        // #ifdef MP-WEIXIN
        if (!getToken()) return
        getWxBindStatus().then((res) => {
          this.wxBound = res.bound === true
        }).catch(() => {})
        // #endif
      },
      // 关联 / 解除微信关联（必须由点击事件触发，wx.login 不弹授权框）
      // add by CYQ 2026-09-09 设置页手动关联微信，登录页仅已绑定显示一键登录
      handleWxBindToggle() {
        // #ifdef MP-WEIXIN
        if (this.wxBinding) return
        if (this.wxBound) {
          this.$modal.confirm('解除后将无法使用微信一键登录，确定解除吗？').then(() => {
            this.wxBinding = true
            unbindWx().then(() => {
              this.wxBound = false
              this.$modal.msgSuccess('已解除微信关联')
            }).finally(() => {
              this.wxBinding = false
            })
          }).catch(() => {})
          return
        }
        this.wxBinding = true
        this.$modal.loading('关联中...')
        uni.login({
          success: (loginRes) => {
            if (!loginRes.code) {
              this.$modal.closeLoading()
              this.$modal.msgError('获取微信凭证失败')
              this.wxBinding = false
              return
            }
            code2session(loginRes.code).then((sessionRes) => {
              const openid = sessionRes.data && sessionRes.data.openid
              if (!openid) {
                this.$modal.closeLoading()
                this.$modal.msgError('换取微信标识失败')
                this.wxBinding = false
                return
              }
              bindWx(openid).then(() => {
                this.$modal.closeLoading()
                this.wxBound = true
                this.$modal.msgSuccess('微信关联成功')
              }).catch(() => {
                this.$modal.closeLoading()
              }).finally(() => {
                this.wxBinding = false
              })
            }).catch(() => {
              this.$modal.closeLoading()
              this.$modal.msgError('换取微信标识失败')
              this.wxBinding = false
            })
          },
          fail: () => {
            this.$modal.closeLoading()
            this.$modal.msgError('唤起微信失败')
            this.wxBinding = false
          }
        })
        // #endif
        // #ifndef MP-WEIXIN
        this.$modal.showToast('当前平台不支持关联微信')
        // #endif
      },
      // 悬浮窗截屏识图开关
      async handleFloatWindowToggle(e) {
        const enabled = e.detail.value
        this.floatWindowEnabled = enabled

        if (enabled) {
          // #ifdef APP-PLUS
          try {
            const perm = await checkFloatWindowPermission()
            if (!perm || !perm.granted) {
              uni.showModal({
                title: '需要悬浮窗权限',
                content: '请在系统设置中为「Caelum」开启"显示在其他应用上层"权限',
                confirmText: '去设置',
                success: (res) => {
                  if (res.confirm) {
                    const Intent = plus.android.importClass('android.content.Intent')
                    const Settings = plus.android.importClass('android.provider.Settings')
                    const Uri = plus.android.importClass('android.net.Uri')
                    const main = plus.android.runtimeMainActivity()
                    const pkgName = main.getPackageName()

                    let intent = null
                    try {
                      intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse('package:' + pkgName)
                      )
                    } catch (e1) {
                      console.warn('[FloatWindow] ACTION_MANAGE_OVERLAY_PERMISSION 不支持，回退到应用详情')
                      intent = null
                    }
                    if (!intent) {
                      intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                      intent.setData(Uri.parse('package:' + pkgName))
                    }
                    intent.addFlags(0x10000000)
                    main.startActivity(intent)
                  }
                }
              })
              this.floatWindowEnabled = false
              uni.setStorageSync(FLOAT_WINDOW_KEY, false)
              return
            }
            await startFloatWindow()
            uni.setStorageSync(FLOAT_WINDOW_KEY, true)
            uni.showToast({ title: '悬浮窗已开启', icon: 'success' })
          } catch (e) {
            console.error('[FloatWindow] 启动失败:', e)
            uni.showToast({ title: e.msg || '启动失败，请重试', icon: 'none' })
            this.floatWindowEnabled = false
            uni.setStorageSync(FLOAT_WINDOW_KEY, false)
          }
          // #endif

          // #ifdef APP-HARMONY
          try {
            await startFlashBall()
            uni.setStorageSync(FLOAT_WINDOW_KEY, true)
            uni.showToast({ title: '闪控球已开启', icon: 'success' })
          } catch (e) {
            console.error('[闪控球] 启动失败:', e)
            uni.showToast({ title: e.msg || '启动失败，请重试', icon: 'none' })
            this.floatWindowEnabled = false
            uni.setStorageSync(FLOAT_WINDOW_KEY, false)
          }
          // #endif

          // #ifndef APP-PLUS
          // #ifndef APP-HARMONY
          uni.showToast({ title: '当前平台不支持悬浮窗', icon: 'none' })
          this.floatWindowEnabled = false
          uni.setStorageSync(FLOAT_WINDOW_KEY, false)
          // #endif
          // #endif
        } else {
          try {
            // #ifdef APP-HARMONY
            await stopFlashBall()
            // #endif
            // #ifndef APP-HARMONY
            await stopFloatWindow()
            // #endif
            uni.setStorageSync(FLOAT_WINDOW_KEY, false)
            // #ifdef APP-HARMONY
            uni.showToast({ title: '闪控球已关闭', icon: 'none' })
            // #endif
            // #ifndef APP-HARMONY
            uni.showToast({ title: '悬浮窗已关闭', icon: 'none' })
            // #endif
          } catch (e) {
            console.error('[FloatWindow] 停止失败:', e)
          }
        }
      },
      checkUpdate() {
        checkUpdate({ showNoUpdateTip: true })
      },
      handleCleanTmp() {
        this.$modal.showToast('模块建设中~')
      },
      handleLogout() {
        this.$modal.confirm('确定注销并退出系统吗？').then(() => {
          this.$store.dispatch('LogOut').then(() => {
            this.$tab.reLaunch('/pages/index')
          })
        })
      },
      // 闪控球轮询检测
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
      // 闪控球截屏识图记账
      async handleFlashBallCapture(clickResult = {}) {
        if (this.recognizing) return
        const filePath = clickResult.screenshotPath || ''
        if (!filePath) return

        this.recognizing = true
        this.stopFlashBallPoll()
        console.log('[设置页] 闪控球点击 → 跳转随手记:', filePath)

        // 直接跳转，由 add.vue 处理加载和识别
        uni.navigateTo({
          url: `/pages/jotdown/add?screenshotPath=${encodeURIComponent(filePath)}`
        })
        this.recognizing = false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .item-box {
    background-color: #FFFFFF;
    margin: 30rpx;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    padding: 10rpx;
    border-radius: 8rpx;
    color: #303133;
    font-size: 32rpx;
  }

  .menu-item-box--switch {
    display: flex;
    align-items: center;
    width: 100%;
  }

  .menu-item-box {
    .text-right {
      margin-left: auto;
      margin-right: 34rpx;
      color: #999;
      font-size: 26rpx;
    }
  }
  .switch-icon {
    flex-shrink: 0;
  }

  .switch-text {
    flex: 1;
    margin-left: 16rpx;
    display: flex;
    flex-direction: column;
    gap: 4rpx;
    min-width: 0;
  }

  .switch-title {
    font-size: 30rpx;
    color: #303133;
  }

  .switch-desc {
    font-size: 22rpx;
    color: #909399;
    line-height: 1.4;
  }

  .switch-right {
    flex-shrink: 0;
    margin-left: 16rpx;
  }
</style>